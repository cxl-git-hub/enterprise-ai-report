"""Authentication service with JWT validation."""

from datetime import datetime, timedelta, timezone
from typing import Optional
from uuid import UUID

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
        user_id: UUID,
        tenant_id: UUID,
        email: str,
        role: str,
        expires_delta: Optional[timedelta] = None,
    ) -> str:
        """Create a JWT access token."""
        if expires_delta is None:
            expires_delta = timedelta(minutes=settings.JWT_EXPIRE_MINUTES)

        expire = datetime.now(timezone.utc) + expires_delta
        payload = {
            "sub": str(user_id),
            "user_id": str(user_id),
            "tenant_id": str(tenant_id),
            "email": email,
            "role": role,
            "exp": expire,
            "iat": datetime.now(timezone.utc),
        }
        return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)

    async def validate_token(self, token: str) -> Optional[dict]:
        """Validate a JWT token and return the user info."""
        try:
            payload = jwt.decode(
                token,
                settings.JWT_SECRET_KEY,
                algorithms=[settings.JWT_ALGORITHM],
            )
            user_id = payload.get("user_id")
            if not user_id:
                return None

            result = await self.db.execute(
                select(User).where(User.id == UUID(user_id), User.is_active == True)
            )
            user = result.scalar_one_or_none()
            if not user:
                return None

            return {
                "id": user.id,
                "tenant_id": user.tenant_id,
                "email": user.email,
                "role": user.role,
                "full_name": user.full_name,
            }
        except JWTError:
            return None

    async def authenticate_user(self, email: str, password: str) -> Optional[User]:
        """Authenticate a user by email and password."""
        result = await self.db.execute(
            select(User).where(User.email == email, User.is_active == True)
        )
        user = result.scalar_one_or_none()
        if not user:
            return None
        if not self.verify_password(password, user.hashed_password):
            return None
        return user
