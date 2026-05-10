package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.AiExecutionTrace;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.AiExecutionTraceMapper;
import com.enterprise.report.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-traces")
@RequiredArgsConstructor
public class AiTraceController {

    private final AiExecutionTraceMapper traceMapper;

    @GetMapping
    public ApiResponse<PageResult<AiExecutionTrace>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String aiTaskType,
            @RequestParam(required = false) String status) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<AiExecutionTrace> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiExecutionTrace::getTenantId, tenantId);
        if (aiTaskType != null && !aiTaskType.isEmpty()) {
            wrapper.eq(AiExecutionTrace::getAiTaskType, aiTaskType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AiExecutionTrace::getStatus, status);
        }
        wrapper.orderByDesc(AiExecutionTrace::getCreatedAt);
        Page<AiExecutionTrace> result = new Page<>(page, size);
        traceMapper.selectPage(result, wrapper);
        return ApiResponse.success(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiExecutionTrace> get(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        AiExecutionTrace trace = traceMapper.selectById(id);
        if (trace == null || !trace.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "AI trace not found");
        }
        return ApiResponse.success(trace);
    }
}
