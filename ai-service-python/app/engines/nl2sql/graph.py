"""NL2SQL LangGraph workflow definition."""

from typing import Any, Dict

from langgraph.graph import END, StateGraph

from app.engines.nl2sql.state import NL2SQLState
from app.engines.nl2sql import nodes
from sqlalchemy.ext.asyncio import AsyncSession


def create_nl2sql_graph(db: AsyncSession):
    """Create the NL2SQL LangGraph workflow."""

    async def _get_schema_context(state: NL2SQLState) -> Dict[str, Any]:
        return await nodes.get_schema_context(state, db)

    async def _check_policy(state: NL2SQLState) -> Dict[str, Any]:
        return await nodes.check_policy(state, db)

    async def _build_prompt(state: NL2SQLState) -> Dict[str, Any]:
        return await nodes.build_prompt(state, db)

    async def _llm_generate_sql(state: NL2SQLState) -> Dict[str, Any]:
        return await nodes.llm_generate_sql(state, db)

    async def _validate_sql(state: NL2SQLState) -> Dict[str, Any]:
        return await nodes.validate_sql(state, db)

    async def _retry_prompt(state: NL2SQLState) -> Dict[str, Any]:
        return await nodes.retry_prompt(state, db)

    async def _execute_sql(state: NL2SQLState) -> Dict[str, Any]:
        return await nodes.execute_sql(state, db)

    def _should_retry(state: NL2SQLState) -> str:
        """Decide whether to retry or proceed."""
        if state.get("error_message"):
            return "end"
        if state.get("is_valid"):
            return "execute"
        if state.get("retry_count", 0) >= state.get("max_retries", 3):
            return "end"
        return "retry"

    def _check_error_after_policy(state: NL2SQLState) -> str:
        """Check if policy check failed."""
        if state.get("error_message"):
            return "end"
        return "continue"

    # Build the graph
    workflow = StateGraph(NL2SQLState)

    # Add nodes
    workflow.add_node("get_schema_context", _get_schema_context)
    workflow.add_node("check_policy", _check_policy)
    workflow.add_node("build_prompt", _build_prompt)
    workflow.add_node("llm_generate_sql", _llm_generate_sql)
    workflow.add_node("extract_sql", nodes.extract_sql)
    workflow.add_node("validate_sql", _validate_sql)
    workflow.add_node("retry_prompt", _retry_prompt)
    workflow.add_node("execute_sql", _execute_sql)

    # Define edges
    workflow.set_entry_point("get_schema_context")
    workflow.add_edge("get_schema_context", "check_policy")

    workflow.add_conditional_edges(
        "check_policy",
        _check_error_after_policy,
        {"continue": "build_prompt", "end": END},
    )

    workflow.add_edge("build_prompt", "llm_generate_sql")
    workflow.add_edge("llm_generate_sql", "extract_sql")
    workflow.add_edge("extract_sql", "validate_sql")

    workflow.add_conditional_edges(
        "validate_sql",
        _should_retry,
        {"retry": "retry_prompt", "execute": "execute_sql", "end": END},
    )

    workflow.add_edge("retry_prompt", "llm_generate_sql")
    workflow.add_edge("execute_sql", END)

    return workflow.compile()
