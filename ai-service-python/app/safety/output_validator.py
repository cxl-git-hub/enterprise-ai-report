"""Output JSON schema validation."""

from typing import Any, Dict, List, Tuple

import jsonschema


class OutputValidator:
    """Validates LLM output against expected JSON schemas."""

    ANALYSIS_SCHEMA = {
        "type": "object",
        "properties": {
            "summary": {"type": "string"},
            "insights": {
                "type": "array",
                "items": {"type": "string"},
            },
            "recommendations": {
                "type": "array",
                "items": {"type": "string"},
            },
            "metrics": {
                "type": "object",
            },
        },
        "required": ["summary"],
    }

    REPORT_SCHEMA = {
        "type": "object",
        "properties": {
            "title": {"type": "string"},
            "sections": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "heading": {"type": "string"},
                        "content": {"type": "string"},
                    },
                    "required": ["heading", "content"],
                },
            },
        },
        "required": ["title"],
    }

    SQL_RESULT_SCHEMA = {
        "type": "object",
        "properties": {
            "sql": {"type": "string"},
            "explanation": {"type": "string"},
            "columns": {
                "type": "array",
                "items": {"type": "string"},
            },
            "row_count": {"type": "integer"},
        },
        "required": ["sql"],
    }

    def validate_analysis_output(self, data: Any) -> Tuple[bool, List[str]]:
        """Validate analysis output against schema."""
        return self._validate(data, self.ANALYSIS_SCHEMA)

    def validate_report_output(self, data: Any) -> Tuple[bool, List[str]]:
        """Validate report output against schema."""
        return self._validate(data, self.REPORT_SCHEMA)

    def validate_sql_result(self, data: Any) -> Tuple[bool, List[str]]:
        """Validate SQL result against schema."""
        return self._validate(data, self.SQL_RESULT_SCHEMA)

    def validate_custom(self, data: Any, schema: Dict[str, Any]) -> Tuple[bool, List[str]]:
        """Validate data against a custom schema."""
        return self._validate(data, schema)

    def _validate(self, data: Any, schema: Dict[str, Any]) -> Tuple[bool, List[str]]:
        """Validate data against a JSON schema."""
        errors = []
        try:
            jsonschema.validate(instance=data, schema=schema)
            return True, []
        except jsonschema.ValidationError as e:
            errors.append(f"Validation error: {e.message}")
            return False, errors
        except jsonschema.SchemaError as e:
            errors.append(f"Schema error: {e.message}")
            return False, errors
        except Exception as e:
            errors.append(f"Unexpected error: {str(e)}")
            return False, errors
