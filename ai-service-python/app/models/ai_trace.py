"""AI Trace model for tracking LLM execution traces."""

import uuid
from sqlalchemy import String, Text, Integer, Float, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class AITrace(BaseModel):
    """Execution trace for an AI operation."""

    __tablename__ = "ai_traces"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("tenants.id"),
        nullable=False,
        index=True,
    )
    workflow_run_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("workflow_runs.id"),
        nullable=True,
        index=True,
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id"),
        nullable=True,
        index=True,
    )
    trace_type: Mapped[str] = mapped_column(String(50), nullable=False)  # nl2sql, analysis, report
    status: Mapped[str] = mapped_column(String(50), default="pending")  # pending, success, failed

    # Input
    input_query: Mapped[str] = mapped_column(Text, nullable=True)
    prompt_text: Mapped[str] = mapped_column(Text, nullable=True)

    # Output
    raw_llm_output: Mapped[str] = mapped_column(Text, nullable=True)
    extracted_sql: Mapped[str] = mapped_column(Text, nullable=True)
    validation_result: Mapped[dict] = mapped_column(JSONB, nullable=True)
    final_output: Mapped[dict] = mapped_column(JSONB, nullable=True)
    error_message: Mapped[str] = mapped_column(Text, nullable=True)

    # Cost tracking
    prompt_tokens: Mapped[int] = mapped_column(Integer, default=0)
    completion_tokens: Mapped[int] = mapped_column(Integer, default=0)
    total_tokens: Mapped[int] = mapped_column(Integer, default=0)
    cost_usd: Mapped[float] = mapped_column(Float, default=0.0)

    # Metadata
    model_name: Mapped[str] = mapped_column(String(100), nullable=True)
    retry_count: Mapped[int] = mapped_column(Integer, default=0)
    duration_ms: Mapped[int] = mapped_column(Integer, nullable=True)
    metadata: Mapped[dict] = mapped_column(JSONB, nullable=True)

    # Relationships
    workflow_run = relationship("WorkflowRun", back_populates="traces", lazy="selectin")
