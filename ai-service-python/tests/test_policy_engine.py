"""Tests for AI policy enforcement engine."""

import pytest

from app.safety.policy_engine import PolicyEngine


class TestPolicyEngine:
    """Test suite for the PolicyEngine."""

    def test_default_policy_allows_sql_generation(self):
        """Test that default policy allows SQL generation."""
        engine = PolicyEngine()
        assert engine.check_sql_generation()

    def test_policy_disables_sql_generation(self):
        """Test that SQL generation can be disabled."""
        engine = PolicyEngine({"allow_sql_generation": False})
        assert not engine.check_sql_generation()
        assert len(engine.violations) == 1
        assert engine.violations[0].rule == "allow_sql_generation"

    def test_default_policy_allows_schema_access(self):
        """Test that default policy allows schema access."""
        engine = PolicyEngine()
        assert engine.check_schema_access()

    def test_policy_disables_schema_access(self):
        """Test that schema access can be disabled."""
        engine = PolicyEngine({"allow_schema_access": False})
        assert not engine.check_schema_access()

    def test_default_policy_blocks_cross_dataset_join(self):
        """Test that cross-dataset joins are blocked by default."""
        engine = PolicyEngine()
        assert not engine.check_cross_dataset_join(2)

    def test_policy_allows_cross_dataset_join(self):
        """Test that cross-dataset joins can be allowed."""
        engine = PolicyEngine({"allow_cross_dataset_join": True})
        assert engine.check_cross_dataset_join(2)

    def test_single_table_not_cross_dataset(self):
        """Test that single table queries don't trigger cross-dataset check."""
        engine = PolicyEngine()
        assert engine.check_cross_dataset_join(1)

    def test_sql_complexity_within_limit(self):
        """Test that SQL within complexity limit passes."""
        engine = PolicyEngine({"max_sql_complexity": 3})
        assert engine.check_sql_complexity(2)

    def test_sql_complexity_exceeds_limit(self):
        """Test that SQL exceeding complexity limit fails."""
        engine = PolicyEngine({"max_sql_complexity": 2})
        assert not engine.check_sql_complexity(5)

    def test_allowed_sql_types(self):
        """Test SQL type checking."""
        engine = PolicyEngine({"allowed_sql_types": ["SELECT"]})
        assert engine.check_sql_type("SELECT")
        assert not engine.check_sql_type("INSERT")
        assert not engine.check_sql_type("DELETE")

    def test_multiple_allowed_sql_types(self):
        """Test multiple allowed SQL types."""
        engine = PolicyEngine({"allowed_sql_types": ["SELECT", "INSERT"]})
        assert engine.check_sql_type("SELECT")
        assert engine.check_sql_type("INSERT")
        assert not engine.check_sql_type("DELETE")

    def test_result_rows_within_limit(self):
        """Test result rows within limit."""
        engine = PolicyEngine({"max_result_rows": 10000})
        assert engine.check_result_rows(5000)

    def test_result_rows_exceeds_limit(self):
        """Test result rows exceeding limit."""
        engine = PolicyEngine({"max_result_rows": 1000})
        assert not engine.check_result_rows(5000)

    def test_result_rows_no_limit(self):
        """Test that no limit specified passes."""
        engine = PolicyEngine()
        assert engine.check_result_rows(None)

    def test_blocked_tables(self):
        """Test blocked tables checking."""
        engine = PolicyEngine({"blocked_tables": ["admin_users", "secrets"]})
        assert engine.check_blocked_tables(["users", "orders"])
        assert not engine.check_blocked_tables(["users", "secrets"])

    def test_allowed_tables(self):
        """Test allowed tables checking."""
        engine = PolicyEngine({"allowed_tables": ["users", "orders"]})
        assert engine.check_allowed_tables(["users", "orders"])
        assert not engine.check_allowed_tables(["users", "admin_secrets"])

    def test_no_table_restriction(self):
        """Test that no allowed_tables config means all are allowed."""
        engine = PolicyEngine()
        assert engine.check_allowed_tables(["any_table"])

    def test_enforce_all_passes(self):
        """Test that enforce_all passes with default config."""
        engine = PolicyEngine()
        is_valid, violations = engine.enforce_all(
            sql_type="SELECT",
            tables=["users"],
            join_count=1,
            limit=100,
            table_count=1,
        )
        assert is_valid
        assert len(violations) == 0

    def test_enforce_all_fails_multiple_violations(self):
        """Test that enforce_all catches multiple violations."""
        engine = PolicyEngine({
            "allow_sql_generation": False,
            "allowed_sql_types": ["SELECT"],
            "max_sql_complexity": 1,
            "blocked_tables": ["secrets"],
        })
        is_valid, violations = engine.enforce_all(
            sql_type="INSERT",
            tables=["secrets"],
            join_count=5,
            limit=100,
            table_count=1,
        )
        assert not is_valid
        assert len(violations) >= 3  # sql_generation, sql_type, complexity, blocked_table

    def test_enforce_all_resets_violations(self):
        """Test that enforce_all resets violations each call."""
        engine = PolicyEngine({"allow_sql_generation": False})

        # First call
        is_valid1, violations1 = engine.enforce_all()
        assert not is_valid1
        assert len(violations1) >= 1

        # Second call with different config
        engine.config = {"allow_sql_generation": True}
        is_valid2, violations2 = engine.enforce_all()
        # Should not include previous violations
        assert all(v["rule"] != "allow_sql_generation" for v in violations2)

    def test_violation_to_dict(self):
        """Test violation serialization."""
        engine = PolicyEngine({"allow_sql_generation": False})
        is_valid, violations = engine.enforce_all()
        assert len(violations) > 0
        violation = violations[0]
        assert "rule" in violation
        assert "message" in violation
        assert "severity" in violation

    def test_reset_clears_violations(self):
        """Test that reset clears violation history."""
        engine = PolicyEngine({"allow_sql_generation": False})
        engine.check_sql_generation()
        assert len(engine.violations) == 1
        engine.reset()
        assert len(engine.violations) == 0
