"""AI Trace model for tracking LLM execution traces."""

from sqlalchemy import String, Text, Integer, BigInteger, Numeric
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class AITrace(BaseModel):
    """Execution trace for an AI operation."""

    __tablename__ = "ai_execution_trace"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    trace_id: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    run_id: Mapped[str] = mapped_column(String(64), nullable=True, index=True)
    node_id: Mapped[str] = mapped_column(String(128), nullable=True)
    ai_task_type: Mapped[str] = mapped_column(String(32), nullable=False)
    input_prompt: Mapped[str] = mapped_column(Text, nullable=False)
    prompt_tokens: Mapped[int] = mapped_column(Integer, default=0)
    completion_tokens: Mapped[int] = mapped_column(Integer, default=0)
    total_tokens: Mapped[int] = mapped_column(Integer, default=0)
    model_name: Mapped[str] = mapped_column(String(64), nullable=True)
    model_config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    raw_output: Mapped[str] = mapped_column(Text, nullable=True)
    validated_output: Mapped[str] = mapped_column(Text, nullable=True)
    validation_passed: Mapped[int] = mapped_column(Integer, nullable=True)
    validation_errors: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    retry_count: Mapped[int] = mapped_column(Integer, default=0)
    latency_ms: Mapped[int] = mapped_column(BigInteger, nullable=True)
    cost: Mapped[float] = mapped_column(Numeric(10, 6), default=0)
    status: Mapped[str] = mapped_column(String(16), default="pending")
