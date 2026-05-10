package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.kpi.KpiCreateRequest;
import com.enterprise.report.dto.kpi.KpiExecuteRequest;
import com.enterprise.report.dto.kpi.KpiResponse;
import com.enterprise.report.entity.KpiDefinition;
import com.enterprise.report.entity.KpiResult;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.KpiResultMapper;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.KpiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;
    private final KpiResultMapper kpiResultMapper;

    @GetMapping
    public ApiResponse<PageResult<KpiDefinition>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<KpiDefinition> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(KpiDefinition::getName, keyword);
        }
        wrapper.orderByDesc(KpiDefinition::getCreatedAt);
        return ApiResponse.success(PageResult.from(kpiService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<KpiDefinition> get(@PathVariable Long id) {
        KpiDefinition kpi = kpiService.getById(id);
        if (kpi == null) {
            throw new BusinessException(404, "KPI not found");
        }
        return ApiResponse.success(kpi);
    }

    @PostMapping
    public ApiResponse<KpiDefinition> create(@RequestBody KpiCreateRequest request) {
        KpiDefinition kpi = new KpiDefinition();
        kpi.setTenantId(TenantContext.getTenantId());
        kpi.setName(request.getName());
        kpi.setDescription(request.getDescription());
        kpi.setSchemaId(request.getSchemaId());
        kpi.setDatasetId(request.getDatasetId());
        kpi.setExpression(request.getExpression());
        kpi.setUnit(request.getUnit());
        kpi.setAggregationType(request.getAggregationType());
        kpi.setFilterCondition(request.getFilterCondition());
        kpi.setGroupBy(request.getGroupBy());
        kpi.setConfig(request.getConfig());
        kpi.setVersion(1);
        kpi.setStatus("active");
        kpiService.save(kpi);
        return ApiResponse.success(kpi);
    }

    @PutMapping("/{id}")
    public ApiResponse<KpiDefinition> update(@PathVariable Long id, @RequestBody KpiDefinition kpi) {
        kpi.setId(id);
        kpiService.updateById(kpi);
        return ApiResponse.success(kpi);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        KpiDefinition kpi = kpiService.getById(id);
        if (kpi == null || !kpi.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "KPI not found");
        }
        kpiService.removeById(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<BigDecimal> execute(@PathVariable Long id, @RequestBody(required = false) KpiExecuteRequest request) {
        if (request == null) {
            request = new KpiExecuteRequest();
        }
        request.setKpiId(id);
        return ApiResponse.success(kpiService.executeKpi(request));
    }

    @GetMapping("/{id}/trend")
    public ApiResponse<List<Map<String, Object>>> getTrend(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "30") Integer limit) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<KpiResult> wrapper = new LambdaQueryWrapper<KpiResult>()
                .eq(KpiResult::getKpiId, id)
                .eq(KpiResult::getTenantId, tenantId)
                .eq(KpiResult::getStatus, 1);
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(KpiResult::getPeriodStart, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(KpiResult::getPeriodEnd, endDate);
        }
        wrapper.orderByDesc(KpiResult::getCreatedAt)
                .last("LIMIT " + limit);

        List<KpiResult> results = kpiResultMapper.selectList(wrapper);
        // Reverse to chronological order
        Collections.reverse(results);

        List<Map<String, Object>> trend = results.stream().map(r -> {
            Map<String, Object> point = new HashMap<>();
            point.put("date", r.getPeriodStart());
            point.put("value", r.getValue());
            point.put("formattedValue", r.getFormattedValue());
            point.put("executedAt", r.getCreatedAt());
            return point;
        }).collect(Collectors.toList());

        return ApiResponse.success(trend);
    }
}
