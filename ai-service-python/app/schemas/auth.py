"""Authentication schemas."""

from typing import Optional
from pydantic import BaseModel


class TokenPayload(BaseModel):
    """JWT token payload."""

    sub: str
    user_id: int
    tenant_id: int
    username: str
    role: str
    exp: Optional[int] = None


class LoginRequest(BaseModel):
    """Login request body."""

    username: str
    password: str


class LoginResponse(BaseModel):
    """Login response body."""

    access_token: str
    token_type: str = "bearer"
    expires_in: int
    user_id: int
    username: str
    role: str
    tenant_id: int
