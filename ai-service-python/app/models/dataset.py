"""Dataset model representing a table/view in a data source."""

from sqlalchemy import String, Text, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Dataset(BaseModel):
    """Dataset (table or view) within a data source."""

    __tablename__ = "dataset"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    source_id: Mapped[int] = mapped_column(BigInteger, nullable=True, index=True)
    dataset_name: Mapped[str] = mapped_column(String(128), nullable=False)
    dataset_type: Mapped[str] = mapped_column(String(32), nullable=False)
    storage_location: Mapped[str] = mapped_column(String(512), nullable=True)
    row_count: Mapped[int] = mapped_column(BigInteger, default=0)
    column_count: Mapped[int] = mapped_column(Integer, default=0)
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    tags: Mapped[str] = mapped_column(Text, nullable=True)  # JSON
    status: Mapped[int] = mapped_column(Integer, default=1)
    created_by: Mapped[int] = mapped_column(BigInteger, nullable=True)
