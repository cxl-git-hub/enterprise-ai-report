"""AI operation schemas."""

from typing import Any, Dict, List, Optional
from uuid import UUID
from pydantic import BaseModel, Field


class NL2SQLRequest(BaseModel):
    """Natural language to SQL request."""

    query: str = Field(..., min_length=1, max_length=5000, description="Natural language query")
    dataset_ids: Optional[List[UUID]] = Field(None, description="Restrict to specific datasets")
    max_rows: int = Field(1000, ge=1, le=10000, description="Maximum result rows")
    include_explanation: bool = Field(True, description="Include SQL explanation")


class NL2SQLResponse(BaseModel):
    """NL2SQL response."""

    sql: str
    explanation: Optional[str] = None
    columns: List[str] = []
    row_count: int = 0
    data: List[Dict[str, Any]] = []
    trace_id: UUID


class AnalysisRequest(BaseModel):
    """Data analysis request."""

    query: str = Field(..., min_length=1, max_length=5000, description="Analysis question")
    dataset_ids: List[UUID] = Field(..., min_length=1, description="Datasets to analyze")
    sql: Optional[str] = Field(None, description="Pre-generated SQL to use")
    analysis_type: str = Field("general", description="Type of analysis: general, trend, comparison, anomaly")
    output_format: str = Field("json", description="Output format: json, chart_config")


class AnalysisResponse(BaseModel):
    """Analysis response."""

    analysis: Dict[str, Any]
    insights: List[str] = []
    recommendations: List[str] = []
    chart_config: Optional[Dict[str, Any]] = None
    trace_id: UUID


class ReportGenerateRequest(BaseModel):
    """Report generation request."""

    title: str = Field(..., min_length=1, max_length=500)
    description: Optional[str] = None
    dataset_ids: List[UUID] = Field(..., min_length=1)
    kpi_ids: Optional[List[UUID]] = None
    template_id: Optional[UUID] = None
    analysis_queries: List[str] = Field(default_factory=list)
    output_format: str = Field("markdown", description="Output format: markdown, html, pdf")
    date_range: Optional[Dict[str, str]] = None


class ReportGenerateResponse(BaseModel):
    """Report generation response."""

    report_id: UUID
    title: str
    content: str
    format: str
    sections: List[Dict[str, Any]] = []
    file_url: Optional[str] = None
    trace_id: UUID
