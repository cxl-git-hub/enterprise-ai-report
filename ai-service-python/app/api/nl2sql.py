"""NL2SQL endpoint."""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_tenant
from app.schemas.ai import NL2SQLRequest
from app.schemas.common import ApiResponse
from app.services.nl2sql_service import NL2SQLService

router = APIRouter()


@router.post("/nl2sql", response_model=ApiResponse)
async def natural_language_to_sql(
    request: NL2SQLRequest,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """Convert natural language query to SQL and execute it."""
    service = NL2SQLService(db)
    result = await service.execute(
        tenant_id=tenant["tenant_id"],
        user_id=tenant["user_id"],
        query=request.query,
        dataset_ids=request.dataset_ids,
        max_rows=request.max_rows,
        include_explanation=request.include_explanation,
    )

    if not result.get("success"):
        return ApiResponse(
            success=False,
            message=result.get("error", "NL2SQL execution failed"),
            error_code="NL2SQL_ERROR",
            data=result,
        )

    return ApiResponse(data=result)
