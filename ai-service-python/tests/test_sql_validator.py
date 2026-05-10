"""Tests for SQL safety validation."""

import pytest

from app.safety.sql_validator import SafetySQLValidator, SQLValidationResult


class TestSQLValidator:
    """Test suite for SQL safety validation."""

    def setup_method(self):
        """Set up test fixtures."""
        self.validator = SafetySQLValidator(
            allowed_tables={"users", "orders", "products", "order_items"},
            schema_columns={
                "users": {"id", "name", "email", "created_at"},
                "orders": {"id", "user_id", "total", "status", "created_at"},
                "products": {"id", "name", "price", "category"},
                "order_items": {"id", "order_id", "product_id", "quantity", "price"},
            },
            max_joins=3,
            max_limit=10000,
            require_where=True,
        )

    def test_valid_select_query(self):
        """Test that a valid SELECT query passes validation."""
        sql = "SELECT id, name, email FROM users WHERE id = 1"
        result = self.validator.validate(sql)
        assert result.is_valid
        assert result.sql_type == "SELECT"
        assert "users" in result.tables_found
        assert result.has_where_clause

    def test_reject_non_select(self):
        """Test that non-SELECT statements are rejected."""
        statements = [
            "INSERT INTO users (name) VALUES ('test')",
            "UPDATE users SET name = 'test' WHERE id = 1",
            "DELETE FROM users WHERE id = 1",
            "DROP TABLE users",
            "ALTER TABLE users ADD COLUMN test TEXT",
            "TRUNCATE TABLE users",
        ]
        for sql in statements:
            result = self.validator.validate(sql)
            assert not result.is_valid, f"Should reject: {sql}"
            assert any("Only SELECT" in e or "Dangerous keyword" in e for e in result.errors)

    def test_reject_dangerous_keywords(self):
        """Test that dangerous keywords are detected."""
        sql = "SELECT * FROM users; DROP TABLE users; --"
        result = self.validator.validate(sql)
        assert not result.is_valid
        assert any("DROP" in e for e in result.errors)

    def test_reject_union_injection(self):
        """Test that UNION-based injection is detected."""
        sql = "SELECT id, name FROM users WHERE id = 1 UNION SELECT id, password FROM admin_users"
        result = self.validator.validate(sql)
        assert not result.is_valid
        assert any("UNION" in e for e in result.errors)

    def test_reject_sleep_injection(self):
        """Test that SLEEP-based injection is detected."""
        sql = "SELECT id FROM users WHERE id = 1 AND SLEEP(5)"
        result = self.validator.validate(sql)
        assert not result.is_valid
        assert any("SLEEP" in e for e in result.errors)

    def test_reject_unallowed_tables(self):
        """Test that queries to unallowed tables are rejected."""
        sql = "SELECT * FROM admin_secrets WHERE 1=1"
        result = self.validator.validate(sql)
        assert not result.is_valid
        assert any("not in the allowed" in e for e in result.errors)

    def test_allow_valid_tables(self):
        """Test that queries to allowed tables pass."""
        sql = "SELECT id, name FROM products WHERE category = 'electronics'"
        result = self.validator.validate(sql)
        assert result.is_valid
        assert "products" in result.tables_found

    def test_reject_too_many_joins(self):
        """Test that queries with too many JOINs are rejected."""
        sql = """
        SELECT u.name, o.total, p.name, oi.quantity
        FROM users u
        JOIN orders o ON u.id = o.user_id
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        WHERE u.id = 1
        """
        result = self.validator.validate(sql)
        assert not result.is_valid
        assert any("JOINs" in e for e in result.errors)

    def test_valid_joins_within_limit(self):
        """Test that JOINs within the limit are accepted."""
        validator = SafetySQLValidator(
            allowed_tables={"users", "orders", "products", "order_items"},
            schema_columns={
                "users": {"id", "name"},
                "orders": {"id", "user_id", "total"},
                "order_items": {"id", "order_id", "product_id"},
                "products": {"id", "name"},
            },
            max_joins=4,
            max_limit=10000,
            require_where=True,
        )
        sql = """
        SELECT u.name, o.total, p.name
        FROM users u
        JOIN orders o ON u.id = o.user_id
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        WHERE u.id = 1
        """
        result = validator.validate(sql)
        assert result.is_valid
        assert result.join_count == 3

    def test_reject_limit_exceeding_max(self):
        """Test that LIMIT exceeding max is rejected."""
        sql = "SELECT id FROM users WHERE id > 0 LIMIT 50000"
        result = self.validator.validate(sql)
        assert not result.is_valid
        assert any("LIMIT" in e for e in result.errors)

    def test_valid_limit_within_bounds(self):
        """Test that LIMIT within bounds is accepted."""
        sql = "SELECT id FROM users WHERE id > 0 LIMIT 100"
        result = self.validator.validate(sql)
        assert result.is_valid
        assert result.limit_value == 100

    def test_warn_no_where_clause(self):
        """Test that missing WHERE clause generates a warning."""
        validator = SafetySQLValidator(
            allowed_tables={"users"},
            schema_columns={"users": {"id", "name"}},
            max_joins=3,
            max_limit=10000,
            require_where=False,
        )
        sql = "SELECT id, name FROM users LIMIT 100"
        result = validator.validate(sql)
        assert result.is_valid
        assert not result.has_where_clause

    def test_reject_no_where_when_required(self):
        """Test that missing WHERE clause is rejected when required."""
        sql = "SELECT id, name FROM users LIMIT 100"
        result = self.validator.validate(sql)
        assert not result.is_valid
        assert any("WHERE" in e for e in result.errors)

    def test_reject_empty_sql(self):
        """Test that empty SQL is rejected."""
        result = self.validator.validate("")
        assert not result.is_valid
        assert any("Empty" in e for e in result.errors)

    def test_extract_columns(self):
        """Test column extraction from SELECT clause."""
        sql = "SELECT id, name, email FROM users WHERE id = 1"
        result = self.validator.validate(sql)
        assert "id" in result.columns_found
        assert "name" in result.columns_found
        assert "email" in result.columns_found

    def test_handle_schema_table_references(self):
        """Test handling of schema.table references."""
        sql = "SELECT public.users.id FROM public.users WHERE public.users.id = 1"
        result = self.validator.validate(sql)
        # Should extract 'users' as the table name
        assert "users" in result.tables_found

    def test_reject_exec_keyword(self):
        """Test that EXEC keyword is rejected."""
        sql = "SELECT EXEC malicious_function() FROM users"
        result = self.validator.validate(sql)
        assert not result.is_valid

    def test_to_dict(self):
        """Test that to_dict returns proper structure."""
        sql = "SELECT id, name FROM users WHERE id = 1 LIMIT 10"
        result = self.validator.validate(sql)
        d = result.to_dict()
        assert "is_valid" in d
        assert "errors" in d
        assert "warnings" in d
        assert "tables_found" in d
        assert "columns_found" in d
        assert "has_where_clause" in d
        assert "join_count" in d
        assert "limit_value" in d
        assert "sql_type" in d
