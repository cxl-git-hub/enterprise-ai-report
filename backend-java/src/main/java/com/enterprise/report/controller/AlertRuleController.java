package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.AlertRule;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.security.UserDetailsImpl;
import com.enterprise.report.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    public ApiResponse<PageResult<AlertRule>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getTenantId, tenantId);
        if (status != null) wrapper.eq(AlertRule::getStatus, status);
        wrapper.orderByDesc(AlertRule::getCreatedAt);
        return ApiResponse.success(PageResult.from(alertRuleService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AlertRule> get(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        AlertRule rule = alertRuleService.getById(id);
        if (rule == null || !rule.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Alert rule not found");
        }
        return ApiResponse.success(rule);
    }

    @PostMapping
    public ApiResponse<AlertRule> create(@RequestBody AlertRule rule,
                                         @AuthenticationPrincipal UserDetailsImpl user) {
        Long tenantId = TenantContext.getTenantId();
        rule.setTenantId(tenantId);
        rule.setUserId(user.getId());
        rule.setStatus("active");
        rule.setTriggerCount(0);

        // Parse NL expression into structured config
        if (rule.getRuleExpression() != null) {
            String parsed = alertRuleService.parseRuleExpression(rule.getRuleExpression());
            rule.setParsedConfig(parsed);
        }

        alertRuleService.save(rule);
        return ApiResponse.success(rule);
    }

    @PutMapping("/{id}")
    public ApiResponse<AlertRule> update(@PathVariable Long id, @RequestBody AlertRule rule) {
        Long tenantId = TenantContext.getTenantId();
        AlertRule existing = alertRuleService.getById(id);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Alert rule not found");
        }
        rule.setId(id);
        rule.setTenantId(tenantId);
        if (rule.getRuleExpression() != null && !rule.getRuleExpression().equals(existing.getRuleExpression())) {
            rule.setParsedConfig(alertRuleService.parseRuleExpression(rule.getRuleExpression()));
        }
        alertRuleService.updateById(rule);
        return ApiResponse.success(rule);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        AlertRule rule = alertRuleService.getById(id);
        if (rule == null || !rule.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Alert rule not found");
        }
        alertRuleService.removeById(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<Void> pause(@PathVariable Long id) {
        AlertRule rule = alertRuleService.getById(id);
        if (rule == null) throw new BusinessException(404, "Alert rule not found");
        rule.setStatus("paused");
        alertRuleService.updateById(rule);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        AlertRule rule = alertRuleService.getById(id);
        if (rule == null) throw new BusinessException(404, "Alert rule not found");
        rule.setStatus("active");
        alertRuleService.updateById(rule);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<String> test(@PathVariable Long id) {
        boolean triggered = alertRuleService.evaluateRule(id);
        return ApiResponse.success(triggered ? "规则已触发" : "规则未触发（当前数据正常）");
    }

    /**
     * Parse a natural language rule expression without saving.
     */
    @PostMapping("/parse")
    public ApiResponse<String> parse(@RequestBody String expression) {
        String parsed = alertRuleService.parseRuleExpression(expression);
        return ApiResponse.success(parsed);
    }
}
