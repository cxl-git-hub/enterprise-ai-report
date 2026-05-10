"""AI policy enforcement engine."""

from typing import Any, Dict, List, Optional, Set, Tuple


class PolicyViolation:
    """Represents a policy violation."""

    def __init__(self, rule: str, message: str, severity: str = "error"):
        self.rule = rule
        self.message = message
        self.severity = severity

    def to_dict(self) -> Dict[str, str]:
        return {
            "rule": self.rule,
            "message": self.message,
            "severity": self.severity,
        }


class PolicyEngine:
    """Enforces AI policies for SQL generation and data access."""

    def __init__(self, policy_config: Optional[Dict[str, Any]] = None):
        """
        Initialize the policy engine.

        Args:
            policy_config: Policy configuration dictionary
        """
        self.config = policy_config or {}
        self.violations: List[PolicyViolation] = []

    def reset(self) -> None:
        """Clear violation history."""
        self.violations = []

    def check_sql_generation(self) -> bool:
        """Check if SQL generation is allowed."""
        if not self.config.get("allow_sql_generation", True):
            self.violations.append(
                PolicyViolation(
                    rule="allow_sql_generation",
                    message="SQL generation is not allowed by policy",
                )
            )
            return False
        return True

    def check_schema_access(self) -> bool:
        """Check if schema access is allowed."""
        if not self.config.get("allow_schema_access", True):
            self.violations.append(
                PolicyViolation(
                    rule="allow_schema_access",
                    message="Schema access is not allowed by policy",
                )
            )
            return False
        return True

    def check_cross_dataset_join(self, table_count: int) -> bool:
        """Check if cross-dataset joins are allowed."""
        if table_count > 1 and not self.config.get("allow_cross_dataset_join", False):
            self.violations.append(
                PolicyViolation(
                    rule="allow_cross_dataset_join",
                    message="Cross-dataset joins are not allowed by policy",
                )
            )
            return False
        return True

    def check_sql_complexity(self, join_count: int) -> bool:
        """Check if SQL complexity is within limits."""
        max_complexity = self.config.get("max_sql_complexity", 3)
        if join_count > max_complexity:
            self.violations.append(
                PolicyViolation(
                    rule="max_sql_complexity",
                    message=f"SQL complexity ({join_count} JOINs) exceeds maximum ({max_complexity})",
                )
            )
            return False
        return True

    def check_sql_type(self, sql_type: str) -> bool:
        """Check if the SQL type is allowed."""
        allowed_types = self.config.get("allowed_sql_types", ["SELECT"])
        if sql_type.upper() not in allowed_types:
            self.violations.append(
                PolicyViolation(
                    rule="allowed_sql_types",
                    message=f"SQL type '{sql_type}' is not allowed. Allowed: {allowed_types}",
                )
            )
            return False
        return True

    def check_result_rows(self, limit: Optional[int]) -> bool:
        """Check if the result row limit is within bounds."""
        max_rows = self.config.get("max_result_rows", 10000)
        if limit and limit > max_rows:
            self.violations.append(
                PolicyViolation(
                    rule="max_result_rows",
                    message=f"Requested limit ({limit}) exceeds maximum ({max_rows})",
                )
            )
            return False
        return True

    def check_blocked_tables(self, tables: List[str]) -> bool:
        """Check if any tables are blocked."""
        blocked = set(self.config.get("blocked_tables", []))
        violations_found = False
        for table in tables:
            if table.lower() in {b.lower() for b in blocked}:
                self.violations.append(
                    PolicyViolation(
                        rule="blocked_tables",
                        message=f"Table '{table}' is blocked by policy",
                    )
                )
                violations_found = True
        return not violations_found

    def check_allowed_tables(self, tables: List[str]) -> bool:
        """Check if all tables are in the allowed list."""
        allowed = self.config.get("allowed_tables")
        if not allowed:
            return True  # No restriction

        allowed_set = {t.lower() for t in allowed}
        violations_found = False
        for table in tables:
            if table.lower() not in allowed_set:
                self.violations.append(
                    PolicyViolation(
                        rule="allowed_tables",
                        message=f"Table '{table}' is not in the allowed list",
                    )
                )
                violations_found = True
        return not violations_found

    def enforce_all(
        self,
        sql_type: str = "SELECT",
        tables: Optional[List[str]] = None,
        join_count: int = 0,
        limit: Optional[int] = None,
        table_count: int = 1,
    ) -> Tuple[bool, List[Dict[str, str]]]:
        """
        Run all policy checks.

        Returns:
            Tuple of (is_valid, list_of_violations)
        """
        self.reset()

        self.check_sql_generation()
        self.check_schema_access()
        self.check_sql_type(sql_type)
        self.check_sql_complexity(join_count)
        self.check_cross_dataset_join(table_count)
        self.check_result_rows(limit)

        if tables:
            self.check_blocked_tables(tables)
            self.check_allowed_tables(tables)

        is_valid = len(self.violations) == 0
        violation_dicts = [v.to_dict() for v in self.violations]

        return is_valid, violation_dicts
