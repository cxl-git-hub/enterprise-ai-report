"""Authentication service with JWT validation."""

from datetime import datetime, timedelta, timezone
from typing import Optional

from jose import JWTError, jwt
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.user import User

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AuthService:
    """Handles authentication, JWT creation/validation, and user lookup."""

    def __init__(self, db: AsyncSession):
        self.db = db

    def hash_password(self, password: str) -> str:
        """Hash a plaintext password."""
        return pwd_context.hash(password)

    def verify_password(self, plain_password: str, hashed_password: str) -> bool:
        """Verify a password against its hash."""
        return pwd_context.verify(plain_password, hashed_password)

    def create_access_token(
        self,
        user_id: int,
        tenant_id: int,
        username: str,
        role: str,
        expires_delta: Optional[timedelta] = None,
    ) -> str:
        """Create a JWT access token."""
        if expires_delta is None:
            expires_delta = timedelta(minutes=settings.JWT_EXPIRE_MINUTES)

        expire = datetime.now(timezone.utc) + expires_delta
        payload = {
            "sub": username,
            "userId": user_id,
            "tenantId": tenant_id,
            "username": username,
            "type": "access",
            "exp": expire,
            "iat": datetime.now(timezone.utc),
        }
        return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)

    async def validate_token(self, token: str) -> Optional[dict]:
        """Validate a JWT token and return the user info.

        Supports both Java backend format (userId/tenantId) and legacy format (user_id/tenant_id).
        """
        try:
            payload = jwt.decode(
                token,
                settings.JWT_SECRET_KEY,
                algorithms=[settings.JWT_ALGORITHM],
            )

            # Support Java backend JWT format: userId (Long), tenantId (Long)
            user_id = payload.get("userId") or payload.get("user_id")
            tenant_id = payload.get("tenantId") or payload.get("tenant_id")

            if not user_id:
                return None

            result = await self.db.execute(
                select(User).where(User.id == int(user_id), User.status == 1)
            )
            user = result.scalar_one_or_none()
            if not user:
                return None

            return {
                "id": user.id,
                "tenant_id": user.tenant_id or int(tenant_id) if tenant_id else None,
                "username": user.username,
                "email": user.email,
                "real_name": user.real_name,
            }
        except (JWTError, ValueError, TypeError):
            return None

    async def authenticate_user(self, username: str, password: str) -> Optional[User]:
        """Authenticate a user by username and password."""
        result = await self.db.execute(
            select(User).where(User.username == username, User.status == 1)
        )
        user = result.scalar_one_or_none()
        if not user:
            return None
        if not self.verify_password(password, user.password):
            return None
        return user
