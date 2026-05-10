"""Execution traces endpoints."""

from typing import Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_tenant
from app.schemas.common import ApiResponse, PageResult
from app.schemas.trace import TraceResponse
from app.services.trace_service import TraceService

router = APIRouter()


@router.get("/traces", response_model=ApiResponse[PageResult[TraceResponse]])
async def list_traces(
    trace_type: Optional[str] = Query(None, description="Filter by trace type"),
    status: Optional[str] = Query(None, description="Filter by status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """List execution traces with pagination."""
    service = TraceService(db)
    traces, total = await service.get_traces(
        tenant_id=tenant["tenant_id"],
        trace_type=trace_type,
        status=status,
        page=page,
        page_size=page_size,
    )

    return ApiResponse(
        data=PageResult(
            items=[TraceResponse.model_validate(t) for t in traces],
            total=total,
            page=page,
            page_size=page_size,
            total_pages=(total + page_size - 1) // page_size,
        )
    )


@router.get("/traces/{trace_id}", response_model=ApiResponse[TraceResponse])
async def get_trace(
    trace_id: int,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """Get a specific trace by ID."""
    service = TraceService(db)
    trace = await service.get_trace_by_id(
        tenant_id=tenant["tenant_id"],
        trace_id=trace_id,
    )

    if not trace:
        return ApiResponse(
            success=False,
            message="Trace not found",
            error_code="NOT_FOUND",
        )

    return ApiResponse(data=TraceResponse.model_validate(trace))
