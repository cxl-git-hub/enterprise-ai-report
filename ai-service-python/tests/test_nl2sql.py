"""Tests for NL2SQL pipeline."""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from uuid import uuid4

from app.engines.nl2sql.state import NL2SQLState
from app.engines.nl2sql.nodes import extract_sql


class TestNL2SQLNodes:
    """Test suite for NL2SQL node functions."""

    def test_extract_sql_from_code_block(self):
        """Test extracting SQL from markdown code block."""
        state = NL2SQLState(
            raw_llm_output='Here is the SQL query:\n\n```sql\nSELECT id, name FROM users WHERE id = 1\n```\n\nThis query retrieves user with ID 1.',
        )
        result = extract_sql(state)
        assert result["extracted_sql"] == "SELECT id, name FROM users WHERE id = 1"

    def test_extract_sql_from_plain_text(self):
        """Test extracting SQL from plain text output."""
        state = NL2SQLState(
            raw_llm_output='SELECT id, name FROM users WHERE id = 1\n\nThis query retrieves the user.',
        )
        result = extract_sql(state)
        assert "SELECT" in result["extracted_sql"]
        assert "users" in result["extracted_sql"]

    def test_extract_sql_removes_semicolon(self):
        """Test that trailing semicolons are removed."""
        state = NL2SQLState(
            raw_llm_output='```sql\nSELECT id FROM users WHERE id = 1;\n```',
        )
        result = extract_sql(state)
        assert not result["extracted_sql"].endswith(";")

    def test_extract_sql_with_explanation(self):
        """Test extracting SQL and explanation."""
        state = NL2SQLState(
            raw_llm_output='```sql\nSELECT COUNT(*) FROM orders WHERE status = \'completed\'\n```\n\nExplanation: This query counts all completed orders.',
        )
        result = extract_sql(state)
        assert "SELECT" in result["extracted_sql"]
        assert "Explanation" in result.get("explanation", "") or result.get("explanation", "") != ""

    def test_extract_sql_empty_output(self):
        """Test handling empty LLM output."""
        state = NL2SQLState(raw_llm_output="")
        result = extract_sql(state)
        assert result["extracted_sql"] == ""

    def test_extract_sql_complex_query(self):
        """Test extracting complex SQL with JOINs."""
        complex_sql = """```sql
SELECT u.name, COUNT(o.id) as order_count, SUM(o.total) as total_spent
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.created_at > '2024-01-01'
GROUP BY u.id, u.name
HAVING COUNT(o.id) > 5
ORDER BY total_spent DESC
LIMIT 10
```"""
        state = NL2SQLState(raw_llm_output=complex_sql)
        result = extract_sql(state)
        assert "SELECT" in result["extracted_sql"]
        assert "JOIN" in result["extracted_sql"]
        assert "GROUP BY" in result["extracted_sql"]
        assert "HAVING" in result["extracted_sql"]


class TestNL2SQLState:
    """Test suite for NL2SQL state management."""

    def test_state_initialization(self):
        """Test that state can be initialized with required fields."""
        state = NL2SQLState(
            tenant_id=str(uuid4()),
            user_id=str(uuid4()),
            query="Show me all active users",
            dataset_ids=[],
            max_rows=1000,
            include_explanation=True,
        )
        assert state["query"] == "Show me all active users"
        assert state["max_rows"] == 1000
        assert state["include_explanation"] is True

    def test_state_optional_fields(self):
        """Test that optional fields default correctly."""
        state = NL2SQLState(
            tenant_id=str(uuid4()),
            user_id=str(uuid4()),
            query="test",
        )
        # These should be accessible (though may raise KeyError if not set)
        assert state.get("schema_context") is None
        assert state.get("retry_count", 0) == 0
        assert state.get("max_retries", 3) == 3


class TestNL2SQLIntegration:
    """Integration tests for NL2SQL pipeline (mocked LLM)."""

    @pytest.mark.asyncio
    async def test_sql_extraction_patterns(self):
        """Test various SQL extraction patterns."""
        test_cases = [
            # Code block with sql tag
            ("```sql\nSELECT 1\n```", "SELECT 1"),
            # Code block without tag
            ("```\nSELECT 1\n```", "SELECT 1"),
            # Plain SELECT
            ("SELECT 1", "SELECT 1"),
            # SELECT with explanation
            ("SELECT 1\n\nThis is a test.", "SELECT 1"),
        ]

        for input_text, expected_sql in test_cases:
            state = NL2SQLState(raw_llm_output=input_text)
            result = extract_sql(state)
            assert expected_sql in result["extracted_sql"], f"Failed for input: {input_text}"
