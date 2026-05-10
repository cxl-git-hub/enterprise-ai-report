"""Report generation LangGraph workflow definition."""

from typing import Any, Dict

from langgraph.graph import END, StateGraph

from app.engines.report.state import ReportState
from app.engines.report import nodes
from sqlalchemy.ext.asyncio import AsyncSession


def create_report_graph(db: AsyncSession):
    """Create the Report Generation LangGraph workflow."""

    async def _aggregate_kpi_data(state: ReportState) -> Dict[str, Any]:
        return await nodes.aggregate_kpi_data(state, db)

    async def _compile_analysis_results(state: ReportState) -> Dict[str, Any]:
        return await nodes.compile_analysis_results(state, db)

    async def _select_template(state: ReportState) -> Dict[str, Any]:
        return await nodes.select_template(state, db)

    async def _generate_narrative(state: ReportState) -> Dict[str, Any]:
        return await nodes.generate_narrative(state, db)

    async def _assemble_document(state: ReportState) -> Dict[str, Any]:
        return await nodes.assemble_document(state, db)

    async def _retry_report(state: ReportState) -> Dict[str, Any]:
        return await nodes.retry_report(state, db)

    def _check_error(state: ReportState) -> str:
        if state.get("error_message"):
            return "end"
        return "continue"

    workflow = StateGraph(ReportState)

    # Add nodes
    workflow.add_node("aggregate_kpi_data", _aggregate_kpi_data)
    workflow.add_node("compile_analysis_results", _compile_analysis_results)
    workflow.add_node("select_template", _select_template)
    workflow.add_node("generate_narrative", _generate_narrative)
    workflow.add_node("validate_report", nodes.validate_report)
    workflow.add_node("retry_report", _retry_report)
    workflow.add_node("assemble_document", _assemble_document)

    # Define edges
    workflow.set_entry_point("aggregate_kpi_data")
    workflow.add_edge("aggregate_kpi_data", "compile_analysis_results")
    workflow.add_edge("compile_analysis_results", "select_template")
    workflow.add_edge("select_template", "generate_narrative")
    workflow.add_edge("generate_narrative", "validate_report")

    workflow.add_conditional_edges(
        "validate_report",
        nodes.check_report_validation,
        {"retry": "retry_report", "assemble": "assemble_document", "end": END},
    )

    workflow.add_edge("retry_report", "generate_narrative")
    workflow.add_edge("assemble_document", END)

    return workflow.compile()
