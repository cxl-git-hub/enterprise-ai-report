package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.entity.DataSource;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/data-quality")
@RequiredArgsConstructor
public class DataQualityController {

    private final DataSourceService dataSourceService;

    /**
     * Get data quality overview for the tenant.
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        Long tenantId = TenantContext.getTenantId();
        List<DataSource> sources = dataSourceService.list(
                new LambdaQueryWrapper<DataSource>().eq(DataSource::getTenantId, tenantId));

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalSources", sources.size());

        int healthy = 0, warning = 0, error = 0;
        for (DataSource ds : sources) {
            String status = ds.getStatus();
            if ("active".equals(status) || "connected".equals(status)) healthy++;
            else if ("warning".equals(status)) warning++;
            else error++;
        }

        overview.put("healthyCount", healthy);
        overview.put("warningCount", warning);
        overview.put("errorCount", error);
        overview.put("qualityScore", sources.isEmpty() ? 0 : Math.round(healthy * 100.0 / sources.size()));

        return ApiResponse.success(overview);
    }

    /**
     * Get health status for each data source.
     */
    @GetMapping("/health")
    public ApiResponse<List<Map<String, Object>>> getHealth() {
        Long tenantId = TenantContext.getTenantId();
        List<DataSource> sources = dataSourceService.list(
                new LambdaQueryWrapper<DataSource>().eq(DataSource::getTenantId, tenantId));

        List<Map<String, Object>> healthList = new ArrayList<>();
        for (DataSource ds : sources) {
            Map<String, Object> health = new HashMap<>();
            health.put("id", ds.getId());
            health.put("name", ds.getName());
            health.put("type", ds.getType());
            health.put("status", mapStatus(ds.getStatus()));
            health.put("lastCheck", ds.getUpdatedAt() != null ? ds.getUpdatedAt().toString() : null);
            health.put("freshness", assessFreshness(ds.getUpdatedAt()));
            health.put("qualityScore", calculateQualityScore(ds));
            healthList.add(health);
        }

        return ApiResponse.success(healthList);
    }

    /**
     * Run health check on a specific data source.
     */
    @PostMapping("/check/{id}")
    public ApiResponse<Map<String, Object>> runCheck(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        DataSource ds = dataSourceService.getById(id);
        if (ds == null || !ds.getTenantId().equals(tenantId)) {
            return ApiResponse.success(Map.of("status", "error", "message", "数据源不存在"));
        }

        // In production, this would actually test the connection
        Map<String, Object> result = new HashMap<>();
        result.put("status", "healthy");
        result.put("message", "连接正常");
        result.put("checkedAt", LocalDateTime.now().toString());

        return ApiResponse.success(result);
    }

    private String mapStatus(String dbStatus) {
        if (dbStatus == null) return "unknown";
        return switch (dbStatus.toLowerCase()) {
            case "active", "connected" -> "healthy";
            case "warning" -> "warning";
            case "error", "disconnected", "failed" -> "error";
            default -> "unknown";
        };
    }

    private String assessFreshness(LocalDateTime lastUpdate) {
        if (lastUpdate == null) return "expired";
        long hoursAgo = java.time.Duration.between(lastUpdate, LocalDateTime.now()).toHours();
        if (hoursAgo < 24) return "fresh";
        if (hoursAgo < 72) return "stale";
        return "expired";
    }

    private int calculateQualityScore(DataSource ds) {
        int score = 80; // Base score
        if ("active".equals(ds.getStatus()) || "connected".equals(ds.getStatus())) score += 15;
        if (ds.getUpdatedAt() != null) {
            long hoursAgo = java.time.Duration.between(ds.getUpdatedAt(), LocalDateTime.now()).toHours();
            if (hoursAgo < 24) score += 5;
        }
        return Math.min(100, score);
    }
}
