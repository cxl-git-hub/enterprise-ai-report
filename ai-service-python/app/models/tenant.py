"""Tenant model for multi-tenancy."""

from sqlalchemy import String, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class Tenant(BaseModel):
    """Organization/tenant model."""

    __tablename__ = "tenant"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_code: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    tenant_name: Mapped[str] = mapped_column(String(128), nullable=False)
    contact_name: Mapped[str] = mapped_column(String(64), nullable=True)
    contact_email: Mapped[str] = mapped_column(String(128), nullable=True)
    contact_phone: Mapped[str] = mapped_column(String(32), nullable=True)
    plan_type: Mapped[str] = mapped_column(String(32), default="standard")
    max_users: Mapped[int] = mapped_column(Integer, default=10)
    max_datasets: Mapped[int] = mapped_column(Integer, default=50)
    max_ai_calls_per_day: Mapped[int] = mapped_column(Integer, default=1000)
    status: Mapped[int] = mapped_column(Integer, default=1)
    expire_time: Mapped[str] = mapped_column(String(32), nullable=True)
