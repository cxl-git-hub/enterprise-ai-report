package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.schema.SchemaCreateRequest;
import com.enterprise.report.dto.schema.SchemaResponse;
import com.enterprise.report.entity.Dataset;
import com.enterprise.report.entity.SchemaDefinition;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.DatasetService;
import com.enterprise.report.service.SchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaService schemaService;
    private final DatasetService datasetService;

    @GetMapping
    public ApiResponse<PageResult<SchemaDefinition>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SchemaDefinition> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SchemaDefinition::getName, keyword);
        }
        wrapper.orderByDesc(SchemaDefinition::getCreatedAt);
        Page<SchemaDefinition> result = schemaService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::populateDatasetName);
        return ApiResponse.success(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<SchemaDefinition> get(@PathVariable Long id) {
        SchemaDefinition schema = schemaService.getById(id);
        if (schema == null) {
            throw new BusinessException(404, "Schema not found");
        }
        populateDatasetName(schema);
        return ApiResponse.success(schema);
    }

    @PostMapping
    public ApiResponse<SchemaDefinition> create(@RequestBody SchemaCreateRequest request) {
        SchemaDefinition schema = new SchemaDefinition();
        schema.setTenantId(TenantContext.getTenantId());
        schema.setName(request.getName());
        schema.setDescription(request.getDescription());
        schema.setDatasetId(request.getDatasetId());
        schema.setColumns(request.getColumns());
        schema.setMetrics(request.getMetrics());
        schema.setDimensions(request.getDimensions());
        schema.setConfig(request.getConfig());
        schemaService.save(schema);
        return ApiResponse.success(schema);
    }

    @PutMapping("/{id}")
    public ApiResponse<SchemaDefinition> update(@PathVariable Long id, @RequestBody SchemaDefinition schema) {
        schema.setId(id);
        schemaService.save(schema);
        return ApiResponse.success(schema);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        schemaService.removeById(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<SchemaDefinition>> getVersions(@PathVariable Long id) {
        return ApiResponse.success(schemaService.getVersions(id));
    }

    @PostMapping("/{id}/activate-version/{version}")
    public ApiResponse<SchemaDefinition> activateVersion(@PathVariable Long id, @PathVariable Integer version) {
        return ApiResponse.success(schemaService.activateVersion(id, version));
    }

    private void populateDatasetName(SchemaDefinition schema) {
        if (schema.getDatasetId() != null) {
            try {
                Dataset dataset = datasetService.getById(schema.getDatasetId());
                schema.setDatasetName(dataset != null ? dataset.getName() : "");
            } catch (Exception e) {
                schema.setDatasetName("");
            }
        }
    }
}
