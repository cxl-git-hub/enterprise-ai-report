"""DataSource model for database connections."""

import uuid
from sqlalchemy import String, Text, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class DataSource(BaseModel):
    """External data source configuration."""

    __tablename__ = "data_sources"

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
    source_type: Mapped[str] = mapped_column(String(50), nullable=False)  # postgres, mysql, etc.
    connection_config: Mapped[dict] = mapped_column(JSONB, nullable=False)  # encrypted in production
    description: Mapped[str] = mapped_column(Text, nullable=True)
    is_active: Mapped[bool] = mapped_column(default=True)

    # Relationships
    tenant = relationship("Tenant", back_populates="data_sources", lazy="selectin")
    datasets = relationship("Dataset", back_populates="data_source", lazy="selectin")
