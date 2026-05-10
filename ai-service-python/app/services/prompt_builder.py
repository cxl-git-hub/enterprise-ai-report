"""Structured prompt construction for LLM calls."""

from typing import Any, Dict, List, Optional


class PromptBuilder:
    """Builds structured prompts with schema context, examples, and constraints."""

    def __init__(self, max_tokens: int = 128000):
        self.max_tokens = max_tokens

    def build_nl2sql_prompt(
        self,
        system_prompt: str,
        user_query: str,
        schema_context: str,
        few_shot_examples: Optional[List[Dict[str, str]]] = None,
        constraints: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, str]:
        """Build a complete NL2SQL prompt."""
        # Build system message
        system_parts = [system_prompt]

        if schema_context:
            system_parts.append(f"\n\n## Database Schema\n{schema_context}")

        if constraints:
            constraint_text = self._format_constraints(constraints)
            system_parts.append(f"\n\n## Constraints\n{constraint_text}")

        full_system = "\n".join(system_parts)

        # Build user message
        user_parts = []

        if few_shot_examples:
            user_parts.append("## Examples")
            for i, example in enumerate(few_shot_examples, 1):
                user_parts.append(f"\n### Example {i}")
                user_parts.append(f"Question: {example.get('question', '')}")
                user_parts.append(f"SQL: {example.get('sql', '')}")
                if example.get("explanation"):
                    user_parts.append(f"Explanation: {example['explanation']}")
            user_parts.append("")

        user_parts.append(f"## Question\n{user_query}")
        user_parts.append("\nGenerate a SQL query to answer this question. Return ONLY the SQL query in a ```sql code block.")

        full_user = "\n".join(user_parts)

        return {
            "system": self._truncate_to_fit(full_system),
            "user": self._truncate_to_fit(full_user),
        }

    def build_analysis_prompt(
        self,
        system_prompt: str,
        user_query: str,
        data_context: str,
        schema_context: str,
        analysis_type: str = "general",
    ) -> Dict[str, str]:
        """Build a data analysis prompt."""
        system_parts = [system_prompt]

        if schema_context:
            system_parts.append(f"\n\n## Data Schema\n{schema_context}")

        system_parts.append(f"\n\n## Analysis Type: {analysis_type}")
        system_parts.append("\nProvide your analysis as structured JSON with the following fields:")
        system_parts.append("- summary: Brief summary of findings")
        system_parts.append("- insights: List of key insights")
        system_parts.append("- recommendations: List of actionable recommendations")
        system_parts.append("- metrics: Key metrics and values")

        full_system = "\n".join(system_parts)

        user_parts = []
        if data_context:
            user_parts.append(f"## Data\n{data_context}")
        user_parts.append(f"\n## Analysis Request\n{user_query}")

        full_user = "\n".join(user_parts)

        return {
            "system": self._truncate_to_fit(full_system),
            "user": self._truncate_to_fit(full_user),
        }

    def build_report_prompt(
        self,
        system_prompt: str,
        title: str,
        description: str,
        data_context: str,
        kpi_context: str,
        template_context: str,
    ) -> Dict[str, str]:
        """Build a report generation prompt."""
        system_parts = [system_prompt]

        if template_context:
            system_parts.append(f"\n\n## Report Template\n{template_context}")

        system_parts.append("\n\nGenerate a professional report with the following structure:")
        system_parts.append("1. Executive Summary")
        system_parts.append("2. Key Metrics and KPIs")
        system_parts.append("3. Detailed Analysis")
        system_parts.append("4. Trends and Patterns")
        system_parts.append("5. Recommendations")
        system_parts.append("6. Conclusion")

        full_system = "\n".join(system_parts)

        user_parts = [f"## Report Title\n{title}"]
        if description:
            user_parts.append(f"\n## Description\n{description}")
        if kpi_context:
            user_parts.append(f"\n## KPI Data\n{kpi_context}")
        if data_context:
            user_parts.append(f"\n## Supporting Data\n{data_context}")
        user_parts.append("\nGenerate the complete report content in Markdown format.")

        full_user = "\n".join(user_parts)

        return {
            "system": self._truncate_to_fit(full_system),
            "user": self._truncate_to_fit(full_user),
        }

    def _format_constraints(self, constraints: Dict[str, Any]) -> str:
        """Format constraints into readable text."""
        parts = []
        if "allowed_sql_types" in constraints:
            parts.append(f"- Allowed SQL types: {', '.join(constraints['allowed_sql_types'])}")
        if "max_sql_complexity" in constraints:
            parts.append(f"- Maximum JOINs: {constraints['max_sql_complexity']}")
        if "max_result_rows" in constraints:
            parts.append(f"- Maximum result rows: {constraints['max_result_rows']}")
        if "require_where_clause" in constraints:
            parts.append(f"- WHERE clause required: {constraints['require_where_clause']}")
        return "\n".join(parts)

    def _truncate_to_fit(self, text: str) -> str:
        """Truncate text to fit within token limit (rough estimate: 4 chars per token)."""
        max_chars = self.max_tokens * 4
        if len(text) <= max_chars:
            return text
        return text[:max_chars - 100] + "\n\n[Content truncated to fit context window]"
