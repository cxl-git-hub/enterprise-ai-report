"""Dataset model representing a table/view in a data source."""

import uuid
from sqlalchemy import String, Text, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class Dataset(BaseModel):
    """Dataset (table or view) within a data source."""

    __tablename__ = "datasets"

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
    data_source_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("data_sources.id"),
        nullable=False,
        index=True,
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    display_name: Mapped[str] = mapped_column(String(255), nullable=True)
    description: Mapped[str] = mapped_column(Text, nullable=True)
    table_name: Mapped[str] = mapped_column(String(255), nullable=False)
    columns_meta: Mapped[dict] = mapped_column(JSONB, nullable=False, default=dict)

    # Relationships
    data_source = relationship("DataSource", back_populates="datasets", lazy="selectin")
    schema_definitions = relationship("SchemaDefinition", back_populates="dataset", lazy="selectin")
    kpi_definitions = relationship("KPIDefinition", back_populates="dataset", lazy="selectin")
