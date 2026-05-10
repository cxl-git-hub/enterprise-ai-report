"""Workflow model for automated reporting workflows."""

import uuid
from sqlalchemy import String, Text, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class Workflow(BaseModel):
    """Automated reporting workflow definition."""

    __tablename__ = "workflows"

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
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=True)
    workflow_type: Mapped[str] = mapped_column(String(50), nullable=False)  # nl2sql, analysis, report
    config: Mapped[dict] = mapped_column(JSONB, nullable=False, default=dict)
    schedule_cron: Mapped[str] = mapped_column(String(100), nullable=True)
    is_active: Mapped[bool] = mapped_column(default=True)
    report_template_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("report_templates.id"),
        nullable=True,
    )

    # Relationships
    runs = relationship("WorkflowRun", back_populates="workflow", lazy="selectin")
