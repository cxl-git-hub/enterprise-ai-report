"""Dataset model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Dataset(BaseModel):
    __tablename__ = "dataset"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    data_source_id: Mapped[int] = mapped_column(BigInteger, nullable=True, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    table_name: Mapped[str] = mapped_column(String(128), nullable=True)
    query_sql: Mapped[str] = mapped_column(Text, nullable=True)
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    status: Mapped[int] = mapped_column(Integer, default=1)
    last_sync_at: Mapped[str] = mapped_column(String(32), nullable=True)
    row_count: Mapped[int] = mapped_column(BigInteger, default=0)
