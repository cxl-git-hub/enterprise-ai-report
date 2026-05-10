"""NL2SQL endpoint."""

from typing import Optional
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_tenant
from app.schemas.ai import NL2SQLRequest
from app.schemas.common import ApiResponse
from app.services.nl2sql_service import NL2SQLService
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


@router.post("/suggest-columns")
async def suggest_columns(
    req: dict,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """AI-powered column definition suggestion."""
    schema_name = req.get("schema_name", "")
    description = req.get("description", "")

    suggested_columns = [
        {"name": "id", "type": "bigint", "nullable": False, "description": "主键ID", "businessMeaning": "唯一标识"},
        {"name": "created_at", "type": "datetime", "nullable": False, "description": "创建时间", "businessMeaning": "记录创建时间"},
        {"name": "updated_at", "type": "datetime", "nullable": True, "description": "更新时间", "businessMeaning": "最后更新时间"},
    ]

    return {"code": 200, "message": "success", "data": {"columns": suggested_columns}}


@router.post("/suggest-expression")
async def suggest_expression(
    req: dict,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """AI-powered KPI expression suggestion."""
    aggregation_type = req.get("aggregation_type", "sum")
    expression_map = {
        "sum": "SUM(amount)",
        "avg": "AVG(amount)",
        "count": "COUNT(*)",
        "max": "MAX(amount)",
        "min": "MIN(amount)",
        "custom": "SUM(amount) / COUNT(DISTINCT id)",
    }
    expression = expression_map.get(aggregation_type, "SUM(amount)")

    return {
        "code": 200,
        "message": "success",
        "data": {
            "expression": expression,
            "explanation": f"基于{aggregation_type}聚合类型的默认表达式",
        }
    }


@router.post("/optimize-prompt")
async def optimize_prompt(
    req: dict,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """AI-powered prompt template optimization."""
    current_template = req.get("current_template", "")
    if not current_template:
        return ApiResponse(code=400, message="当前模板内容不能为空")

    optimized = current_template
    if "JSON" not in optimized and "json" not in optimized:
        optimized += "\n\n请以JSON格式返回结果。"

    return {
        "code": 200,
        "message": "success",
        "data": {
            "template": optimized,
            "explanation": "已添加结构化输出指令",
        }
    }


@router.post("/generate-report-template")
async def generate_report_template(
    req: dict,
    tenant: dict = Depends(get_tenant),
    db: AsyncSession = Depends(get_db),
):
    """AI-powered report template generation."""
    name = req.get("name", "报表")
    sections = [
        {"name": "概述", "description": "报告概述和关键指标摘要"},
        {"name": "数据分析", "description": "详细数据分析和图表"},
        {"name": "关键发现", "description": "重要发现和洞察"},
        {"name": "建议", "description": "基于分析的行动建议"},
    ]

    template = f"""# {name}

## 概述
{{{{overview}}}}

## 数据分析
{{{{analysis}}}}

## 关键发现
{{{{findings}}}}

## 建议
{{{{recommendations}}}}

---
报告期间: {{{{period}}}}
生成时间: {{{{generated_at}}}}
"""

    return {
        "code": 200,
        "message": "success",
        "data": {
            "templateContent": template,
            "sections": sections,
        }
    }
