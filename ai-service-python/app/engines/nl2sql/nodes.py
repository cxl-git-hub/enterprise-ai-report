"""NL2SQL LangGraph node implementations."""

import json
import re
from typing import Any, Dict

import httpx
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.dataset import Dataset
from app.models.schema_definition import SchemaDefinition
from app.engines.nl2sql.state import NL2SQLState
from app.services.ai_policy_service import AIPolicyService
from app.services.sql_validator import SQLValidator
from app.services.prompt_builder import PromptBuilder
from app.services.cost_tracker import CostTracker
from app.services.llm_client import get_llm_client
from app.prompts.nl2sql_system import NL2SQL_SYSTEM_PROMPT
from app.safety.prompt_guard import PromptGuard


async def get_schema_context(state: NL2SQLState, db: AsyncSession) -> Dict[str, Any]:
    """Retrieve schema context for the requested datasets."""
    tenant_id = state["tenant_id"]
    dataset_ids = state.get("dataset_ids", [])

    # Fetch datasets
    query = select(Dataset).where(Dataset.tenant_id == int(tenant_id))
    if dataset_ids:
        query = query.where(Dataset.id.in_([int(d) for d in dataset_ids]))

    result = await db.execute(query)
    datasets = list(result.scalars().all())

    if not datasets:
        return {
            "error_message": "No datasets found for the given criteria",
            "schema_context": "",
            "allowed_tables": [],
            "schema_columns": {},
        }

    # Fetch schema definitions for each dataset
    schema_parts = []
    allowed_tables = []
    schema_columns = {}

    for ds in datasets:
        allowed_tables.append(ds.table_name.lower())
        schema_result = await db.execute(
            select(SchemaDefinition).where(SchemaDefinition.dataset_id == ds.id)
        )
        columns = list(schema_result.scalars().all())

        col_defs = []
        col_names = []
        for col in columns:
            nullable = "NULL" if col.is_nullable else "NOT NULL"
            pk = " [PK]" if col.is_primary_key else ""
            col_defs.append(f"  {col.column_name} {col.data_type} {nullable}{pk}")
            col_names.append(col.column_name.lower())

        schema_columns[ds.table_name.lower()] = col_names
        schema_parts.append(
            f"Table: {ds.table_name}\n"
            f"Description: {ds.description or ds.display_name or ds.name}\n"
            f"Columns:\n" + "\n".join(col_defs)
        )

    schema_context = "\n\n".join(schema_parts)

    return {
        "schema_context": schema_context,
        "allowed_tables": allowed_tables,
        "schema_columns": schema_columns,
    }


async def check_policy(state: NL2SQLState, db: AsyncSession) -> Dict[str, Any]:
    """Check AI policy before SQL generation."""
    tenant_id = int(state["tenant_id"])
    policy_service = AIPolicyService(db)

    # Check if SQL generation is allowed
    allowed, message = await policy_service.check_sql_generation_allowed(tenant_id)
    if not allowed:
        return {
            "policy_ok": False,
            "policy_message": message,
            "error_message": message,
        }

    # Get SQL constraints
    constraints = await policy_service.get_sql_constraints(tenant_id)

    return {
        "policy_ok": True,
        "policy_message": message,
        "constraints": constraints,
    }


async def build_prompt(state: NL2SQLState, db: AsyncSession) -> Dict[str, Any]:
    """Build the NL2SQL prompt with schema context."""
    # Check for prompt injection
    guard = PromptGuard()
    query = state["query"]
    if guard.detect_injection(query):
        return {
            "error_message": "Query rejected: potential prompt injection detected",
        }

    builder = PromptBuilder(max_tokens=settings.PROMPT_MAX_TOKENS)
    constraints = state.get("constraints", {})

    prompt = builder.build_nl2sql_prompt(
        system_prompt=NL2SQL_SYSTEM_PROMPT,
        user_query=query,
        schema_context=state.get("schema_context", ""),
        constraints=constraints,
    )

    return {
        "system_prompt": prompt["system"],
        "user_prompt": prompt["user"],
        "prompt_text": f"{prompt['system']}\n\n{prompt['user']}",
    }


async def llm_generate_sql(state: NL2SQLState, db: AsyncSession) -> Dict[str, Any]:
    """Call the LLM to generate SQL using the unified client."""
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


def extract_sql(state: NL2SQLState) -> Dict[str, Any]:
    """Extract SQL from LLM output."""
    raw_output = state.get("raw_llm_output", "")

    # Try to extract from code block
    sql_match = re.search(r'```sql\s*\n?(.*?)\n?```', raw_output, re.DOTALL | re.IGNORECASE)
    if sql_match:
        sql = sql_match.group(1).strip()
    else:
        # Try to find SELECT statement
        select_match = re.search(r'(SELECT\s+.+?)(?:\n\n|\Z)', raw_output, re.DOTALL | re.IGNORECASE)
        if select_match:
            sql = select_match.group(1).strip()
        else:
            sql = raw_output.strip()

    # Clean up
    sql = sql.rstrip(";").strip()

    # Extract explanation if present
    explanation = ""
    explanation_match = re.search(r'(?:Explanation|Note|Comment):\s*(.+?)(?:\n\n|\Z)', raw_output, re.DOTALL | re.IGNORECASE)
    if explanation_match:
        explanation = explanation_match.group(1).strip()

    return {
        "extracted_sql": sql,
        "explanation": explanation,
    }


async def validate_sql(state: NL2SQLState, db: AsyncSession) -> Dict[str, Any]:
    """Validate the extracted SQL against safety rules and policy."""
    sql = state.get("extracted_sql", "")
    allowed_tables = set(state.get("allowed_tables", []))
    schema_columns = state.get("schema_columns", {})
    constraints = state.get("constraints", {})

    validator = SQLValidator(
        allowed_tables=allowed_tables,
        schema_columns=schema_columns,
        max_joins=constraints.get("max_sql_complexity", settings.SQL_MAX_JOINS),
        max_limit=constraints.get("max_result_rows", settings.SQL_MAX_ROWS),
    )

    result = validator.validate(sql)

    return {
        "validation_result": result.to_dict(),
        "is_valid": result.is_valid,
    }


async def retry_prompt(state: NL2SQLState, db: AsyncSession) -> Dict[str, Any]:
    """Build a retry prompt with validation error feedback."""
    retry_count = state.get("retry_count", 0) + 1
    validation_result = state.get("validation_result", {})
    errors = validation_result.get("errors", [])

    error_feedback = "\n".join(f"- {e}" for e in errors)
    retry_user_prompt = (
        f"{state.get('user_prompt', '')}\n\n"
        f"## Previous Attempt Failed\n"
        f"The following SQL was generated but failed validation:\n"
        f"```sql\n{state.get('extracted_sql', '')}\n```\n\n"
        f"Validation errors:\n{error_feedback}\n\n"
        f"Please fix these issues and generate a corrected SQL query."
    )

    return {
        "user_prompt": retry_user_prompt,
        "retry_count": retry_count,
    }


async def execute_sql(state: NL2SQLState, db: AsyncSession) -> Dict[str, Any]:
    """Execute the validated SQL and return results."""
    sql = state.get("extracted_sql", "")
    max_rows = state.get("max_rows", 1000)

    # Add LIMIT if not present
    if "LIMIT" not in sql.upper():
        sql = f"{sql} LIMIT {max_rows}"

    try:
        result = await db.execute(text(sql))
        rows = result.mappings().all()
        columns = list(rows[0].keys()) if rows else []
        data = [dict(row) for row in rows]

        return {
            "final_sql": sql,
            "columns": columns,
            "row_count": len(data),
            "data": data,
            "result": {
                "sql": sql,
                "columns": columns,
                "row_count": len(data),
                "data": data[:100],  # Limit response size
                "explanation": state.get("explanation", ""),
            },
        }
    except Exception as e:
        return {
            "error_message": f"SQL execution failed: {str(e)}",
            "final_sql": sql,
        }
