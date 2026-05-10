"""AI Policy model for controlling AI behavior per tenant."""

from sqlalchemy import String, Text, Integer, BigInteger, Boolean
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class AIPolicy(BaseModel):
    """AI policy configuration for a tenant."""

    __tablename__ = "ai_policy"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    policy_code: Mapped[str] = mapped_column(String(128), nullable=False)
    policy_name: Mapped[str] = mapped_column(String(128), nullable=False)
    policy_type: Mapped[str] = mapped_column(String(32), nullable=False)
    rules: Mapped[str] = mapped_column(Text, nullable=False)  # JSON
    description: Mapped[str] = mapped_column(String(512), nullable=True)
    status: Mapped[int] = mapped_column(Integer, default=1)
