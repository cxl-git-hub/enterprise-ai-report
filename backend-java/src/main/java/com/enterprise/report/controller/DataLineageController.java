package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.entity.DataLineage;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.DataLineageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data-lineage")
@RequiredArgsConstructor
public class DataLineageController {

    private final DataLineageService dataLineageService;

    /**
     * Get lineage for a specific AI output (report, analysis, etc.)
     */
    @GetMapping("/output/{refType}/{refId}")
    public ApiResponse<List<DataLineage>> getLineageForOutput(
            @PathVariable String refType,
            @PathVariable String refId) {
        return ApiResponse.success(dataLineageService.getLineageForOutput(refType, refId));
    }

    /**
     * Get all lineage records for a dataset (reverse impact analysis).
     */
    @GetMapping("/dataset/{datasetId}")
    public ApiResponse<List<DataLineage>> getLineageForDataset(@PathVariable Long datasetId) {
        return ApiResponse.success(dataLineageService.getLineageForDataset(datasetId));
    }

    /**
     * Get recent lineage records for the tenant.
     */
    @GetMapping("/recent")
    public ApiResponse<List<DataLineage>> getRecentLineage(
            @RequestParam(defaultValue = "20") Integer limit) {
        Long tenantId = TenantContext.getTenantId();
        List<DataLineage> records = dataLineageService.list(
                new LambdaQueryWrapper<DataLineage>()
                        .eq(DataLineage::getTenantId, tenantId)
                        .orderByDesc(DataLineage::getCreatedAt)
                        .last("LIMIT " + limit));
        return ApiResponse.success(records);
    }
}
