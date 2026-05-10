"""Schema definition model for column metadata."""

import uuid
from sqlalchemy import String, Text, Integer, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class SchemaDefinition(BaseModel):
    """Column-level schema definition for a dataset."""

    __tablename__ = "schema_definitions"

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
    column_name: Mapped[str] = mapped_column(String(255), nullable=False)
    data_type: Mapped[str] = mapped_column(String(100), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=True)
    is_primary_key: Mapped[bool] = mapped_column(default=False)
    is_nullable: Mapped[bool] = mapped_column(default=True)
    ordinal_position: Mapped[int] = mapped_column(Integer, nullable=True)
    sample_values: Mapped[str] = mapped_column(Text, nullable=True)

    # Relationships
    dataset = relationship("Dataset", back_populates="schema_definitions", lazy="selectin")
