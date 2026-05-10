"""KPI definition model."""

import uuid
from sqlalchemy import String, Text, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class KPIDefinition(BaseModel):
    """KPI (Key Performance Indicator) definition."""

    __tablename__ = "kpi_definitions"

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
    dataset_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("datasets.id"),
        nullable=False,
        index=True,
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    display_name: Mapped[str] = mapped_column(String(255), nullable=True)
    description: Mapped[str] = mapped_column(Text, nullable=True)
    aggregation_type: Mapped[str] = mapped_column(String(50), nullable=False)  # sum, avg, count, etc.
    column_name: Mapped[str] = mapped_column(String(255), nullable=False)
    formula: Mapped[str] = mapped_column(Text, nullable=True)  # Custom SQL formula
    filters: Mapped[dict] = mapped_column(JSONB, nullable=True)
    unit: Mapped[str] = mapped_column(String(50), nullable=True)

    # Relationships
    dataset = relationship("Dataset", back_populates="kpi_definitions", lazy="selectin")
