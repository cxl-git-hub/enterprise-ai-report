"""SchemaDefinition model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class SchemaDefinition(BaseModel):
    __tablename__ = "schema_definition"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    dataset_id: Mapped[int] = mapped_column(BigInteger, nullable=True, index=True)
    columns: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    metrics: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    dimensions: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    version: Mapped[int] = mapped_column(Integer, default=1)
    status: Mapped[str] = mapped_column(String(16), default="active")
