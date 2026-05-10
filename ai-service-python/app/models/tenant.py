"""Tenant model - aligned with database schema."""

from sqlalchemy import String, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Tenant(BaseModel):
    __tablename__ = "tenant"

    name: Mapped[str] = mapped_column(String(128), nullable=False)
    code: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    contact_name: Mapped[str] = mapped_column(String(64), nullable=True)
    contact_email: Mapped[str] = mapped_column(String(128), nullable=True)
    contact_phone: Mapped[str] = mapped_column(String(32), nullable=True)
    plan_type: Mapped[str] = mapped_column(String(32), default="standard")
    status: Mapped[int] = mapped_column(Integer, default=1)
    max_users: Mapped[int] = mapped_column(Integer, default=100)
    max_datasources: Mapped[int] = mapped_column(Integer, default=10)
    max_datasets: Mapped[int] = mapped_column(Integer, default=50)
    max_ai_calls_per_day: Mapped[int] = mapped_column(Integer, default=1000)
