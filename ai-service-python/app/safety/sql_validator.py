"""SQL safety validator using AST parsing.

This module provides comprehensive SQL validation including:
- Statement type checking (SELECT only)
- Table and column validation against schema registry
- Injection pattern detection
- JOIN complexity limits
- WHERE clause requirements
- LIMIT enforcement
"""

import re
from typing import Any, Dict, List, Optional, Set

import sqlparse
from sqlparse.sql import Identifier, IdentifierList, Parenthesis, Statement, Where
from sqlparse.tokens import DML, Keyword, Punctuation


class SQLValidationResult:
    """Result of SQL validation."""

    def __init__(self):
        self.is_valid: bool = True
        self.errors: List[str] = []
        self.warnings: List[str] = []
        self.tables_found: List[str] = []
        self.columns_found: List[str] = []
        self.has_where_clause: bool = False
        self.join_count: int = 0
        self.limit_value: Optional[int] = None
        self.sql_type: str = "SELECT"

    def add_error(self, error: str) -> None:
        self.errors.append(error)
        self.is_valid = False

    def add_warning(self, warning: str) -> None:
        self.warnings.append(warning)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "is_valid": self.is_valid,
            "errors": self.errors,
            "warnings": self.warnings,
            "tables_found": self.tables_found,
            "columns_found": self.columns_found,
            "has_where_clause": self.has_where_clause,
            "join_count": self.join_count,
            "limit_value": self.limit_value,
            "sql_type": self.sql_type,
        }


class SafetySQLValidator:
    """Validates SQL for safety, correctness, and policy compliance."""

    DANGEROUS_KEYWORDS = {
        "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "EXEC", "EXECUTE",
        "TRUNCATE", "GRANT", "REVOKE", "CREATE", "REPLACE",
    }

    INJECTION_PATTERNS = [
        "UNION", "UNION ALL", "INTO OUTFILE", "INTO DUMPFILE",
        "LOAD_FILE", "BENCHMARK", "SLEEP", "WAITFOR",
    ]

    def __init__(
        self,
        allowed_tables: Optional[Set[str]] = None,
        schema_columns: Optional[Dict[str, Set[str]]] = None,
        max_joins: int = 3,
        max_limit: int = 10000,
        require_where: bool = True,
    ):
        self.allowed_tables = allowed_tables or set()
        self.schema_columns = schema_columns or {}
        self.max_joins = max_joins
        self.max_limit = max_limit
        self.require_where = require_where

    def validate(self, sql: str) -> SQLValidationResult:
        """Run all validation checks on a SQL statement."""
        result = SQLValidationResult()

        if not sql or not sql.strip():
            result.add_error("Empty SQL statement")
            return result

        try:
            parsed = sqlparse.parse(sql.strip().rstrip(";"))
            if not parsed:
                result.add_error("Failed to parse SQL")
                return result
            stmt = parsed[0]
        except Exception as e:
            result.add_error(f"SQL parse error: {str(e)}")
            return result

        result.sql_type = self._get_statement_type(stmt)

        if result.sql_type != "SELECT":
            result.add_error(f"Only SELECT statements are allowed, got: {result.sql_type}")
            return result

        self._check_dangerous_keywords(sql, result)
        if not result.is_valid:
            return result

        self._check_injection_patterns(sql, result)
        if not result.is_valid:
            return result

        self._extract_tables(stmt, result)
        if self.allowed_tables:
            self._validate_tables(result)

        self._extract_columns(stmt, result)
        if self.schema_columns:
            self._validate_columns(result)

        self._count_joins(sql, result)
        if result.join_count > self.max_joins:
            result.add_error(f"Too many JOINs: {result.join_count} (max: {self.max_joins})")

        self._check_where_clause(stmt, result)
        if self.require_where and not result.has_where_clause:
            result.add_error("WHERE clause is required")

        self._check_limit(sql, result)
        if result.limit_value and result.limit_value > self.max_limit:
            result.add_error(f"LIMIT {result.limit_value} exceeds maximum: {self.max_limit}")

        return result

    def _get_statement_type(self, stmt: Statement) -> str:
        for token in stmt.tokens:
            if token.ttype is DML:
                return token.normalized.upper()
            if token.ttype is Keyword:
                if token.normalized.upper() in self.DANGEROUS_KEYWORDS:
                    return token.normalized.upper()
        return "UNKNOWN"

    def _check_dangerous_keywords(self, sql: str, result: SQLValidationResult) -> None:
        sql_upper = sql.upper()
        for keyword in self.DANGEROUS_KEYWORDS:
            if re.search(r'\b' + keyword + r'\b', sql_upper):
                result.add_error(f"Dangerous keyword detected: {keyword}")

    def _check_injection_patterns(self, sql: str, result: SQLValidationResult) -> None:
        sql_upper = sql.upper()
        for pattern in self.INJECTION_PATTERNS:
            if pattern in sql_upper:
                result.add_error(f"Potential SQL injection pattern: {pattern}")

    def _extract_tables(self, stmt: Statement, result: SQLValidationResult) -> None:
        tables = set()
        from_seen = False
        join_seen = False

        for token in stmt.flatten():
            if token.ttype is Keyword and token.normalized.upper() == "FROM":
                from_seen = True
                join_seen = False
                continue
            if token.ttype is Keyword and "JOIN" in token.normalized.upper():
                join_seen = True
                from_seen = False
                continue
            if token.ttype is Keyword:
                from_seen = False
                join_seen = False
                continue

            if (from_seen or join_seen) and token.ttype is not Punctuation:
                name = token.normalized.strip().strip('"').strip('`')
                if name and not name.startswith("(") and name.upper() not in ("AS", "ON", "WHERE", "AND", "OR"):
                    parts = name.split(".")
                    table_name = parts[-1] if len(parts) > 1 else parts[0]
                    if table_name:
                        tables.add(table_name.lower())

        result.tables_found = list(tables)

    def _validate_tables(self, result: SQLValidationResult) -> None:
        for table in result.tables_found:
            if table not in self.allowed_tables:
                result.add_error(f"Table '{table}' is not in the allowed table list")

    def _extract_columns(self, stmt: Statement, result: SQLValidationResult) -> None:
        columns = []
        select_seen = False

        for token in stmt.tokens:
            if token.ttype is DML and token.normalized.upper() == "SELECT":
                select_seen = True
                continue
            if select_seen:
                if token.ttype is Keyword and token.normalized.upper() == "FROM":
                    break
                if isinstance(token, IdentifierList):
                    for identifier in token.get_identifiers():
                        col_name = self._extract_column_name(identifier)
                        if col_name and col_name != "*":
                            columns.append(col_name)
                elif isinstance(token, Identifier):
                    col_name = self._extract_column_name(token)
                    if col_name and col_name != "*":
                        columns.append(col_name)

        result.columns_found = columns

    def _extract_column_name(self, identifier) -> Optional[str]:
        name = identifier.get_name()
        if not name:
            return None
        if "." in name:
            return name.split(".")[-1].lower()
        return name.lower()

    def _validate_columns(self, result: SQLValidationResult) -> None:
        for col in result.columns_found:
            found = any(col in cols for cols in self.schema_columns.values())
            if not found:
                result.add_warning(f"Column '{col}' not found in schema registry")

    def _count_joins(self, sql: str, result: SQLValidationResult) -> None:
        result.join_count = len(re.findall(r'\bJOIN\b', sql, re.IGNORECASE))

    def _check_where_clause(self, stmt: Statement, result: SQLValidationResult) -> None:
        for token in stmt.tokens:
            if isinstance(token, Where):
                result.has_where_clause = True
                return
        result.has_where_clause = False

    def _check_limit(self, sql: str, result: SQLValidationResult) -> None:
        limit_match = re.search(r'\bLIMIT\s+(\d+)', sql, re.IGNORECASE)
        if limit_match:
            result.limit_value = int(limit_match.group(1))
