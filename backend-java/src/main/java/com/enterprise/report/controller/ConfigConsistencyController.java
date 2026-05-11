package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.config.*;
import com.enterprise.report.entity.ConfigSnapshot;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.ConfigConsistencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
            @RequestParam(defaultValue = "20") Integer size) {
        Long tenantId = TenantContext.getTenantId();
        return ApiResponse.success(configConsistencyService.getSnapshots(tenantId, page, size));
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

    /**
     * Export tenant configuration as JSON file
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportConfig() {
        Long tenantId = TenantContext.getTenantId();
        ConfigSnapshot snapshot = configConsistencyService.createSnapshot(tenantId, "export_" + System.currentTimeMillis(), "Auto-export");

        byte[] data = snapshot.getFullSnapshot().getBytes(StandardCharsets.UTF_8);
        String filename = "config_export_" + tenantId + "_" + System.currentTimeMillis() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    /**
     * Import tenant configuration from JSON file
     */
    @PostMapping("/import")
    public ApiResponse<Void> importConfig(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) Boolean merge) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "File is empty");
        }

        Long tenantId = TenantContext.getTenantId();
        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            // Create a snapshot from the imported config, then rollback to it
            ConfigSnapshot snapshot = configConsistencyService.createSnapshot(tenantId, "import_" + System.currentTimeMillis(), "Imported from file");
            // The snapshot creation captures current state; for import we need to directly restore
            // Parse and validate the JSON structure
            configConsistencyService.importConfig(tenantId, json, Boolean.TRUE.equals(merge));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to import config: " + e.getMessage());
        }
        return ApiResponse.success();
    }
}
