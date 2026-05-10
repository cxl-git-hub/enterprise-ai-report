"""AI Policy model for controlling AI behavior per tenant."""

import uuid
from sqlalchemy import String, Text, Integer, Boolean, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class AIPolicy(BaseModel):
    """AI policy configuration for a tenant."""

    __tablename__ = "ai_policies"

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
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    # SQL generation policies
    allow_sql_generation: Mapped[bool] = mapped_column(Boolean, default=True)
    allow_schema_access: Mapped[bool] = mapped_column(Boolean, default=True)
    allow_cross_dataset_join: Mapped[bool] = mapped_column(Boolean, default=False)
    max_sql_complexity: Mapped[int] = mapped_column(Integer, default=3)  # max JOINs
    allowed_sql_types: Mapped[list] = mapped_column(JSONB, default=["SELECT"])
    max_result_rows: Mapped[int] = mapped_column(Integer, default=10000)
    require_where_clause: Mapped[bool] = mapped_column(Boolean, default=True)

    # Prompt policies
    allowed_datasets: Mapped[list] = mapped_column(JSONB, nullable=True)  # dataset IDs
    blocked_tables: Mapped[list] = mapped_column(JSONB, nullable=True)
    custom_rules: Mapped[dict] = mapped_column(JSONB, nullable=True)

    # Relationships
    tenant = relationship("Tenant", back_populates="ai_policies", lazy="selectin")
