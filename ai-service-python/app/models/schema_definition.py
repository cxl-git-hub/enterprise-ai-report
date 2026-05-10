"""Schema definition model for column metadata."""

from sqlalchemy import String, Text, Integer, BigInteger, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class SchemaDefinition(BaseModel):
    """Column-level schema definition for a dataset."""

    __tablename__ = "schema_definition"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    schema_code: Mapped[str] = mapped_column(String(128), nullable=False)
    schema_name: Mapped[str] = mapped_column(String(128), nullable=False)
    version: Mapped[int] = mapped_column(Integer, default=1)
    dataset_id: Mapped[int] = mapped_column(BigInteger, nullable=True, index=True)
    column_definitions: Mapped[str] = mapped_column(Text, nullable=False)  # JSON
    validation_rules: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    status: Mapped[str] = mapped_column(String(16), default="active")
    created_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
