"""FastAPI dependencies for database sessions, auth, and tenant resolution."""

from typing import Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_async_session
from app.core.config import settings
from app.services.auth_service import AuthService

security = HTTPBearer()


async def get_db() -> AsyncSession:
    """Dependency that yields a database session."""
    async for session in get_async_session():
        yield session


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """Validate JWT token and return current user info."""
    token = credentials.credentials
    auth_service = AuthService(db)
    user = await auth_service.validate_token(token)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )
    return user


async def get_tenant(
    current_user: dict = Depends(get_current_user),
) -> dict:
    """Extract tenant context from the authenticated user."""
    tenant_id = current_user.get("tenant_id")
    if not tenant_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No tenant context available",
        )
    return {"tenant_id": tenant_id, "user_id": current_user["id"]}
