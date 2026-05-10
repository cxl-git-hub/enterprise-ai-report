"""Data analysis service orchestrator."""

from typing import Any, Dict, List, Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.engines.analysis.graph import create_analysis_graph
from app.engines.analysis.state import AnalysisState
from app.services.trace_service import TraceService


class AnalysisService:
    """Orchestrates the data analysis pipeline using LangGraph."""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.trace_service = TraceService(db)

    async def execute(
        self,
        tenant_id: int,
        user_id: int,
        query: str,
        dataset_ids: List[int],
        sql: Optional[str] = None,
        analysis_type: str = "general",
        output_format: str = "json",
    ) -> Dict[str, Any]:
        """Execute the analysis pipeline."""
        trace = await self.trace_service.create_trace(
            tenant_id=tenant_id,
            user_id=user_id,
            trace_type="analysis",
            input_query=query,
        )

        try:
            initial_state = AnalysisState(
                tenant_id=str(tenant_id),
                user_id=str(user_id),
                query=query,
                dataset_ids=[str(d) for d in dataset_ids],
                sql=sql,
                analysis_type=analysis_type,
                output_format=output_format,
            )

            graph = create_analysis_graph(self.db)
            final_state = await graph.ainvoke(initial_state)

            await self.trace_service.update_trace(
                trace_id=trace.id,
                status="success" if final_state.get("result") else "failed",
                raw_llm_output=final_state.get("raw_llm_output"),
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
                return {"success": False, "error": final_state["error_message"], "trace_id": str(trace.id)}

            return {
                "success": True,
                "analysis": final_state.get("result", {}),
                "insights": final_state.get("insights", []),
                "recommendations": final_state.get("recommendations", []),
                "chart_config": final_state.get("chart_config"),
                "trace_id": str(trace.id),
            }

        except Exception as e:
            await self.trace_service.update_trace(
                trace_id=trace.id,
                status="failed",
                error_message=str(e),
            )
            return {"success": False, "error": str(e), "trace_id": str(trace.id)}
