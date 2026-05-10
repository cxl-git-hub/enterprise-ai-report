"""Report generation service orchestrator."""

from typing import Any, Dict, List, Optional
from uuid import UUID, uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.engines.report.graph import create_report_graph
from app.engines.report.state import ReportState
from app.services.trace_service import TraceService


class ReportService:
    """Orchestrates the report generation pipeline using LangGraph."""

    def __init__(self, db: AsyncSession):
        self.db = db
        self.trace_service = TraceService(db)

    async def generate(
        self,
        tenant_id: UUID,
        user_id: UUID,
        title: str,
        dataset_ids: List[UUID],
        description: Optional[str] = None,
        kpi_ids: Optional[List[UUID]] = None,
        template_id: Optional[UUID] = None,
        analysis_queries: Optional[List[str]] = None,
        output_format: str = "markdown",
        date_range: Optional[Dict[str, str]] = None,
    ) -> Dict[str, Any]:
        """Generate a report using the LangGraph pipeline."""
        trace = await self.trace_service.create_trace(
            tenant_id=tenant_id,
            user_id=user_id,
            trace_type="report",
            input_query=title,
        )

        try:
            initial_state = ReportState(
                tenant_id=str(tenant_id),
                user_id=str(user_id),
                title=title,
                description=description or "",
                dataset_ids=[str(d) for d in dataset_ids],
                kpi_ids=[str(k) for k in kpi_ids] if kpi_ids else [],
                template_id=str(template_id) if template_id else None,
                analysis_queries=analysis_queries or [],
                output_format=output_format,
                date_range=date_range,
            )

            graph = create_report_graph(self.db)
            final_state = await graph.ainvoke(initial_state)

            report_id = uuid4()

            await self.trace_service.update_trace(
                trace_id=trace.id,
                status="success" if final_state.get("report_content") else "failed",
                final_output={
                    "report_id": str(report_id),
                    "title": title,
                    "content": final_state.get("report_content", ""),
                },
                prompt_tokens=final_state.get("prompt_tokens", 0),
                completion_tokens=final_state.get("completion_tokens", 0),
                total_tokens=final_state.get("total_tokens", 0),
                cost_usd=final_state.get("cost_usd", 0.0),
                model_name=final_state.get("model_name"),
                error_message=final_state.get("error_message"),
            )

            if final_state.get("error_message"):
                return {"success": False, "error": final_state["error_message"], "trace_id": str(trace.id)}

            return {
                "success": True,
                "report_id": str(report_id),
                "title": title,
                "content": final_state.get("report_content", ""),
                "format": output_format,
                "sections": final_state.get("sections", []),
                "file_url": final_state.get("file_url"),
                "trace_id": str(trace.id),
            }

        except Exception as e:
            await self.trace_service.update_trace(
                trace_id=trace.id,
                status="failed",
                error_message=str(e),
            )
            return {"success": False, "error": str(e), "trace_id": str(trace.id)}
