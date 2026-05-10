"""Health check endpoint."""

from fastapi import APIRouter

from app.schemas.common import ApiResponse

router = APIRouter()


@router.get("/health", response_model=ApiResponse)
async def health_check():
    """Health check endpoint."""
    return ApiResponse(
        data={
            "status": "healthy",
            "service": "ai-service",
            "version": "1.0.0",
        }
    )
