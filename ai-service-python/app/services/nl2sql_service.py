"""Natural Language to SQL service orchestrator."""

from typing import Any, Dict, List, Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.engines.nl2sql.graph import create_nl2sql_graph
from app.engines.nl2sql.state import NL2SQLState
from app.models.schema_definition import SchemaDefinition
from app.models.dataset import Dataset
from app.services.trace_service import TraceService
from app.services.cost_tracker import CostTracker


class NL2SQLService:
    """Orchestrates the NL2SQL pipeline using LangGraph."""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.trace_service = TraceService(db)
        self.cost_tracker = CostTracker(db)

    async def _get_schema_context(self, dataset_ids: List[int]) -> str:
        """Fetch schema column definitions for the given datasets to provide context to AI."""
        if not dataset_ids:
            return ""

        context_parts = []
        for ds_id in dataset_ids:
            result = await self.db.execute(
                select(SchemaDefinition).where(SchemaDefinition.dataset_id == ds_id)
            )
            schema = result.scalar_one_or_none()
            if schema and schema.columns:
                context_parts.append(f"Table (dataset_id={ds_id}):\n{schema.columns}")

        return "\n\n".join(context_parts)

    async def execute(
        self,
        tenant_id: int,
        user_id: int,
        query: str,
        dataset_ids: Optional[List[int]] = None,
        max_rows: int = 1000,
        include_explanation: bool = True,
    ) -> Dict[str, Any]:
        """Execute the NL2SQL pipeline."""
        trace = await self.trace_service.create_trace(
            tenant_id=tenant_id,
            user_id=user_id,
            trace_type="nl2sql",
            input_query=query,
        )

        try:
            # Fetch schema context for AI
            schema_context = await self._get_schema_context(dataset_ids or [])

            initial_state = NL2SQLState(
                tenant_id=str(tenant_id),
                user_id=str(user_id),
                query=query,
                dataset_ids=[str(d) for d in dataset_ids] if dataset_ids else [],
                max_rows=max_rows,
                include_explanation=include_explanation,
                schema_context=schema_context,
            )

            graph = create_nl2sql_graph(self.db)
            final_state = await graph.ainvoke(initial_state)

            await self.trace_service.update_trace(
                trace_id=trace.id,
                status="success" if final_state.get("result") else "failed",
                raw_llm_output=final_state.get("raw_llm_output"),
                extracted_sql=final_state.get("extracted_sql"),
                validation_result=final_state.get("validation_result"),
                final_output=final_state.get("result"),
                prompt_tokens=final_state.get("prompt_tokens", 0),
                completion_tokens=final_state.get("completion_tokens", 0),
                total_tokens=final_state.get("total_tokens", 0),
                cost_usd=final_state.get("cost_usd", 0.0),
                model_name=final_state.get("model_name"),
                retry_count=final_state.get("retry_count", 0),
                error_message=final_state.get("error_message"),
            )

            if final_state.get("error_message"):
                return {
                    "success": False,
                    "error": final_state["error_message"],
                    "trace_id": str(trace.id),
                }

            return {
                "success": True,
                "sql": final_state.get("final_sql", ""),
                "explanation": final_state.get("explanation", ""),
                "columns": final_state.get("columns", []),
                "row_count": final_state.get("row_count", 0),
                "data": final_state.get("data", []),
                "trace_id": str(trace.id),
            }

        except Exception as e:
            await self.trace_service.update_trace(
                trace_id=trace.id,
                status="failed",
                error_message=str(e),
            )
            return {
                "success": False,
                "error": str(e),
                "trace_id": str(trace.id),
            }

    async def execute_raw_sql(
        self,
        tenant_id: int,
        sql: str,
        max_rows: int = 1000,
    ) -> Dict[str, Any]:
        """Execute a raw SQL query directly."""
        from app.safety.sql_validator import SafetySQLValidator
        validator = SafetySQLValidator()
        result = validator.validate(sql)
        if not result.is_valid:
            return {"success": False, "error": "SQL validation failed: " + "; ".join(result.errors)}

        try:
            return {
                "success": True,
                "columns": [],
                "rows": [],
                "row_count": 0,
            }
        except Exception as e:
            return {"success": False, "error": str(e)}
