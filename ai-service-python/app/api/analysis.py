"""Analysis endpoint."""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_tenant
from app.schemas.ai import AnalysisRequest
from app.schemas.common import ApiResponse
from app.services.analysis_service import AnalysisService

router = APIRouter()


@router.post("/analysis")
async def analyze_data(
    request: AnalysisRequest,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """Perform data analysis using AI."""
    service = AnalysisService(db)
    result = await service.execute(
        tenant_id=tenant["tenant_id"],
        user_id=tenant["user_id"],
        query=request.query,
        dataset_ids=request.dataset_ids,
        sql=request.sql,
        analysis_type=request.analysis_type,
        output_format=request.output_format,
    )

    if not result.get("success"):
        return ApiResponse(code=500, message=result.get("error", "Analysis failed"), data=result)

    return ApiResponse(data=result)
