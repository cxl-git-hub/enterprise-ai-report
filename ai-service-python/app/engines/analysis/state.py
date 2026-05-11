"""Analysis LangGraph state definition."""

from typing import Any, Dict, List, Optional
from typing_extensions import TypedDict


class AnalysisState(TypedDict, total=False):
    """State for the Analysis LangGraph workflow."""

    # Input
    tenant_id: str
    user_id: str
    query: str
    dataset_ids: List[str]
    sql: Optional[str]
    analysis_type: str
    output_format: str
    context: Optional[List[str]]  # Multi-turn conversation context

    # Data context
    schema_context: str
    data_context: str
    raw_data: List[Dict[str, Any]]

    # Prompt
    system_prompt: str
    user_prompt: str

    # LLM output
    raw_llm_output: str
    parsed_result: Dict[str, Any]

    # Validation
    is_valid: bool
    validation_errors: List[str]

    # Output
    result: Dict[str, Any]
    insights: List[str]
    recommendations: List[str]
    chart_config: Optional[Dict[str, Any]]

    # Tracking
    retry_count: int
    max_retries: int
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int
    cost_usd: float
    model_name: str
    error_message: str
