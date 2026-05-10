"""Report template model."""

from sqlalchemy import String, Text, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class ReportTemplate(BaseModel):
    """Template for generating reports."""

    __tablename__ = "report_template"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    template_code: Mapped[str] = mapped_column(String(128), nullable=False)
    template_name: Mapped[str] = mapped_column(String(128), nullable=False)
    version: Mapped[int] = mapped_column(Integer, default=1)
    output_format: Mapped[str] = mapped_column(String(16), nullable=False)
    template_file_path: Mapped[str] = mapped_column(String(512), nullable=False)
    schema_ids: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    kpi_ids: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    prompt_ids: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    variables: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    status: Mapped[str] = mapped_column(String(16), default="active")
    created_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
