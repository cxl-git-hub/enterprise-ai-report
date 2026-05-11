"""NL2SQL endpoint."""

from typing import Optional
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_tenant
from app.schemas.ai import NL2SQLRequest
from app.schemas.common import ApiResponse
from app.services.nl2sql_service import NL2SQLService
from app.services.confidence_scorer import ConfidenceScorer
from app.safety.sql_validator import SafetySQLValidator

router = APIRouter()


class NL2SQLValidateRequest(BaseModel):
    sql: str
    schemaId: Optional[str] = None


class NL2SQLExecuteRequest(BaseModel):
    sql: str
    schemaId: Optional[str] = None
    maxRows: int = 1000


@router.post("/nl2sql")
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
        return ApiResponse(code=500, message=result.get("error", "NL2SQL execution failed"), data=result)

    # Add confidence scoring
    confidence = ConfidenceScorer.score_nl2sql(
        natural_query=request.query,
        generated_sql=result.get("sql", ""),
    )
    result["confidence"] = confidence

    # Add data source citations
    citations = ConfidenceScorer.extract_data_sources(
        generated_sql=result.get("sql"),
        dataset_ids=request.dataset_ids,
    )
    # Enrich citations with metadata from database
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
        "text": "以下 SQL 由 AI 根据自然语言自动生成，执行前请确认 SQL 逻辑正确性。AI 生成的查询可能无法完全理解业务上下文。",
        "level": "warning",
        "type": "nl2sql",
    }

    return ApiResponse(data=result)


@router.post("/nl2sql/validate")
async def validate_sql(
    request: NL2SQLValidateRequest,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """Validate a SQL statement."""
    validator = SafetySQLValidator()
    result = validator.validate(request.sql)
    return ApiResponse(data={
        "valid": result.is_valid,
        "errors": result.errors,
    })


@router.post("/nl2sql/execute")
async def execute_sql(
    request: NL2SQLExecuteRequest,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """Execute a validated SQL query."""
    service = NL2SQLService(db)
    result = await service.execute_raw_sql(
        tenant_id=tenant["tenant_id"],
        sql=request.sql,
        max_rows=request.maxRows,
    )

    if not result.get("success"):
        return ApiResponse(code=500, message=result.get("error", "SQL execution failed"), data=result)

    return ApiResponse(data=result)
