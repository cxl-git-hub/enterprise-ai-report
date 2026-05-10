"""Report generation LangGraph state definition."""

from typing import Any, Dict, List, Optional
from typing_extensions import TypedDict


class ReportState(TypedDict, total=False):
    """State for the Report Generation LangGraph workflow."""

    # Input
    tenant_id: str
    user_id: str
    title: str
    description: str
    dataset_ids: List[str]
    kpi_ids: List[str]
    template_id: Optional[str]
    analysis_queries: List[str]
    output_format: str
    date_range: Optional[Dict[str, str]]

    # KPI data
    kpi_data: Dict[str, Any]
    kpi_context: str

    # Analysis results
    analysis_results: List[Dict[str, Any]]
    data_context: str

    # Template
    template_context: str
    sections: List[Dict[str, Any]]

    # Prompt
    system_prompt: str
    user_prompt: str

    # LLM output
    raw_llm_output: str
    report_content: str

    # Validation
    is_valid: bool
    validation_errors: List[str]

    # Output
    file_url: Optional[str]

    # Tracking
    retry_count: int
    max_retries: int
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int
    cost_usd: float
    model_name: str
    error_message: str
