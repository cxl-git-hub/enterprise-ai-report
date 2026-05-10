"""NL2SQL LangGraph state definition."""

from typing import Any, Dict, List, Optional
from typing_extensions import TypedDict


class NL2SQLState(TypedDict, total=False):
    """State for the NL2SQL LangGraph workflow."""

    # Input
    tenant_id: str
    user_id: str
    query: str
    dataset_ids: List[str]
    max_rows: int
    include_explanation: bool

    # Schema context
    schema_context: str
    allowed_tables: List[str]
    schema_columns: Dict[str, List[str]]

    # Policy
    policy_ok: bool
    policy_message: str
    constraints: Dict[str, Any]

    # Prompt
    system_prompt: str
    user_prompt: str
    prompt_text: str

    # LLM output
    raw_llm_output: str
    extracted_sql: str
    explanation: str

    # Validation
    validation_result: Dict[str, Any]
    is_valid: bool

    # Execution
    final_sql: str
    columns: List[str]
    row_count: int
    data: List[Dict[str, Any]]

    # Tracking
    retry_count: int
    max_retries: int
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int
    cost_usd: float
    model_name: str
    error_message: str
    result: Dict[str, Any]
