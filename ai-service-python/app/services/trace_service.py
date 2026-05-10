"""Execution tracing service."""

import uuid
from typing import Any, Dict, List, Optional

from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_trace import AiExecutionTrace


class TraceService:
    """Manages AI execution traces for observability."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_trace(
        self,
        tenant_id: int,
        user_id: int,
        trace_type: str,
        input_query: str,
        workflow_run_id: Optional[str] = None,
    ) -> AiExecutionTrace:
        """Create a new execution trace."""
        trace = AiExecutionTrace(
            tenant_id=tenant_id,
            trace_id=str(uuid.uuid4()),
            run_id=workflow_run_id,
            ai_task_type=trace_type,
            input_prompt=input_query,
            status="pending",
        )
        self.db.add(trace)
        await self.db.flush()
        return trace

    async def update_trace(
        self,
        trace_id: Any,
        status: Optional[str] = None,
        raw_llm_output: Optional[str] = None,
        extracted_sql: Optional[str] = None,
        validation_result: Optional[dict] = None,
        final_output: Optional[dict] = None,
        prompt_tokens: int = 0,
        completion_tokens: int = 0,
        total_tokens: int = 0,
        cost_usd: float = 0.0,
        model_name: Optional[str] = None,
        retry_count: int = 0,
        duration_ms: Optional[int] = None,
        error_message: Optional[str] = None,
        prompt_text: Optional[str] = None,
    ) -> None:
        """Update an existing trace with results."""
        result = await self.db.execute(
            select(AiExecutionTrace).where(AiExecutionTrace.id == int(str(trace_id)))
        )
        trace = result.scalar_one_or_none()
        if not trace:
            return

        if status is not None:
            trace.status = status
        if raw_llm_output is not None:
            trace.raw_output = raw_llm_output
        if model_name is not None:
            trace.model_name = model_name
        trace.prompt_tokens = prompt_tokens
        trace.completion_tokens = completion_tokens
        trace.total_tokens = total_tokens
        trace.cost = cost_usd
        trace.retry_count = retry_count
        if duration_ms is not None:
            trace.latency_ms = duration_ms

        await self.db.flush()

    async def get_traces(
        self,
        tenant_id: int,
        trace_type: Optional[str] = None,
        status: Optional[str] = None,
        page: int = 1,
        page_size: int = 20,
    ) -> tuple[List[AiExecutionTrace], int]:
        """Get traces for a tenant with pagination."""
        query = select(AiExecutionTrace).where(AiExecutionTrace.tenant_id == tenant_id)
        count_query = select(func.count()).select_from(AiExecutionTrace).where(
            AiExecutionTrace.tenant_id == tenant_id
        )

        if trace_type:
            query = query.where(AiExecutionTrace.ai_task_type == trace_type)
            count_query = count_query.where(AiExecutionTrace.ai_task_type == trace_type)
        if status:
            query = query.where(AiExecutionTrace.status == status)
            count_query = count_query.where(AiExecutionTrace.status == status)

        total_result = await self.db.execute(count_query)
        total = total_result.scalar()

        query = query.order_by(AiExecutionTrace.created_at.desc())
        query = query.offset((page - 1) * page_size).limit(page_size)
        result = await self.db.execute(query)
        traces = list(result.scalars().all())

        return traces, total

    async def get_trace_by_id(self, tenant_id: int, trace_id: int) -> Optional[AiExecutionTrace]:
        """Get a single trace by ID."""
        result = await self.db.execute(
            select(AiExecutionTrace).where(
                AiExecutionTrace.id == trace_id,
                AiExecutionTrace.tenant_id == tenant_id,
            )
        )
        return result.scalar_one_or_none()
