"""Trace and validation schemas."""

from typing import Any, Dict, List, Optional
from uuid import UUID
from datetime import datetime
from pydantic import BaseModel


class TraceResponse(BaseModel):
    """AI execution trace response."""

    id: UUID
    trace_type: str
    status: str
    input_query: Optional[str] = None
    extracted_sql: Optional[str] = None
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0
    cost_usd: float = 0.0
    model_name: Optional[str] = None
    retry_count: int = 0
    duration_ms: Optional[int] = None
    error_message: Optional[str] = None
    created_at: datetime

    model_config = {"from_attributes": True}


class SqlValidationResponse(BaseModel):
    """SQL validation result."""

    is_valid: bool
    errors: List[str] = []
    warnings: List[str] = []
    tables_found: List[str] = []
    columns_found: List[str] = []
    has_where_clause: bool = False
    join_count: int = 0
    limit_value: Optional[int] = None
