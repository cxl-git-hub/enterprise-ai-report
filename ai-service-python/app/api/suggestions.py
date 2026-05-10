"""AI Suggestion endpoints for schema, KPI, prompt, and report template generation."""

import json
import re
import logging
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

from app.core.dependencies import get_db, get_current_user
from app.services.ai_policy_service import AIPolicyService
from app.services.llm_client import LLMClient

logger = logging.getLogger(__name__)
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


def _extract_json(text: str) -> Any:
    """Extract JSON from LLM response text."""
    # Try to find JSON array or object in the response
    json_match = re.search(r'[\[\{][\s\S]*[\]\}]', text)
    if json_match:
        try:
            return json.loads(json_match.group())
        except json.JSONDecodeError:
            pass
    # Try the whole text
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None


@router.post("/suggest-columns")
async def suggest_columns(
    req: SuggestColumnsRequest,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    """AI-powered column definition suggestion based on schema name and description."""
    policy_service = AIPolicyService(db)
    if not await policy_service.is_allowed(user["tenant_id"], "allow_schema_access"):
        raise HTTPException(403, "AI schema access not allowed by policy")

    llm = LLMClient()

    prompt = f"""你是一个数据分析专家。请根据以下Schema信息，建议合理的列定义。

Schema名称: {req.schema_name}
描述: {req.description or '无'}

请返回JSON格式的列定义数组，每个列包含:
- name: 列名(英文小写+下划线)
- type: 数据类型 (string/number/date/boolean/json)
- nullable: 是否可空 (boolean)
- description: 列描述(中文)
- businessMeaning: 业务含义(中文)

请直接返回JSON数组，不要其他文字。至少建议5-8个常用的列。"""

    try:
        result = await llm.chat_simple("你是数据分析专家，只返回JSON格式结果。", prompt, temperature=0.3)
        parsed = _extract_json(result)
        if isinstance(parsed, list) and len(parsed) > 0:
            columns = parsed
        else:
            columns = [
                {"name": "id", "type": "number", "nullable": False, "description": "主键ID", "businessMeaning": "唯一标识"},
                {"name": "name", "type": "string", "nullable": False, "description": "名称", "businessMeaning": "显示名称"},
                {"name": "status", "type": "string", "nullable": True, "description": "状态", "businessMeaning": "记录状态"},
                {"name": "created_at", "type": "date", "nullable": False, "description": "创建时间", "businessMeaning": "记录创建时间"},
                {"name": "updated_at", "type": "date", "nullable": True, "description": "更新时间", "businessMeaning": "最后更新时间"},
            ]
    except Exception as e:
        logger.warning(f"AI suggest-columns failed: {e}")
        columns = [
            {"name": "id", "type": "number", "nullable": False, "description": "主键ID", "businessMeaning": "唯一标识"},
            {"name": "name", "type": "string", "nullable": False, "description": "名称", "businessMeaning": "显示名称"},
            {"name": "created_at", "type": "date", "nullable": False, "description": "创建时间", "businessMeaning": "记录创建时间"},
        ]

    return {"code": 200, "message": "success", "data": {"columns": columns}}


@router.post("/suggest-expression")
async def suggest_expression(
    req: SuggestExpressionRequest,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    """AI-powered KPI expression suggestion."""
    policy_service = AIPolicyService(db)
    if not await policy_service.is_allowed(user["tenant_id"], "allow_sql_generation"):
        raise HTTPException(403, "AI expression generation not allowed by policy")

    llm = LLMClient()

    prompt = f"""你是一个KPI计算专家。请根据以下KPI信息，建议合理的SQL聚合表达式。

KPI名称: {req.kpi_name}
描述: {req.description or '无'}
聚合类型: {req.aggregation_type or '自动判断'}

要求:
1. 返回一个合法的SQL聚合表达式
2. 表达式应该可以直接用在SELECT语句中
3. 如果需要过滤条件，也请一并提供

请返回JSON格式:
{{
  "expression": "SQL表达式",
  "explanation": "表达式说明",
  "filter": "WHERE条件(可选)"
}}

只返回JSON，不要其他文字。"""

    try:
        result = await llm.chat_simple("你是KPI计算专家，只返回JSON格式结果。", prompt, temperature=0.2)
        parsed = _extract_json(result)
        if isinstance(parsed, dict) and "expression" in parsed:
            return {
                "code": 200,
                "message": "success",
                "data": {
                    "expression": parsed["expression"],
                    "explanation": parsed.get("explanation", ""),
                    "filter": parsed.get("filter"),
                }
            }
    except Exception as e:
        logger.warning(f"AI suggest-expression failed: {e}")

    # Fallback
    agg = (req.aggregation_type or "sum").lower()
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
        "code": 200,
        "message": "success",
        "data": {
            "expression": expression,
            "explanation": f"基于{agg}聚合类型的默认表达式，可根据实际数据结构调整",
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

    llm = LLMClient()

    prompt = f"""你是一个AI Prompt工程专家。请优化以下提示词模板。

模板名称: {req.name}
用途: {req.description or '无'}
分类: {req.category or '通用'}
当前模板:
---
{req.current_template}
---

请优化这个模板，使其:
1. 指令更清晰明确
2. 输出格式更规范(要求JSON格式输出)
3. 添加必要的约束条件
4. 保持原有功能不变

请返回JSON格式:
{{
  "template": "优化后的模板内容",
  "explanation": "优化说明",
  "improvements": ["改进点1", "改进点2"]
}}

只返回JSON，不要其他文字。"""

    try:
        result = await llm.chat_simple("你是Prompt工程专家，只返回JSON格式结果。", prompt, temperature=0.3)
        parsed = _extract_json(result)
        if isinstance(parsed, dict) and "template" in parsed:
            return {
                "code": 200,
                "message": "success",
                "data": {
                    "template": parsed["template"],
                    "explanation": parsed.get("explanation", "已优化提示词模板"),
                    "improvements": parsed.get("improvements", []),
                }
            }
    except Exception as e:
        logger.warning(f"AI optimize-prompt failed: {e}")

    # Fallback: add structured output instruction
    optimized = req.current_template
    if "JSON" not in optimized and "json" not in optimized:
        optimized += "\n\n请以JSON格式返回结果。"

    return {
        "code": 200,
        "message": "success",
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
    llm = LLMClient()

    prompt = f"""你是一个报表设计专家。请根据以下需求生成报表模板。

报表名称: {req.name}
描述: {req.description or '无'}
输出格式: {req.format}

请生成一个完整的报表模板，包含:
1. 报表标题
2. 多个章节(概述、数据分析、关键发现、建议等)
3. 每个章节有标题和内容占位符
4. 页脚信息

请返回JSON格式:
{{
  "templateContent": "模板内容(HTML或Markdown格式)",
  "sections": [
    {{"name": "章节名", "description": "章节说明"}}
  ],
  "variables": ["变量1", "变量2"]
}}

只返回JSON，不要其他文字。"""

    try:
        result = await llm.chat_simple("你是报表设计专家，只返回JSON格式结果。", prompt, temperature=0.4)
        parsed = _extract_json(result)
        if isinstance(parsed, dict) and "templateContent" in parsed:
            return {
                "code": 200,
                "message": "success",
                "data": {
                    "templateContent": parsed["templateContent"],
                    "sections": parsed.get("sections", []),
                    "variables": parsed.get("variables", []),
                }
            }
    except Exception as e:
        logger.warning(f"AI generate-report-template failed: {e}")

    # Fallback
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
        "code": 200,
        "message": "success",
        "data": {
            "templateContent": template,
            "sections": sections,
        }
    }
