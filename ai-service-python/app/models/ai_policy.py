"""AiPolicy model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class AiPolicy(BaseModel):
    __tablename__ = "ai_policy"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    max_rows_returned: Mapped[int] = mapped_column(Integer, default=10000)
    max_execution_time: Mapped[int] = mapped_column(Integer, default=300)
    allowed_datasets: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    blocked_tables: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    status: Mapped[int] = mapped_column(Integer, default=1)
