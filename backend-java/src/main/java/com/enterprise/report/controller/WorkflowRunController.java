package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.workflow.WorkflowRunResponse;
import com.enterprise.report.dto.workflow.WorkflowNodeRunResponse;
import com.enterprise.report.entity.WorkflowRun;
import com.enterprise.report.entity.WorkflowNodeRun;
import com.enterprise.report.entity.WorkflowStateSnapshot;
import com.enterprise.report.entity.WorkflowDefinition;
import com.enterprise.report.enums.WorkflowState;
import com.enterprise.report.mapper.WorkflowStateSnapshotMapper;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.WorkflowExecutionService;
import com.enterprise.report.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflow-runs")
@RequiredArgsConstructor
public class WorkflowRunController {

    private final WorkflowExecutionService executionService;
    private final WorkflowService workflowService;
    private final WorkflowStateSnapshotMapper snapshotMapper;

    @GetMapping
    public ApiResponse<PageResult<WorkflowRunResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) String status) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<WorkflowRun> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowRun::getTenantId, tenantId);
        if (workflowId != null) {
            wrapper.eq(WorkflowRun::getWorkflowId, workflowId);
        }
        if (status != null && !status.isEmpty()) {
            // Convert lowercase frontend status to uppercase enum value
            try {
                WorkflowState state = WorkflowState.valueOf(status.toUpperCase());
                wrapper.eq(WorkflowRun::getState, state);
            } catch (IllegalArgumentException e) {
                // Invalid status value, ignore filter
            }
        }
        wrapper.orderByDesc(WorkflowRun::getCreatedAt);
        Page<WorkflowRun> result = executionService.page(new Page<>(page, size), wrapper);

        PageResult<WorkflowRunResponse> pageResult = new PageResult<>();
        pageResult.setItems(result.getRecords().stream().map(this::toRunResponse).collect(Collectors.toList()));
        pageResult.setTotal(result.getTotal());
        return ApiResponse.success(pageResult);
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkflowRunResponse> get(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        WorkflowRun run = executionService.getRunDetail(id);
        if (!run.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Workflow run not found");
        }
        WorkflowRunResponse response = toRunResponse(run);

        // Load state snapshots
        List<WorkflowStateSnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<WorkflowStateSnapshot>()
                        .eq(WorkflowStateSnapshot::getWorkflowRunId, id)
                        .orderByAsc(WorkflowStateSnapshot::getCreatedAt));
        response.setStateSnapshots(snapshots.stream().map(s -> {
            WorkflowRunResponse.StateSnapshotResponse snap = new WorkflowRunResponse.StateSnapshotResponse();
            snap.setState(s.getCurrentNodeId());
            snap.setTimestamp(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            snap.setCurrentNodeId(s.getCurrentNodeId());
            snap.setCompletedNodes(s.getCompletedNodes());
            snap.setFailedNodes(s.getFailedNodes());
            return snap;
        }).collect(Collectors.toList()));

        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<WorkflowRunResponse> resume(@PathVariable Long id) {
        WorkflowRun run = executionService.resume(id);
        return ApiResponse.success(toRunResponse(run));
    }

    @GetMapping("/{id}/nodes")
    public ApiResponse<List<WorkflowNodeRunResponse>> getNodeRuns(@PathVariable Long id) {
        List<WorkflowNodeRun> nodeRuns = executionService.getNodeRuns(id);
        return ApiResponse.success(nodeRuns.stream()
                .map(this::toNodeRunResponse)
                .collect(Collectors.toList()));
    }

    private WorkflowRunResponse toRunResponse(WorkflowRun run) {
        WorkflowRunResponse response = new WorkflowRunResponse();
        response.setId(run.getId());
        response.setTenantId(run.getTenantId());
        response.setWorkflowId(run.getWorkflowId());
        response.setWorkflowVersion(run.getWorkflowVersion());
        response.setRunId(run.getRunId());
        response.setTriggerType(run.getTriggerType());
        response.setTriggeredBy(run.getTriggeredBy());
        response.setStatus(run.getState() != null ? run.getState().name().toLowerCase() : "pending");
        response.setCurrentNodeId(run.getCurrentNodeId());
        response.setInputParams(run.getInputParams());
        response.setOutputResult(run.getOutputResult());
        response.setErrorMessage(run.getErrorMessage());
        response.setStartedAt(run.getStartTime());
        response.setFinishedAt(run.getEndTime());
        response.setDuration(run.getDurationMs());
        response.setTotalTokens(run.getTotalTokens());
        response.setTotalCost(run.getTotalCost());
        response.setCreatedAt(run.getCreatedAt());

        // Look up workflow name
        if (run.getWorkflowId() != null) {
            try {
                WorkflowDefinition wf = workflowService.getById(run.getWorkflowId());
                response.setWorkflowName(wf != null ? wf.getName() : "工作流 #" + run.getWorkflowId());
            } catch (Exception e) {
                response.setWorkflowName("工作流 #" + run.getWorkflowId());
            }
        }

        if (run.getNodeRuns() != null) {
            response.setNodeRuns(run.getNodeRuns().stream()
                    .map(this::toNodeRunResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private WorkflowNodeRunResponse toNodeRunResponse(WorkflowNodeRun nodeRun) {
        WorkflowNodeRunResponse response = new WorkflowNodeRunResponse();
        response.setId(nodeRun.getId());
        response.setRunId(nodeRun.getRunId());
        response.setNodeId(nodeRun.getNodeId());
        response.setNodeName(nodeRun.getNodeName());
        response.setNodeType(nodeRun.getNodeType());
        response.setStatus(nodeRun.getState() != null ? nodeRun.getState().name().toLowerCase() : "pending");
        response.setInput(nodeRun.getInputData());
        response.setOutput(nodeRun.getOutputData());
        response.setError(nodeRun.getErrorMessage());
        response.setRetryCount(nodeRun.getRetryCount());
        response.setStartedAt(nodeRun.getStartTime());
        response.setFinishedAt(nodeRun.getEndTime());
        response.setDuration(nodeRun.getDurationMs());
        response.setTokensUsed(nodeRun.getTokensUsed());
        response.setCost(nodeRun.getCost());
        return response;
    }
}
