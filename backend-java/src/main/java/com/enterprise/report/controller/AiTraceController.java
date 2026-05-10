package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.AiExecutionTrace;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.AiExecutionTraceMapper;
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
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<AiExecutionTrace> wrapper = new LambdaQueryWrapper<>();
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(AiExecutionTrace::getOperationType, operationType);
        }
        if (status != null) {
            wrapper.eq(AiExecutionTrace::getStatus, status);
        }
        wrapper.orderByDesc(AiExecutionTrace::getCreatedAt);
        Page<AiExecutionTrace> result = new Page<>(page, size);
        traceMapper.selectPage(result, wrapper);
        return ApiResponse.success(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiExecutionTrace> get(@PathVariable Long id) {
        AiExecutionTrace trace = traceMapper.selectById(id);
        if (trace == null) {
            throw new BusinessException(404, "AI trace not found");
        }
        return ApiResponse.success(trace);
    }
}
