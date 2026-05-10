"""WorkflowRun model for tracking workflow executions."""

import uuid
from datetime import datetime
from sqlalchemy import String, Text, ForeignKey, DateTime
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class WorkflowRun(BaseModel):
    """A single execution of a workflow."""

    __tablename__ = "workflow_runs"

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
    workflow_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("workflows.id"),
        nullable=False,
        index=True,
    )
    status: Mapped[str] = mapped_column(String(50), default="pending")  # pending, running, completed, failed
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=True)
    completed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=True)
    error_message: Mapped[str] = mapped_column(Text, nullable=True)
    result_data: Mapped[dict] = mapped_column(JSONB, nullable=True)
    total_cost: Mapped[float] = mapped_column(nullable=True, default=0.0)

    # Relationships
    workflow = relationship("Workflow", back_populates="runs", lazy="selectin")
    traces = relationship("AITrace", back_populates="workflow_run", lazy="selectin")
