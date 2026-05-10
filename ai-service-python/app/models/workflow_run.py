"""WorkflowRun model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text, Numeric
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class WorkflowRun(BaseModel):
    __tablename__ = "workflow_run"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    workflow_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    workflow_version: Mapped[int] = mapped_column(Integer, nullable=False)
    run_id: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    trigger_type: Mapped[str] = mapped_column(String(32), nullable=False)
    triggered_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
    state: Mapped[str] = mapped_column(String(16), default="PENDING")
    current_node_id: Mapped[str] = mapped_column(String(128), nullable=True)
    input_params: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    output_result: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    error_message: Mapped[str] = mapped_column(Text, nullable=True)
    start_time: Mapped[str] = mapped_column(String(32), nullable=True)
    end_time: Mapped[str] = mapped_column(String(32), nullable=True)
    duration_ms: Mapped[int] = mapped_column(BigInteger, nullable=True)
    total_tokens: Mapped[int] = mapped_column(BigInteger, default=0)
    total_cost: Mapped[float] = mapped_column(Numeric(10, 4), default=0)
