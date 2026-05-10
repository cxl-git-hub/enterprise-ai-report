"""Trace and validation schemas."""

from typing import Any, Dict, List, Optional
from datetime import datetime
from pydantic import BaseModel


class TraceResponse(BaseModel):
    """AI execution trace response - aligned with ai_execution_trace table."""

    id: int
    traceId: Optional[str] = None
    aiTaskType: Optional[str] = None
    status: Optional[str] = None
    inputPrompt: Optional[str] = None
    rawOutput: Optional[str] = None
    validatedOutput: Optional[str] = None
    promptTokens: int = 0
    completionTokens: int = 0
    totalTokens: int = 0
    modelName: Optional[str] = None
    retryCount: int = 0
    latencyMs: Optional[int] = None
    cost: float = 0.0
    createdAt: Optional[datetime] = None

    model_config = {"from_attributes": True}

    @classmethod
    def model_validate(cls, obj, **kwargs):
        """Map database fields to response fields."""
        if hasattr(obj, '__dict__'):
            data = {
                "id": obj.id,
                "traceId": getattr(obj, 'trace_id', None),
                "aiTaskType": getattr(obj, 'ai_task_type', None),
                "status": getattr(obj, 'status', None),
                "inputPrompt": getattr(obj, 'input_prompt', None),
                "rawOutput": getattr(obj, 'raw_output', None),
                "validatedOutput": getattr(obj, 'validated_output', None),
                "promptTokens": getattr(obj, 'prompt_tokens', 0),
                "completionTokens": getattr(obj, 'completion_tokens', 0),
                "totalTokens": getattr(obj, 'total_tokens', 0),
                "modelName": getattr(obj, 'model_name', None),
                "retryCount": getattr(obj, 'retry_count', 0),
                "latencyMs": getattr(obj, 'latency_ms', None),
                "cost": float(getattr(obj, 'cost', 0) or 0),
                "createdAt": getattr(obj, 'created_at', None),
            }
            return cls(**data)
        return super().model_validate(obj, **kwargs)


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
