package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.AuditLog;
import com.enterprise.report.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping
    public ApiResponse<PageResult<AuditLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(AuditLog::getResourceType, resourceType);
        }
        if (userId != null) {
            wrapper.eq(AuditLog::getUserId, userId);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        Page<AuditLog> result = new Page<>(page, size);
        auditLogMapper.selectPage(result, wrapper);
        return ApiResponse.success(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditLog> get(@PathVariable Long id) {
        return ApiResponse.success(auditLogMapper.selectById(id));
    }
}
