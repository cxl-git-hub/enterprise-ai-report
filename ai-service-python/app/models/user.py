"""User model."""

from sqlalchemy import String, Integer, BigInteger
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class User(BaseModel):
    """User model with authentication fields."""

    __tablename__ = "sys_user"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False, index=True)
    username: Mapped[str] = mapped_column(String(64), nullable=False)
    password: Mapped[str] = mapped_column(String(256), nullable=False)
    real_name: Mapped[str] = mapped_column(String(64), nullable=True)
    email: Mapped[str] = mapped_column(String(128), nullable=True)
    phone: Mapped[str] = mapped_column(String(32), nullable=True)
    avatar: Mapped[str] = mapped_column(String(512), nullable=True)
    status: Mapped[int] = mapped_column(Integer, default=1)
    last_login_time: Mapped[str] = mapped_column(String(32), nullable=True)
    last_login_ip: Mapped[str] = mapped_column(String(64), nullable=True)
