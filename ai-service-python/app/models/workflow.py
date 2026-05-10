"""Workflow model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class WorkflowDefinition(BaseModel):
    __tablename__ = "workflow_definition"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    dag_definition: Mapped[str] = mapped_column(Text, nullable=False)  # JSON
    trigger_type: Mapped[str] = mapped_column(String(32), default="manual")
    cron_expression: Mapped[str] = mapped_column(String(64), nullable=True)
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    version: Mapped[int] = mapped_column(Integer, default=1)
    state: Mapped[str] = mapped_column(String(16), nullable=True)
    status: Mapped[str] = mapped_column(String(16), default="draft")
