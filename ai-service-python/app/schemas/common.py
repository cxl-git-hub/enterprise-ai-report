"""Common response schemas."""

from typing import Any, Generic, List, Optional, TypeVar
from pydantic import BaseModel

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """Standard API response wrapper - matches Java backend format."""

    code: int = 200
    message: str = "success"
    data: Optional[T] = None

    def __init__(self, data=None, code=200, message="success", **kwargs):
        super().__init__(code=code, message=message, data=data, **kwargs)


class PageResult(BaseModel, Generic[T]):
    """Paginated result wrapper."""

    items: List[T] = []
    total: int = 0
    page: int = 1
    page_size: int = 20
    total_pages: int = 0
