"""Workflow model for automated reporting workflows."""

from sqlalchemy import String, Text, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Workflow(BaseModel):
    """Automated reporting workflow definition."""

    __tablename__ = "workflow_definition"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    workflow_code: Mapped[str] = mapped_column(String(128), nullable=False)
    workflow_name: Mapped[str] = mapped_column(String(128), nullable=False)
    version: Mapped[int] = mapped_column(Integer, default=1)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    dag_definition: Mapped[str] = mapped_column(Text, nullable=False)  # JSON
    trigger_type: Mapped[str] = mapped_column(String(32), default="manual")
    cron_expression: Mapped[str] = mapped_column(String(64), nullable=True)
    timeout_seconds: Mapped[int] = mapped_column(Integer, default=3600)
    max_retries: Mapped[int] = mapped_column(Integer, default=3)
    retry_delay_seconds: Mapped[int] = mapped_column(Integer, default=60)
    status: Mapped[str] = mapped_column(String(16), default="draft")
    created_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
