"""KPIDefinition model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class KPIDefinition(BaseModel):
    __tablename__ = "kpi_definition"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    schema_id: Mapped[int] = mapped_column(BigInteger, nullable=True, index=True)
    dataset_id: Mapped[int] = mapped_column(BigInteger, nullable=True)
    expression: Mapped[str] = mapped_column(Text, nullable=False)
    unit: Mapped[str] = mapped_column(String(32), nullable=True)
    aggregation_type: Mapped[str] = mapped_column(String(32), nullable=True)
    filter_condition: Mapped[str] = mapped_column(Text, nullable=True)
    group_by: Mapped[str] = mapped_column(Text, nullable=True)
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    version: Mapped[int] = mapped_column(Integer, default=1)
    status: Mapped[str] = mapped_column(String(16), default="active")
