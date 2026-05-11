"""Analysis endpoint."""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_tenant
from app.schemas.ai import AnalysisRequest
from app.schemas.common import ApiResponse
from app.services.analysis_service import AnalysisService
from app.services.confidence_scorer import ConfidenceScorer

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
        context=request.context,
    )

    if not result.get("success"):
        return ApiResponse(code=500, message=result.get("error", "Analysis failed"), data=result)

    # Add confidence scoring
    confidence = ConfidenceScorer.score_analysis(
        query=request.query,
        analysis_type=request.analysis_type,
        result_data=result.get("analysis", {}),
    )
    result["confidence"] = confidence

    # Add data source citations
    citations = ConfidenceScorer.extract_data_sources(
        dataset_ids=request.dataset_ids,
    )
    # Enrich citations with metadata
    if citations and request.dataset_ids:
        from sqlalchemy import select
        from app.models.dataset import Dataset
        for cite in citations:
            ds_id = cite.get("datasetId")
            if ds_id:
                ds_result = await db.execute(select(Dataset).where(Dataset.id == ds_id))
                ds = ds_result.scalar_one_or_none()
                if ds:
                    cite["sourceName"] = ds.name
                    cite["lastUpdated"] = str(ds.updated_at) if hasattr(ds, 'updated_at') and ds.updated_at else None

    result["citations"] = citations

    # Add disclaimer
    result["disclaimer"] = {
        "text": "以下分析结论由 AI 基于数据自动生成，仅供参考。AI 可能无法完全理解业务上下文，请结合实际业务判断。重要决策请核实原始数据。",
        "level": "warning",
        "type": "analysis",
    }

    return ApiResponse(data=result)
