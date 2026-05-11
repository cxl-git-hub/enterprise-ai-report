package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.safety.DataMasker;
import com.enterprise.report.safety.RowLevelSecurity;
import com.enterprise.report.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/data-security")
@RequiredArgsConstructor
public class DataSecurityController {

    private final DataMasker dataMasker;
    private final RowLevelSecurity rowLevelSecurity;

    /**
     * Mask a single value.
     */
    @PostMapping("/mask")
    public ApiResponse<Map<String, String>> maskValue(@RequestBody MaskRequest request) {
        String masked = dataMasker.mask(request.getValue(), request.getFieldType());
        Map<String, String> result = new HashMap<>();
        result.put("original", request.getValue());
        result.put("masked", masked);
        result.put("type", request.getFieldType());
        return ApiResponse.success(result);
    }

    /**
     * Auto-detect and mask sensitive data.
     */
    @PostMapping("/auto-mask")
    public ApiResponse<Map<String, String>> autoMask(@RequestBody Map<String, String> values) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result.put(entry.getKey(), dataMasker.autoMask(entry.getValue()));
        }
        return ApiResponse.success(result);
    }

    /**
     * Get row-level security filter for a user.
     */
    @GetMapping("/rls-filter")
    public ApiResponse<Map<String, Object>> getRlsFilter(
            @RequestParam String tableName,
            @RequestParam(required = false) String[] roles) {
        Long tenantId = TenantContext.getTenantId();
        String[] userRoles = roles != null ? roles : new String[]{"viewer"};
        
        String filter = rowLevelSecurity.buildSecurityFilter(tenantId, tenantId, userRoles, tableName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("filter", filter);
        result.put("tenantId", tenantId);
        result.put("tableName", tableName);
        return ApiResponse.success(result);
    }

    /**
     * Check if current user can access a specific row.
     */
    @PostMapping("/check-access")
    public ApiResponse<Map<String, Object>> checkAccess(@RequestBody AccessCheckRequest request) {
        Long tenantId = TenantContext.getTenantId();
        String[] userRoles = request.getRoles() != null ? request.getRoles() : new String[]{"viewer"};
        
        boolean canAccess = rowLevelSecurity.canAccess(
                tenantId, tenantId, userRoles,
                request.getRowTenantId(), request.getRowCreatedBy(), request.getRowStatus()
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("canAccess", canAccess);
        return ApiResponse.success(result);
    }

    public static class MaskRequest {
        private String value;
        private String fieldType;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getFieldType() { return fieldType; }
        public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    }

    public static class AccessCheckRequest {
        private Long rowTenantId;
        private Long rowCreatedBy;
        private String rowStatus;
        private String[] roles;
        public Long getRowTenantId() { return rowTenantId; }
        public void setRowTenantId(Long rowTenantId) { this.rowTenantId = rowTenantId; }
        public Long getRowCreatedBy() { return rowCreatedBy; }
        public void setRowCreatedBy(Long rowCreatedBy) { this.rowCreatedBy = rowCreatedBy; }
        public String getRowStatus() { return rowStatus; }
        public void setRowStatus(String rowStatus) { this.rowStatus = rowStatus; }
        public String[] getRoles() { return roles; }
        public void setRoles(String[] roles) { this.roles = roles; }
    }
}
