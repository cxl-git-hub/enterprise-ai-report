"""PromptTemplate model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class PromptTemplate(BaseModel):
    __tablename__ = "prompt_template"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    schema_id: Mapped[int] = mapped_column(BigInteger, nullable=True)
    template_content: Mapped[str] = mapped_column(Text, nullable=False)
    variables: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    version: Mapped[int] = mapped_column(Integer, default=1)
    status: Mapped[str] = mapped_column(String(16), default="active")
