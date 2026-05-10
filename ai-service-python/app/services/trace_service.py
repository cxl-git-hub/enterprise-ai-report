"""Execution tracing service."""

from typing import Any, Dict, List, Optional
from uuid import UUID

from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_trace import AITrace


class TraceService:
    """Manages AI execution traces for observability."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_trace(
        self,
        tenant_id: UUID,
        user_id: UUID,
        trace_type: str,
        input_query: str,
        workflow_run_id: Optional[UUID] = None,
    ) -> AITrace:
        """Create a new execution trace."""
        trace = AITrace(
            tenant_id=tenant_id,
            user_id=user_id,
            workflow_run_id=workflow_run_id,
            trace_type=trace_type,
            status="pending",
            input_query=input_query,
        )
        self.db.add(trace)
        await self.db.flush()
        return trace

    async def update_trace(
        self,
        trace_id: UUID,
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
        result = await self.db.execute(select(AITrace).where(AITrace.id == trace_id))
        trace = result.scalar_one_or_none()
        if not trace:
            return

        if status is not None:
            trace.status = status
        if raw_llm_output is not None:
            trace.raw_llm_output = raw_llm_output
        if extracted_sql is not None:
            trace.extracted_sql = extracted_sql
        if validation_result is not None:
            trace.validation_result = validation_result
        if final_output is not None:
            trace.final_output = final_output
        if prompt_text is not None:
            trace.prompt_text = prompt_text
        trace.prompt_tokens = prompt_tokens
        trace.completion_tokens = completion_tokens
        trace.total_tokens = total_tokens
        trace.cost_usd = cost_usd
        if model_name is not None:
            trace.model_name = model_name
        trace.retry_count = retry_count
        if duration_ms is not None:
            trace.duration_ms = duration_ms
        if error_message is not None:
            trace.error_message = error_message

        await self.db.flush()

    async def get_traces(
        self,
        tenant_id: UUID,
        trace_type: Optional[str] = None,
        status: Optional[str] = None,
        page: int = 1,
        page_size: int = 20,
    ) -> tuple[List[AITrace], int]:
        """Get traces for a tenant with pagination."""
        query = select(AITrace).where(AITrace.tenant_id == tenant_id)
        count_query = select(func.count()).select_from(AITrace).where(AITrace.tenant_id == tenant_id)

        if trace_type:
            query = query.where(AITrace.trace_type == trace_type)
            count_query = count_query.where(AITrace.trace_type == trace_type)
        if status:
            query = query.where(AITrace.status == status)
            count_query = count_query.where(AITrace.status == status)

        total_result = await self.db.execute(count_query)
        total = total_result.scalar()

        query = query.order_by(AITrace.created_at.desc())
        query = query.offset((page - 1) * page_size).limit(page_size)
        result = await self.db.execute(query)
        traces = list(result.scalars().all())

        return traces, total

    async def get_trace_by_id(self, tenant_id: UUID, trace_id: UUID) -> Optional[AITrace]:
        """Get a single trace by ID."""
        result = await self.db.execute(
            select(AITrace).where(AITrace.id == trace_id, AITrace.tenant_id == tenant_id)
        )
        return result.scalar_one_or_none()
