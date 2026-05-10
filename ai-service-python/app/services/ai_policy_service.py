"""AI policy enforcement service."""

import json
from typing import Optional, Dict, Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_policy import AIPolicy


class AIPolicyService:
    """Enforces AI policies before LLM operations."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_active_policy(self, tenant_id: int) -> Optional[AIPolicy]:
        """Get the active AI policy for a tenant."""
        result = await self.db.execute(
            select(AIPolicy).where(
                AIPolicy.tenant_id == tenant_id,
                AIPolicy.status == 1,
            )
        )
        return result.scalar_one_or_none()

    def _parse_rules(self, policy: AIPolicy) -> Dict[str, Any]:
        """Parse the JSON rules from policy."""
        if not policy.rules:
            return {}
        try:
            return json.loads(policy.rules) if isinstance(policy.rules, str) else policy.rules
        except (json.JSONDecodeError, TypeError):
            return {}

    async def is_allowed(self, tenant_id: int, rule_name: str) -> bool:
        """Check if a specific rule is allowed."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return True  # Default allow
        rules = self._parse_rules(policy)
        return rules.get(rule_name, True)

    async def check_sql_generation_allowed(self, tenant_id: int) -> tuple[bool, str]:
        """Check if SQL generation is allowed for the tenant."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return True, "No policy configured, defaulting to allow"
        rules = self._parse_rules(policy)
        allowed = rules.get("allow_sql_generation", True)
        if not allowed:
            return False, "SQL generation is not allowed by tenant AI policy"
        return True, "SQL generation allowed by policy"

    async def check_schema_access_allowed(self, tenant_id: int) -> bool:
        """Check if schema access is allowed for the tenant."""
        return await self.is_allowed(tenant_id, "allow_schema_access")

    async def check_cross_dataset_join(self, tenant_id: int) -> bool:
        """Check if cross-dataset joins are allowed."""
        return await self.is_allowed(tenant_id, "allow_cross_dataset_join")

    async def get_sql_constraints(self, tenant_id: int) -> dict:
        """Get SQL constraints from the policy."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return {
                "max_sql_complexity": 3,
                "allowed_sql_types": ["SELECT"],
                "max_result_rows": 10000,
                "require_where_clause": True,
            }
        rules = self._parse_rules(policy)
        return {
            "max_sql_complexity": rules.get("max_sql_complexity", 3),
            "allowed_sql_types": rules.get("allowed_sql_types", ["SELECT"]),
            "max_result_rows": rules.get("max_result_rows", 10000),
            "require_where_clause": rules.get("require_where_clause", True),
        }

    async def validate_sql_against_policy(
        self, tenant_id: int, tables: list, join_count: int, sql_type: str = "SELECT"
    ) -> tuple[bool, str]:
        """Validate generated SQL against the tenant's policy."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return True, "No policy configured"

        rules = self._parse_rules(policy)

        allowed_types = rules.get("allowed_sql_types", ["SELECT"])
        if sql_type not in allowed_types:
            return False, f"SQL type '{sql_type}' is not allowed. Allowed: {allowed_types}"

        max_complexity = rules.get("max_sql_complexity", 3)
        if join_count > max_complexity:
            return False, f"SQL complexity ({join_count} JOINs) exceeds max allowed ({max_complexity})"

        blocked = rules.get("blocked_tables", [])
        for table in tables:
            if table in blocked:
                return False, f"Table '{table}' is blocked by policy"

        return True, "SQL passes policy validation"
