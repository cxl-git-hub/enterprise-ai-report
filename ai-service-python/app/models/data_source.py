"""DataSource model for database connections."""

from sqlalchemy import String, Text, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class DataSource(BaseModel):
    """External data source configuration."""

    __tablename__ = "data_source"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    source_name: Mapped[str] = mapped_column(String(128), nullable=False)
    source_type: Mapped[str] = mapped_column(String(32), nullable=False)
    connection_config: Mapped[str] = mapped_column(Text, nullable=False)  # JSON
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    status: Mapped[int] = mapped_column(Integer, default=1)
    last_sync_time: Mapped[str] = mapped_column(String(32), nullable=True)
    created_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
