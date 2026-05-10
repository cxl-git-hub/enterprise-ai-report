package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.config.*;
import com.enterprise.report.entity.ConfigSnapshot;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.ConfigConsistencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigConsistencyController {

    private final ConfigConsistencyService configConsistencyService;

    /**
     * 一致性校验 - 返回结构化结果（前端期望 POST）
     */
    @PostMapping("/validate")
    public ApiResponse<ValidationResult> validate() {
        Long tenantId = TenantContext.getTenantId();
        return ApiResponse.success(configConsistencyService.validateStructured(tenantId));
    }

    /**
     * 依赖关系图 - 前端可视化用
     */
    @GetMapping("/dependency-graph")
    public ApiResponse<DependencyGraph> getDependencyGraph() {
        Long tenantId = TenantContext.getTenantId();
        return ApiResponse.success(configConsistencyService.getDependencyGraph(tenantId));
    }

    /**
     * 创建快照（前端调用 POST /config/snapshots）
     */
    @PostMapping("/snapshots")
    public ApiResponse<ConfigSnapshot> createSnapshot(@RequestBody CreateSnapshotRequest request) {
        Long tenantId = TenantContext.getTenantId();
        return ApiResponse.success(configConsistencyService.createSnapshot(tenantId, request.getName(), request.getDescription()));
    }

    /**
     * 快照列表（分页）
     */
    @GetMapping("/snapshots")
    public ApiResponse<PageResult<ConfigSnapshot>> listSnapshots(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = TenantContext.getTenantId();
        return ApiResponse.success(configConsistencyService.getSnapshots(tenantId, page, pageSize));
    }

    /**
     * 快照详情
     */
    @GetMapping("/snapshots/{id}")
    public ApiResponse<ConfigSnapshot> getSnapshot(@PathVariable Long id) {
        return ApiResponse.success(configConsistencyService.getSnapshot(id));
    }

    /**
     * 恢复快照（前端调用 POST /config/snapshots/{id}/restore）
     */
    @PostMapping("/snapshots/{id}/restore")
    public ApiResponse<Void> restoreSnapshot(@PathVariable Long id) {
        configConsistencyService.rollback(id);
        return ApiResponse.success();
    }

    /**
     * 快照对比
     */
    @GetMapping("/snapshots/diff")
    public ApiResponse<SnapshotDiff> diffSnapshots(
            @RequestParam Long id1,
            @RequestParam Long id2) {
        return ApiResponse.success(configConsistencyService.diffSnapshots(id1, id2));
    }
}
