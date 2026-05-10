"""Prompt template model."""

from sqlalchemy import String, Text, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class PromptTemplate(BaseModel):
    """Reusable prompt template for AI operations."""

    __tablename__ = "prompt_template"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    prompt_code: Mapped[str] = mapped_column(String(128), nullable=False)
    prompt_name: Mapped[str] = mapped_column(String(128), nullable=False)
    version: Mapped[int] = mapped_column(Integer, default=1)
    schema_id: Mapped[int] = mapped_column(BigInteger, nullable=True)
    schema_version: Mapped[int] = mapped_column(Integer, nullable=True)
    prompt_type: Mapped[str] = mapped_column(String(32), nullable=False)
    system_prompt: Mapped[str] = mapped_column(Text, nullable=False)
    user_prompt_template: Mapped[str] = mapped_column(Text, nullable=False)
    output_schema: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    model_config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    status: Mapped[str] = mapped_column(String(16), default="active")
    created_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
