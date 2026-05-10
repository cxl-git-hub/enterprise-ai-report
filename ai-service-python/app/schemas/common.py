"""Common response schemas."""

from typing import Any, Generic, List, Optional, TypeVar
from pydantic import BaseModel

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """Standard API response wrapper."""

    success: bool = True
    message: str = "OK"
    data: Optional[T] = None
    error_code: Optional[str] = None


class PageResult(BaseModel, Generic[T]):
    """Paginated result wrapper."""

    items: List[T] = []
    total: int = 0
    page: int = 1
    page_size: int = 20
    total_pages: int = 0
