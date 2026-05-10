"""Token and cost tracking service."""

from typing import Optional

from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.ai_trace import AiExecutionTrace


class CostTracker:
    """Tracks LLM token usage and calculates costs."""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.prompt_price = settings.COST_PER_1K_PROMPT_TOKENS
        self.completion_price = settings.COST_PER_1K_COMPLETION_TOKENS

    def calculate_cost(
        self,
        prompt_tokens: int,
        completion_tokens: int,
    ) -> float:
        """Calculate cost in USD for token usage."""
        prompt_cost = (prompt_tokens * self.prompt_price) / 1000
        completion_cost = (completion_tokens * self.completion_price) / 1000
        return round(prompt_cost + completion_cost, 6)

    def calculate_cost_for_model(
        self,
        prompt_tokens: int,
        completion_tokens: int,
        model_name: str,
    ) -> float:
        """Calculate cost with model-specific pricing."""
        model_pricing = {
            "gpt-4o": (0.005, 0.015),
            "gpt-4o-mini": (0.00015, 0.0006),
            "gpt-4-turbo": (0.01, 0.03),
            "gpt-3.5-turbo": (0.0005, 0.0015),
            "claude-3-opus": (0.015, 0.075),
            "claude-3-sonnet": (0.003, 0.015),
            "claude-3-haiku": (0.00025, 0.00125),
            "deepseek-chat": (0.00014, 0.00028),
            "glm-4": (0.001, 0.001),
            "qwen-max": (0.004, 0.012),
        }

        prompt_price, completion_price = model_pricing.get(
            model_name, (self.prompt_price, self.completion_price)
        )

        prompt_cost = (prompt_tokens * prompt_price) / 1000
        completion_cost = (completion_tokens * completion_price) / 1000
        return round(prompt_cost + completion_cost, 6)

    async def record_usage(
        self,
        trace_id: int,
        prompt_tokens: int,
        completion_tokens: int,
        model_name: Optional[str] = None,
    ) -> float:
        """Record token usage and return calculated cost."""
        cost = self.calculate_cost_for_model(
            prompt_tokens, completion_tokens, model_name or settings.LLM_MODEL
        )
        return cost

    async def get_usage_summary(
        self,
        tenant_id: int,
        trace_type: Optional[str] = None,
    ) -> dict:
        """Get aggregated usage summary for a tenant."""
        query = select(
            func.sum(AiExecutionTrace.total_tokens).label("total_tokens"),
            func.sum(AiExecutionTrace.prompt_tokens).label("prompt_tokens"),
            func.sum(AiExecutionTrace.completion_tokens).label("completion_tokens"),
            func.sum(AiExecutionTrace.cost).label("total_cost"),
            func.count(AiExecutionTrace.id).label("total_calls"),
        ).where(AiExecutionTrace.tenant_id == tenant_id)

        if trace_type:
            query = query.where(AiExecutionTrace.ai_task_type == trace_type)

        result = await self.db.execute(query)
        row = result.one()

        return {
            "total_tokens": row.total_tokens or 0,
            "prompt_tokens": row.prompt_tokens or 0,
            "completion_tokens": row.completion_tokens or 0,
            "total_cost_usd": float(row.total_cost or 0),
            "total_calls": row.total_calls or 0,
        }
