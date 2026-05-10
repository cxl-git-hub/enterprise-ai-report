"""AI Suggestion endpoints for schema, KPI, prompt, and report template generation."""

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

from app.core.dependencies import get_db, get_current_user
from app.services.ai_policy_service import AIPolicyService
from app.services.prompt_builder import PromptBuilder

router = APIRouter()


class SuggestColumnsRequest(BaseModel):
    schema_name: str
    description: Optional[str] = None
    dataset_id: Optional[str] = None


class SuggestExpressionRequest(BaseModel):
    kpi_name: str
    description: Optional[str] = None
    schema_id: Optional[str] = None
    aggregation_type: Optional[str] = None


class OptimizePromptRequest(BaseModel):
    name: str
    description: Optional[str] = None
    category: Optional[str] = None
    current_template: Optional[str] = None


class GenerateReportTemplateRequest(BaseModel):
    name: str
    description: Optional[str] = None
    format: str = "pdf"


@router.post("/suggest-columns")
async def suggest_columns(
    req: SuggestColumnsRequest,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    """AI-powered column definition suggestion based on schema name and description."""
    policy_service = AIPolicyService(db)
    if not await policy_service.is_allowed(user.tenant_id, "allow_schema_access"):
        raise HTTPException(403, "AI schema access not allowed by policy")

    prompt_builder = PromptBuilder(db)
    prompt = f"""你是一个数据分析专家。请根据以下Schema信息，建议合理的列定义。

Schema名称: {req.schema_name}
描述: {req.description or '无'}

请返回JSON格式的列定义数组，每个列包含:
- name: 列名(英文)
- type: 数据类型
- nullable: 是否可空
- description: 列描述
- businessMeaning: 业务含义

请直接返回JSON数组，不要其他文字。"""

    # In production, call LLM here. For now, return a structured suggestion.
    suggested_columns = [
        {"name": "id", "type": "bigint", "nullable": False, "description": "主键ID", "businessMeaning": "唯一标识"},
        {"name": "created_at", "type": "datetime", "nullable": False, "description": "创建时间", "businessMeaning": "记录创建时间"},
        {"name": "updated_at", "type": "datetime", "nullable": True, "description": "更新时间", "businessMeaning": "最后更新时间"},
    ]

    return {"data": {"columns": suggested_columns, "prompt": prompt}}


@router.post("/suggest-expression")
async def suggest_expression(
    req: SuggestExpressionRequest,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    """AI-powered KPI expression suggestion."""
    policy_service = AIPolicyService(db)
    if not await policy_service.is_allowed(user.tenant_id, "allow_sql_generation"):
        raise HTTPException(403, "AI expression generation not allowed by policy")

    prompt = f"""你是一个KPI计算专家。请根据以下KPI信息，建议合理的计算表达式。

KPI名称: {req.kpi_name}
描述: {req.description or '无'}
聚合类型: {req.aggregation_type or '自动判断'}

请返回一个SQL聚合表达式，例如: SUM(amount) / COUNT(DISTINCT order_id)
只返回表达式本身，不要其他文字。"""

    # Default suggestion based on aggregation type
    agg = req.aggregation_type or "sum"
    expression_map = {
        "sum": "SUM(amount)",
        "avg": "AVG(amount)",
        "count": "COUNT(*)",
        "max": "MAX(amount)",
        "min": "MIN(amount)",
        "custom": "SUM(amount) / COUNT(DISTINCT id)",
    }
    expression = expression_map.get(agg, "SUM(amount)")

    return {
        "data": {
            "expression": expression,
            "explanation": f"基于{agg}聚合类型的默认表达式，可根据实际数据结构调整",
            "prompt": prompt,
        }
    }


@router.post("/optimize-prompt")
async def optimize_prompt(
    req: OptimizePromptRequest,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    """AI-powered prompt template optimization."""
    if not req.current_template:
        raise HTTPException(400, "当前模板内容不能为空")

    # Add best practices to the template
    optimized = req.current_template

    # Add structured output instruction if not present
    if "JSON" not in optimized and "json" not in optimized:
        optimized += "\n\n请以JSON格式返回结果。"

    return {
        "data": {
            "template": optimized,
            "explanation": "已添加结构化输出指令，确保AI返回可解析的JSON格式",
        }
    }


@router.post("/generate-report-template")
async def generate_report_template(
    req: GenerateReportTemplateRequest,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    """AI-powered report template generation."""
    sections = [
        {"name": "概述", "description": "报告概述和关键指标摘要"},
        {"name": "数据分析", "description": "详细数据分析和图表"},
        {"name": "关键发现", "description": "重要发现和洞察"},
        {"name": "建议", "description": "基于分析的行动建议"},
    ]

    template = f"""# {req.name}

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
        "data": {
            "templateContent": template,
            "sections": sections,
        }
    }
