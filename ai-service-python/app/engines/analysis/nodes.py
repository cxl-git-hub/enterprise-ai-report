"""Analysis LangGraph node implementations."""

import json
from typing import Any, Dict, List

import httpx
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.dataset import Dataset
from app.models.schema_definition import SchemaDefinition
from app.engines.analysis.state import AnalysisState
from app.services.prompt_builder import PromptBuilder
from app.services.cost_tracker import CostTracker
from app.services.llm_client import get_llm_client
from app.safety.prompt_guard import PromptGuard
from app.safety.output_validator import OutputValidator
from app.prompts.analysis_system import ANALYSIS_SYSTEM_PROMPT


async def retrieve_data(state: AnalysisState, db: AsyncSession) -> Dict[str, Any]:
    """Retrieve data for analysis."""
    tenant_id = state["tenant_id"]
    dataset_ids = state.get("dataset_ids", [])
    sql = state.get("sql")

    if sql:
        # Use provided SQL
        try:
            result = await db.execute(text(sql))
            rows = result.mappings().all()
            raw_data = [dict(row) for row in rows[:1000]]  # Limit data
            data_context = json.dumps(raw_data[:100], indent=2, default=str)  # First 100 rows for prompt
            return {
                "raw_data": raw_data,
                "data_context": data_context,
            }
        except Exception as e:
            return {"error_message": f"Failed to execute SQL: {str(e)}"}

    # Build a simple SELECT for the datasets
    
    query = select(Dataset).where(Dataset.tenant_id == tenant_id)
    if dataset_ids:
        query = query.where(Dataset.id.in_([int(d) for d in dataset_ids]))

    result = await db.execute(query)
    datasets = list(result.scalars().all())

    if not datasets:
        return {"error_message": "No datasets found"}

    # Fetch sample data from first dataset
    ds = datasets[0]
    try:
        sample_query = text(f"SELECT * FROM {ds.table_name} LIMIT 100")
        result = await db.execute(sample_query)
        rows = result.mappings().all()
        raw_data = [dict(row) for row in rows]
        data_context = json.dumps(raw_data, indent=2, default=str)
        return {
            "raw_data": raw_data,
            "data_context": data_context,
        }
    except Exception as e:
        return {"error_message": f"Failed to retrieve data: {str(e)}"}


async def inject_schema_context(state: AnalysisState, db: AsyncSession) -> Dict[str, Any]:
    """Inject schema context for the analysis."""
    tenant_id = state["tenant_id"]
    dataset_ids = state.get("dataset_ids", [])

    
    query = select(Dataset).where(Dataset.tenant_id == tenant_id)
    if dataset_ids:
        query = query.where(Dataset.id.in_([int(d) for d in dataset_ids]))

    result = await db.execute(query)
    datasets = list(result.scalars().all())

    schema_parts = []
    for ds in datasets:
        schema_result = await db.execute(
            select(SchemaDefinition).where(SchemaDefinition.dataset_id == ds.id)
        )
        columns = list(schema_result.scalars().all())

        col_defs = []
        for col in columns:
            desc = f" - {col.description}" if col.description else ""
            col_defs.append(f"  {col.column_name} ({col.data_type}){desc}")

        schema_parts.append(
            f"Table: {ds.table_name}\n"
            f"Description: {ds.description or ds.name}\n"
            f"Columns:\n" + "\n".join(col_defs)
        )

    return {"schema_context": "\n\n".join(schema_parts)}


async def build_analysis_prompt(state: AnalysisState, db: AsyncSession) -> Dict[str, Any]:
    """Build the analysis prompt."""
    guard = PromptGuard()
    query = state["query"]
    if guard.detect_injection(query):
        return {"error_message": "Query rejected: potential prompt injection detected"}

    builder = PromptBuilder(max_tokens=settings.PROMPT_MAX_TOKENS)

    prompt = builder.build_analysis_prompt(
        system_prompt=ANALYSIS_SYSTEM_PROMPT,
        user_query=query,
        data_context=state.get("data_context", ""),
        schema_context=state.get("schema_context", ""),
        analysis_type=state.get("analysis_type", "general"),
    )

    return {
        "system_prompt": prompt["system"],
        "user_prompt": prompt["user"],
    }


async def llm_analyze(state: AnalysisState, db: AsyncSession) -> Dict[str, Any]:
    """Call the LLM for analysis using the unified client."""
    system_prompt = state.get("system_prompt", "")
    user_prompt = state.get("user_prompt", "")

    llm = get_llm_client()
    result = await llm.chat(
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    )

    raw_output = result["content"]

    cost_tracker = CostTracker(db)
    prompt_tokens = result["prompt_tokens"]
    completion_tokens = result["completion_tokens"]
    cost = cost_tracker.calculate_cost_for_model(
        prompt_tokens, completion_tokens, result["model"]
    )

    return {
        "raw_llm_output": raw_output,
        "prompt_tokens": prompt_tokens,
        "completion_tokens": completion_tokens,
        "total_tokens": result["total_tokens"],
        "cost_usd": state.get("cost_usd", 0.0) + cost,
        "model_name": result["model"],
    }


def validate_output(state: AnalysisState) -> Dict[str, Any]:
    """Validate the LLM output as JSON."""
    raw_output = state.get("raw_llm_output", "")

    # Try to extract JSON from the output
    parsed = None
    try:
        # Try direct parse
        parsed = json.loads(raw_output)
    except json.JSONDecodeError:
        # Try to extract from code block
        import re
        json_match = re.search(r'```(?:json)?\s*\n?(.*?)\n?```', raw_output, re.DOTALL)
        if json_match:
            try:
                parsed = json.loads(json_match.group(1))
            except json.JSONDecodeError:
                pass

    if not parsed:
        # Create a basic structure from the text
        parsed = {
            "summary": raw_output[:500] if raw_output else "No analysis available",
            "insights": [],
            "recommendations": [],
            "metrics": {},
        }

    # Validate structure
    validator = OutputValidator()
    is_valid, errors = validator.validate_analysis_output(parsed)

    insights = parsed.get("insights", [])
    recommendations = parsed.get("recommendations", [])

    # If not valid but we have some content, use what we have
    if not is_valid and parsed.get("summary"):
        is_valid = True
        errors = []

    return {
        "parsed_result": parsed,
        "is_valid": is_valid,
        "validation_errors": errors,
        "insights": insights if isinstance(insights, list) else [],
        "recommendations": recommendations if isinstance(recommendations, list) else [],
        "result": parsed,
    }


def check_validation(state: AnalysisState) -> str:
    """Check if validation passed or needs retry."""
    if state.get("error_message"):
        return "end"
    if state.get("is_valid"):
        return "end"
    if state.get("retry_count", 0) >= state.get("max_retries", 2):
        return "end"
    return "retry"


async def retry_analysis(state: AnalysisState, db: AsyncSession) -> Dict[str, Any]:
    """Build a retry prompt with validation feedback."""
    retry_count = state.get("retry_count", 0) + 1
    errors = state.get("validation_errors", [])

    error_feedback = "\n".join(f"- {e}" for e in errors)
    retry_user_prompt = (
        f"{state.get('user_prompt', '')}\n\n"
        f"## Previous Attempt Failed Validation\n"
        f"Errors: {error_feedback}\n\n"
        f"Please provide your response as valid JSON with these fields:\n"
        f"- summary: string\n"
        f"- insights: array of strings\n"
        f"- recommendations: array of strings\n"
        f"- metrics: object"
    )

    return {
        "user_prompt": retry_user_prompt,
        "retry_count": retry_count,
    }
