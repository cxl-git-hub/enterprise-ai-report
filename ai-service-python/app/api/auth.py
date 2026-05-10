"""Authentication endpoints."""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_async_session
from app.core.dependencies import get_db
from app.schemas.auth import LoginRequest, LoginResponse
from app.schemas.common import ApiResponse
from app.services.auth_service import AuthService

router = APIRouter()


@router.post("/login", response_model=ApiResponse[LoginResponse])
async def login(
    request: LoginRequest,
    db: AsyncSession = Depends(get_db),
):
    """Authenticate user and return JWT token."""
    auth_service = AuthService(db)
    user = await auth_service.authenticate_user(request.email, request.password)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )

    access_token = auth_service.create_access_token(
        user_id=user.id,
        tenant_id=user.tenant_id,
        email=user.email,
        role=user.role,
    )

    from app.core.config import settings

    return ApiResponse(
        data=LoginResponse(
            access_token=access_token,
            token_type="bearer",
            expires_in=settings.JWT_EXPIRE_MINUTES * 60,
            user_id=user.id,
            email=user.email,
            role=user.role,
            tenant_id=user.tenant_id,
        )
    )


@router.post("/validate", response_model=ApiResponse)
async def validate_token(
    token: str,
    db: AsyncSession = Depends(get_db),
):
    """Validate a JWT token."""
    auth_service = AuthService(db)
    user = await auth_service.validate_token(token)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )

    return ApiResponse(data={"valid": True, "user": user})
