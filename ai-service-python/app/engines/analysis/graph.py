"""Analysis LangGraph workflow definition."""

from typing import Any, Dict

from langgraph.graph import END, StateGraph

from app.engines.analysis.state import AnalysisState
from app.engines.analysis import nodes
from sqlalchemy.ext.asyncio import AsyncSession


def create_analysis_graph(db: AsyncSession):
    """Create the Analysis LangGraph workflow."""

    async def _retrieve_data(state: AnalysisState) -> Dict[str, Any]:
        return await nodes.retrieve_data(state, db)

    async def _inject_schema_context(state: AnalysisState) -> Dict[str, Any]:
        return await nodes.inject_schema_context(state, db)

    async def _build_analysis_prompt(state: AnalysisState) -> Dict[str, Any]:
        return await nodes.build_analysis_prompt(state, db)

    async def _llm_analyze(state: AnalysisState) -> Dict[str, Any]:
        return await nodes.llm_analyze(state, db)

    async def _retry_analysis(state: AnalysisState) -> Dict[str, Any]:
        return await nodes.retry_analysis(state, db)

    def _check_error(state: AnalysisState) -> str:
        if state.get("error_message"):
            return "end"
        return "continue"

    workflow = StateGraph(AnalysisState)

    # Add nodes
    workflow.add_node("retrieve_data", _retrieve_data)
    workflow.add_node("inject_schema_context", _inject_schema_context)
    workflow.add_node("build_analysis_prompt", _build_analysis_prompt)
    workflow.add_node("llm_analyze", _llm_analyze)
    workflow.add_node("validate_output", nodes.validate_output)
    workflow.add_node("retry_analysis", _retry_analysis)

    # Define edges
    workflow.set_entry_point("retrieve_data")

    workflow.add_conditional_edges(
        "retrieve_data",
        _check_error,
        {"continue": "inject_schema_context", "end": END},
    )

    workflow.add_edge("inject_schema_context", "build_analysis_prompt")

    workflow.add_conditional_edges(
        "build_analysis_prompt",
        _check_error,
        {"continue": "llm_analyze", "end": END},
    )

    workflow.add_edge("llm_analyze", "validate_output")

    workflow.add_conditional_edges(
        "validate_output",
        nodes.check_validation,
        {"retry": "retry_analysis", "end": END},
    )

    workflow.add_edge("retry_analysis", "llm_analyze")

    return workflow.compile()
