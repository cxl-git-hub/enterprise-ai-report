"""Authentication schemas."""

from typing import Optional
from uuid import UUID
from pydantic import BaseModel, EmailStr


class TokenPayload(BaseModel):
    """JWT token payload."""

    sub: str
    user_id: UUID
    tenant_id: UUID
    email: str
    role: str
    exp: Optional[int] = None


class LoginRequest(BaseModel):
    """Login request body."""

    email: EmailStr
    password: str


class LoginResponse(BaseModel):
    """Login response body."""

    access_token: str
    token_type: str = "bearer"
    expires_in: int
    user_id: UUID
    email: str
    role: str
    tenant_id: UUID
