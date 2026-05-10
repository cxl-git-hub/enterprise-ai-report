package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.dataset.DatasetCreateRequest;
import com.enterprise.report.dto.dataset.DatasetResponse;
import com.enterprise.report.entity.Dataset;
import com.enterprise.report.entity.DatasetColumn;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @GetMapping
    public ApiResponse<PageResult<Dataset>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Dataset> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Dataset::getName, keyword)
                    .or().like(Dataset::getTableName, keyword);
        }
        wrapper.orderByDesc(Dataset::getCreatedAt);
        return ApiResponse.success(PageResult.from(datasetService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<DatasetResponse> get(@PathVariable Long id) {
        Dataset dataset = datasetService.getById(id);
        if (dataset == null) {
            throw new BusinessException(404, "Dataset not found");
        }
        DatasetResponse response = new DatasetResponse();
        response.setId(dataset.getId());
        response.setTenantId(dataset.getTenantId());
        response.setDataSourceId(dataset.getDataSourceId());
        response.setName(dataset.getName());
        response.setDescription(dataset.getDescription());
        response.setTableName(dataset.getTableName());
        response.setQuerySql(dataset.getQuerySql());
        response.setConfig(dataset.getConfig());
        response.setStatus(dataset.getStatus());
        response.setLastSyncAt(dataset.getLastSyncAt());
        response.setRowCount(dataset.getRowCount());
        response.setCreatedAt(dataset.getCreatedAt());
        response.setColumns(datasetService.getColumns(id));
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<Dataset> create(@RequestBody DatasetCreateRequest request) {
        Dataset dataset = new Dataset();
        dataset.setTenantId(TenantContext.getTenantId());
        dataset.setDataSourceId(request.getDataSourceId());
        dataset.setName(request.getName());
        dataset.setDescription(request.getDescription());
        dataset.setTableName(request.getTableName());
        dataset.setQuerySql(request.getQuerySql());
        dataset.setConfig(request.getConfig());
        dataset.setStatus(1);
        datasetService.save(dataset);
        return ApiResponse.success(dataset);
    }

    @PutMapping("/{id}")
    public ApiResponse<Dataset> update(@PathVariable Long id, @RequestBody Dataset dataset) {
        dataset.setId(id);
        datasetService.updateById(dataset);
        return ApiResponse.success(dataset);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        datasetService.removeById(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/columns")
    public ApiResponse<List<DatasetColumn>> getColumns(@PathVariable Long id) {
        return ApiResponse.success(datasetService.getColumns(id));
    }

    @PostMapping("/{id}/sync-columns")
    public ApiResponse<Void> syncColumns(@PathVariable Long id) {
        datasetService.syncColumns(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<Map<String, Object>> preview(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "100") Integer limit) {
        return ApiResponse.success(datasetService.preview(id, limit));
    }
}
