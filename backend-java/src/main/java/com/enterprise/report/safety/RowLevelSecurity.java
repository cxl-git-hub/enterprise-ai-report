package com.enterprise.report.safety;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Row-level security service.
 * Filters data based on user role, tenant, and permission rules.
 */
@Slf4j
@Component
public class RowLevelSecurity {

    /**
     * Build SQL WHERE clause for row-level security based on user context.
     * Returns additional WHERE conditions to append to the query.
     */
    public String buildSecurityFilter(Long tenantId, Long userId, String[] userRoles, String tableName) {
        List<String> conditions = new ArrayList<>();

        // Tenant isolation - always enforce
        conditions.add("tenant_id = " + tenantId);

        // Role-based row filtering
        for (String role : userRoles) {
            switch (role.toLowerCase()) {
                case "admin":
                    // Admin sees all rows within tenant - no additional filter
                    break;
                case "manager":
                    // Manager sees their department's data
                    conditions.add("(department_id IN (SELECT department_id FROM sys_user WHERE id = " + userId + ") OR created_by = " + userId + ")");
                    break;
                case "analyst":
                    // Analyst sees only non-sensitive data
                    conditions.add("sensitivity_level <= 2");
                    break;
                case "viewer":
                    // Viewer sees only published/approved data
                    conditions.add("status IN ('published', 'approved')");
                    conditions.add("created_by = " + userId + " OR is_public = 1");
                    break;
                default:
                    // Default: user sees only their own data
                    conditions.add("created_by = " + userId);
                    break;
            }
        }

        return conditions.isEmpty() ? "1=1" : String.join(" AND ", conditions);
    }

    /**
     * Check if a user has access to a specific row.
     */
    public boolean canAccess(Long tenantId, Long userId, String[] userRoles, 
                              Long rowTenantId, Long rowCreatedBy, String rowStatus) {
        // Must be same tenant
        if (!Objects.equals(tenantId, rowTenantId)) return false;

        // Admin can access everything in their tenant
        for (String role : userRoles) {
            if ("admin".equalsIgnoreCase(role)) return true;
        }

        // Owner can always access their own data
        if (Objects.equals(userId, rowCreatedBy)) return true;

        // Check role-specific access
        for (String role : userRoles) {
            switch (role.toLowerCase()) {
                case "manager":
                    return true; // Managers can access all tenant data
                case "analyst":
                    return "published".equals(rowStatus) || "approved".equals(rowStatus);
                case "viewer":
                    return "published".equals(rowStatus);
                default:
                    return false;
            }
        }

        return false;
    }

    /**
     * Get sensitivity level for a user role.
     * Higher number = more sensitive = more restricted.
     */
    public int getMaxSensitivityLevel(String role) {
        return switch (role.toLowerCase()) {
            case "admin" -> 5;
            case "manager" -> 4;
            case "analyst" -> 3;
            case "viewer" -> 1;
            default -> 0;
        };
    }
}
