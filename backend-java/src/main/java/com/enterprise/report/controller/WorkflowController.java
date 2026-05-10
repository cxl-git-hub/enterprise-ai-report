package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.workflow.WorkflowCreateRequest;
import com.enterprise.report.dto.workflow.WorkflowTriggerRequest;
import com.enterprise.report.entity.WorkflowDefinition;
import com.enterprise.report.entity.WorkflowRun;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.WorkflowExecutionService;
import com.enterprise.report.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService executionService;

    @GetMapping
    public ApiResponse<PageResult<WorkflowDefinition>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(WorkflowDefinition::getName, keyword);
        }
        wrapper.orderByDesc(WorkflowDefinition::getCreatedAt);
        return ApiResponse.success(PageResult.from(workflowService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkflowDefinition> get(@PathVariable Long id) {
        WorkflowDefinition workflow = workflowService.getById(id);
        if (workflow == null) {
            throw new BusinessException(404, "Workflow not found");
        }
        return ApiResponse.success(workflow);
    }

    @PostMapping
    public ApiResponse<WorkflowDefinition> create(@RequestBody WorkflowCreateRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setTenantId(TenantContext.getTenantId());
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setDagDefinition(request.getDagDefinition());
        workflow.setTriggerType(request.getTriggerType());
        workflow.setCronExpression(request.getCronExpression());
        workflow.setConfig(request.getConfig());
        return ApiResponse.success(workflowService.createWithValidation(workflow));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkflowDefinition> update(@PathVariable Long id, @RequestBody WorkflowCreateRequest request) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setDagDefinition(request.getDagDefinition());
        workflow.setTriggerType(request.getTriggerType());
        workflow.setCronExpression(request.getCronExpression());
        workflow.setConfig(request.getConfig());
        return ApiResponse.success(workflowService.updateWithValidation(id, workflow));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        WorkflowDefinition workflow = workflowService.getById(id);
        if (workflow == null || !workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Workflow not found");
        }
        workflowService.removeById(id);
        return ApiResponse.success();
    }

    @PostMapping("/trigger")
    public ApiResponse<WorkflowRun> trigger(@Valid @RequestBody WorkflowTriggerRequest request) {
        return ApiResponse.success(executionService.trigger(request.getWorkflowId(), request.getInputParams()));
    }
}
