"""Report generation endpoint."""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_tenant
from app.schemas.ai import ReportGenerateRequest
from app.schemas.common import ApiResponse
from app.services.report_service import ReportService

router = APIRouter()


@router.post("/report/generate", response_model=ApiResponse)
async def generate_report(
    request: ReportGenerateRequest,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """Generate a report using AI."""
    service = ReportService(db)
    result = await service.generate(
        tenant_id=tenant["tenant_id"],
        user_id=tenant["user_id"],
        title=request.title,
        dataset_ids=request.dataset_ids,
        description=request.description,
        kpi_ids=request.kpi_ids,
        template_id=request.template_id,
        analysis_queries=request.analysis_queries,
        output_format=request.output_format,
        date_range=request.date_range,
    )

    if not result.get("success"):
        return ApiResponse(
            success=False,
            message=result.get("error", "Report generation failed"),
            error_code="REPORT_ERROR",
            data=result,
        )

    return ApiResponse(data=result)
