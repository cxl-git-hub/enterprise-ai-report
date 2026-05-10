"""Report generation LangGraph node implementations."""

import json
from typing import Any, Dict, List

import httpx
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.dataset import Dataset
from app.models.kpi_definition import KPIDefinition
from app.models.schema_definition import SchemaDefinition
from app.models.report_template import ReportTemplate
from app.engines.report.state import ReportState
from app.services.prompt_builder import PromptBuilder
from app.services.cost_tracker import CostTracker
from app.safety.prompt_guard import PromptGuard
from app.prompts.report_system import REPORT_SYSTEM_PROMPT


async def aggregate_kpi_data(state: ReportState, db: AsyncSession) -> Dict[str, Any]:
    """Aggregate KPI data for the report."""
    tenant_id = state["tenant_id"]
    kpi_ids = state.get("kpi_ids", [])
    dataset_ids = state.get("dataset_ids", [])
    date_range = state.get("date_range")

    from uuid import UUID

    kpi_data = {}
    kpi_parts = []

    # Fetch KPIs
    kpi_query = select(KPIDefinition).where(KPIDefinition.tenant_id == tenant_id)
    if kpi_ids:
        kpi_query = kpi_query.where(KPIDefinition.id.in_([UUID(k) for k in kpi_ids]))

    result = await db.execute(kpi_query)
    kpis = list(result.scalars().all())

    for kpi in kpis:
        try:
            # Build aggregation query
            agg_func = kpi.aggregation_type.upper()
            column = kpi.column_name
            table_query = select(Dataset.table_name).where(Dataset.id == kpi.dataset_id)
            table_result = await db.execute(table_query)
            table_name = table_result.scalar_one_or_none()

            if not table_name:
                continue

            if kpi.formula:
                sql = f"SELECT {kpi.formula} as value FROM {table_name}"
            else:
                sql = f"SELECT {agg_func}({column}) as value FROM {table_name}"

            if date_range:
                start = date_range.get("start", "")
                end = date_range.get("end", "")
                if start and end:
                    sql += f" WHERE created_at BETWEEN '{start}' AND '{end}'"

            data_result = await db.execute(text(sql))
            row = data_result.mappings().first()
            value = row["value"] if row else None

            kpi_data[kpi.name] = {
                "display_name": kpi.display_name or kpi.name,
                "value": value,
                "unit": kpi.unit,
                "aggregation": kpi.aggregation_type,
            }
            kpi_parts.append(
                f"- {kpi.display_name or kpi.name}: {value} {kpi.unit or ''} ({kpi.aggregation_type})"
            )
        except Exception:
            continue

    kpi_context = "\n".join(kpi_parts) if kpi_parts else "No KPI data available"

    return {
        "kpi_data": kpi_data,
        "kpi_context": kpi_context,
    }


async def compile_analysis_results(state: ReportState, db: AsyncSession) -> Dict[str, Any]:
    """Compile data context from datasets for the report."""
    tenant_id = state["tenant_id"]
    dataset_ids = state.get("dataset_ids", [])
    analysis_queries = state.get("analysis_queries", [])

    from uuid import UUID

    data_parts = []
    analysis_results = []

    # Run any analysis queries
    for i, query in enumerate(analysis_queries):
        try:
            result = await db.execute(text(query))
            rows = result.mappings().all()[:50]  # Limit rows
            data = [dict(row) for row in rows]
            analysis_results.append({"query_index": i, "query": query, "data": data})
            data_parts.append(f"Query {i + 1}: {query}\nResults: {json.dumps(data[:10], default=str)}")
        except Exception as e:
            data_parts.append(f"Query {i + 1}: {query}\nError: {str(e)}")

    # Get dataset summaries
    query = select(Dataset).where(Dataset.tenant_id == tenant_id)
    if dataset_ids:
        query = query.where(Dataset.id.in_([UUID(d) for d in dataset_ids]))

    result = await db.execute(query)
    datasets = list(result.scalars().all())

    for ds in datasets:
        try:
            count_result = await db.execute(text(f"SELECT COUNT(*) as cnt FROM {ds.table_name}"))
            count = count_result.scalar()
            data_parts.append(f"Dataset '{ds.name}': {count} records in {ds.table_name}")
        except Exception:
            continue

    return {
        "analysis_results": analysis_results,
        "data_context": "\n\n".join(data_parts) if data_parts else "No data context available",
    }


async def select_template(state: ReportState, db: AsyncSession) -> Dict[str, Any]:
    """Select and load the report template."""
    tenant_id = state["tenant_id"]
    template_id = state.get("template_id")

    template_context = ""

    if template_id:
        from uuid import UUID
        result = await db.execute(
            select(ReportTemplate).where(
                ReportTemplate.id == UUID(template_id),
                ReportTemplate.tenant_id == tenant_id,
            )
        )
        template = result.scalar_one_or_none()
        if template:
            sections = template.sections if isinstance(template.sections, list) else []
            template_context = (
                f"Template: {template.name}\n"
                f"Type: {template.template_type}\n"
                f"Sections: {json.dumps(sections, default=str)}\n"
                f"Content Template:\n{template.content_template}"
            )
            return {
                "template_context": template_context,
                "sections": sections,
            }

    # Default template
    default_sections = [
        {"title": "Executive Summary", "type": "summary"},
        {"title": "Key Metrics", "type": "kpi"},
        {"title": "Detailed Analysis", "type": "analysis"},
        {"title": "Trends and Patterns", "type": "trends"},
        {"title": "Recommendations", "type": "recommendations"},
        {"title": "Conclusion", "type": "conclusion"},
    ]

    return {
        "template_context": "Use a professional report format with the following sections:\n" +
                           "\n".join(f"- {s['title']}" for s in default_sections),
        "sections": default_sections,
    }


async def generate_narrative(state: ReportState, db: AsyncSession) -> Dict[str, Any]:
    """Generate the report narrative using LLM."""
    guard = PromptGuard()
    title = state["title"]
    if guard.detect_injection(title):
        return {"error_message": "Title rejected: potential prompt injection detected"}

    builder = PromptBuilder(max_tokens=settings.PROMPT_MAX_TOKENS)

    prompt = builder.build_report_prompt(
        system_prompt=REPORT_SYSTEM_PROMPT,
        title=title,
        description=state.get("description", ""),
        data_context=state.get("data_context", ""),
        kpi_context=state.get("kpi_context", ""),
        template_context=state.get("template_context", ""),
    )

    async with httpx.AsyncClient(timeout=120.0) as client:
        response = await client.post(
            f"{settings.LLM_BASE_URL}/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.LLM_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.LLM_MODEL,
                "messages": [
                    {"role": "system", "content": prompt["system"]},
                    {"role": "user", "content": prompt["user"]},
                ],
                "max_tokens": settings.LLM_MAX_TOKENS,
                "temperature": 0.3,
            },
        )
        response.raise_for_status()
        data = response.json()

    raw_output = data["choices"][0]["message"]["content"]
    usage = data.get("usage", {})

    cost_tracker = CostTracker(db)
    prompt_tokens = usage.get("prompt_tokens", 0)
    completion_tokens = usage.get("completion_tokens", 0)
    cost = cost_tracker.calculate_cost_for_model(
        prompt_tokens, completion_tokens, settings.LLM_MODEL
    )

    return {
        "raw_llm_output": raw_output,
        "report_content": raw_output,
        "prompt_tokens": prompt_tokens,
        "completion_tokens": completion_tokens,
        "total_tokens": prompt_tokens + completion_tokens,
        "cost_usd": state.get("cost_usd", 0.0) + cost,
        "model_name": settings.LLM_MODEL,
    }


def validate_report(state: ReportState) -> Dict[str, Any]:
    """Validate the generated report content."""
    content = state.get("report_content", "")

    errors = []
    if not content or len(content) < 100:
        errors.append("Report content is too short")

    # Check for required sections
    required_sections = ["summary", "recommendation"]
    content_lower = content.lower()
    for section in required_sections:
        if section not in content_lower:
            errors.append(f"Missing required section: {section}")

    is_valid = len(errors) == 0

    return {
        "is_valid": is_valid,
        "validation_errors": errors,
    }


async def assemble_document(state: ReportState, db: AsyncSession) -> Dict[str, Any]:
    """Assemble the final document."""
    content = state.get("report_content", "")
    output_format = state.get("output_format", "markdown")
    title = state.get("title", "Report")

    # For now, return the content as-is
    # In production, this would convert to PDF/HTML and upload to MinIO
    file_url = None

    if output_format == "html":
        # Simple markdown to HTML conversion would go here
        pass

    return {
        "report_content": content,
        "file_url": file_url,
    }


def check_report_validation(state: ReportState) -> str:
    """Check if report validation passed."""
    if state.get("error_message"):
        return "end"
    if state.get("is_valid", True):
        return "assemble"
    if state.get("retry_count", 0) >= state.get("max_retries", 2):
        return "assemble"  # Use what we have
    return "retry"


async def retry_report(state: ReportState, db: AsyncSession) -> Dict[str, Any]:
    """Build retry prompt for report generation."""
    retry_count = state.get("retry_count", 0) + 1
    errors = state.get("validation_errors", [])

    error_feedback = "\n".join(f"- {e}" for e in errors)
    retry_user_prompt = (
        f"{state.get('user_prompt', '')}\n\n"
        f"## Previous attempt had issues:\n{error_feedback}\n\n"
        f"Please ensure the report includes all required sections."
    )

    return {
        "user_prompt": retry_user_prompt,
        "retry_count": retry_count,
    }
