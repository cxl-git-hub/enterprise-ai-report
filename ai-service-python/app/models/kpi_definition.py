"""KPI definition model."""

from sqlalchemy import String, Text, Integer, BigInteger, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class KPIDefinition(BaseModel):
    """KPI (Key Performance Indicator) definition."""

    __tablename__ = "kpi_definition"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    kpi_code: Mapped[str] = mapped_column(String(128), nullable=False)
    kpi_name: Mapped[str] = mapped_column(String(128), nullable=False)
    version: Mapped[int] = mapped_column(Integer, default=1)
    schema_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    schema_version: Mapped[int] = mapped_column(Integer, nullable=False)
    kpi_type: Mapped[str] = mapped_column(String(32), nullable=False)
    expression: Mapped[str] = mapped_column(Text, nullable=False)
    dimensions: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    filters: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    unit: Mapped[str] = mapped_column(String(32), nullable=True)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    business_explanation: Mapped[str] = mapped_column(Text, nullable=True)
    status: Mapped[str] = mapped_column(String(16), default="active")
    created_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
