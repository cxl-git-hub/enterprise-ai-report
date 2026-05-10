"""Report template model."""

import uuid
from sqlalchemy import String, Text, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class ReportTemplate(BaseModel):
    """Template for generating reports."""

    __tablename__ = "report_templates"

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
    template_type: Mapped[str] = mapped_column(String(50), nullable=False)  # pdf, html, markdown
    content_template: Mapped[str] = mapped_column(Text, nullable=False)  # Jinja2 template
    sections: Mapped[dict] = mapped_column(JSONB, nullable=False, default=list)
    style_config: Mapped[dict] = mapped_column(JSONB, nullable=True)
    is_default: Mapped[bool] = mapped_column(default=False)
