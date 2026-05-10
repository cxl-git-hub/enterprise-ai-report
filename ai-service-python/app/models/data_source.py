"""DataSource model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class DataSource(BaseModel):
    __tablename__ = "data_source"

    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    type: Mapped[str] = mapped_column(String(32), nullable=False)
    host: Mapped[str] = mapped_column(String(256), nullable=True)
    port: Mapped[int] = mapped_column(Integer, nullable=True)
    database_name: Mapped[str] = mapped_column(String(128), nullable=True)
    username: Mapped[str] = mapped_column(String(128), nullable=True)
    encrypted_password: Mapped[str] = mapped_column(String(512), nullable=True)
    connection_url: Mapped[str] = mapped_column(String(512), nullable=True)
    config: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    status: Mapped[int] = mapped_column(Integer, default=1)
    last_test_at: Mapped[str] = mapped_column(String(32), nullable=True)
    last_test_result: Mapped[str] = mapped_column(String(256), nullable=True)
