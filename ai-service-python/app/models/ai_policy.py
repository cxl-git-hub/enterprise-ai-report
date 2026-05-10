"""AiPolicy model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class AiPolicy(BaseModel):
    __tablename__ = "ai_policy"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    allow_sql_generation: Mapped[int] = mapped_column(Integer, default=1)
    allow_cross_dataset_join: Mapped[int] = mapped_column(Integer, default=0)
    allow_data_modification: Mapped[int] = mapped_column(Integer, default=0)
    max_rows_returned: Mapped[int] = mapped_column(Integer, default=1000)
    max_execution_time: Mapped[int] = mapped_column(Integer, default=30)
    allowed_datasets: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    blocked_tables: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    status: Mapped[int] = mapped_column(Integer, default=1)
