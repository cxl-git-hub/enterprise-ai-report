"""AI policy enforcement service."""

from typing import Optional
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_policy import AIPolicy


class AIPolicyService:
    """Enforces AI policies before LLM operations."""

    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_active_policy(self, tenant_id: UUID) -> Optional[AIPolicy]:
        """Get the active AI policy for a tenant."""
        result = await self.db.execute(
            select(AIPolicy).where(
                AIPolicy.tenant_id == tenant_id,
                AIPolicy.is_active == True,
            )
        )
        return result.scalar_one_or_none()

    async def check_sql_generation_allowed(self, tenant_id: UUID) -> tuple[bool, str]:
        """Check if SQL generation is allowed for the tenant."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return True, "No policy configured, defaulting to allow"
        if not policy.allow_sql_generation:
            return False, "SQL generation is not allowed by policy"
        return True, "SQL generation allowed"

    async def check_schema_access_allowed(self, tenant_id: UUID) -> tuple[bool, str]:
        """Check if schema access is allowed for the tenant."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return True, "No policy configured, defaulting to allow"
        if not policy.allow_schema_access:
            return False, "Schema access is not allowed by policy"
        return True, "Schema access allowed"

    async def check_cross_dataset_join(self, tenant_id: UUID) -> tuple[bool, str]:
        """Check if cross-dataset joins are allowed."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return False, "No policy configured, cross-dataset joins disabled by default"
        if not policy.allow_cross_dataset_join:
            return False, "Cross-dataset joins are not allowed by policy"
        return True, "Cross-dataset joins allowed"

    async def get_sql_constraints(self, tenant_id: UUID) -> dict:
        """Get SQL constraints from the policy."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return {
                "max_sql_complexity": 3,
                "allowed_sql_types": ["SELECT"],
                "max_result_rows": 10000,
                "require_where_clause": True,
            }
        return {
            "max_sql_complexity": policy.max_sql_complexity,
            "allowed_sql_types": policy.allowed_sql_types,
            "max_result_rows": policy.max_result_rows,
            "require_where_clause": policy.require_where_clause,
        }

    async def validate_sql_against_policy(
        self, tenant_id: UUID, tables: list[str], join_count: int, sql_type: str = "SELECT"
    ) -> tuple[bool, str]:
        """Validate generated SQL against the tenant's policy."""
        policy = await self.get_active_policy(tenant_id)
        if not policy:
            return True, "No policy configured"

        # Check SQL type
        if sql_type not in policy.allowed_sql_types:
            return False, f"SQL type '{sql_type}' is not allowed. Allowed: {policy.allowed_sql_types}"

        # Check JOIN complexity
        if join_count > policy.max_sql_complexity:
            return False, f"SQL complexity ({join_count} JOINs) exceeds max allowed ({policy.max_sql_complexity})"

        # Check blocked tables
        if policy.blocked_tables:
            for table in tables:
                if table in policy.blocked_tables:
                    return False, f"Table '{table}' is blocked by policy"

        # Check allowed datasets
        if policy.allowed_datasets:
            # This would need dataset-to-table mapping; simplified here
            pass

        return True, "SQL passes policy validation"
