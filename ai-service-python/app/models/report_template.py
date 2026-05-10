"""ReportTemplate model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class ReportTemplate(BaseModel):
    __tablename__ = "report_template"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    format: Mapped[str] = mapped_column(String(16), nullable=False)
    template_file: Mapped[str] = mapped_column(String(512), nullable=True)
    variables: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    version: Mapped[int] = mapped_column(Integer, default=1)
    status: Mapped[str] = mapped_column(String(16), default="active")
