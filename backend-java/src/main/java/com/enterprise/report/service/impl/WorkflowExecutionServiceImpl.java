package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.engine.workflow.DagExecutor;
import com.enterprise.report.entity.*;
import com.enterprise.report.enums.WorkflowState;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.*;
import com.enterprise.report.service.WorkflowExecutionService;
import com.enterprise.report.service.WorkflowService;
import com.enterprise.report.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionServiceImpl extends ServiceImpl<WorkflowRunMapper, WorkflowRun> implements WorkflowExecutionService {

    private final WorkflowService workflowService;
    private final WorkflowNodeRunMapper nodeRunMapper;
    private final WorkflowExecutionLogMapper executionLogMapper;
    private final WorkflowStateSnapshotMapper stateSnapshotMapper;
    private final DagExecutor dagExecutor;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public WorkflowRun trigger(Long workflowId, Map<String, Object> inputParams) {
        WorkflowDefinition workflow = workflowService.getById(workflowId);
        if (workflow == null) {
            throw new BusinessException(404, "Workflow not found");
        }

        WorkflowRun run = new WorkflowRun();
        run.setTenantId(workflow.getTenantId());
        run.setWorkflowId(workflowId);
        run.setWorkflowVersion(workflow.getVersion());
        run.setRunId(UUID.randomUUID().toString());
        run.setTriggerType("manual");
        run.setState(WorkflowState.PENDING);
        try {
            run.setInputParams(objectMapper.writeValueAsString(inputParams != null ? inputParams : new HashMap<>()));
        } catch (Exception e) {
            run.setInputParams("{}");
        }
        save(run);

        // Log trigger event
        logExecution(run.getRunId(), null, "INFO", "Workflow triggered", null);

        try {
            dagExecutor.execute(run, workflow);
        } catch (Exception e) {
            log.error("Workflow execution failed: {}", e.getMessage());
            run.setState(WorkflowState.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(LocalDateTime.now());
            updateById(run);
            logExecution(run.getRunId(), null, "ERROR", "Workflow failed: " + e.getMessage(), null);
        }

        // Send notifications
        try {
            String wfName = workflow.getName();
            if (run.getState() == WorkflowState.SUCCESS) {
                notificationService.notifyWorkflowComplete(run.getTenantId(), run.getTriggeredBy(), wfName, run.getId());
            } else if (run.getState() == WorkflowState.FAILED) {
                notificationService.notifyWorkflowFailed(run.getTenantId(), run.getTriggeredBy(), wfName, run.getId(), run.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send workflow notification: {}", e.getMessage());
        }

        return run;
    }

    @Override
    public WorkflowRun getRunDetail(Long runId) {
        WorkflowRun run = getById(runId);
        if (run == null) {
            throw new BusinessException(404, "Workflow run not found");
        }
        List<WorkflowNodeRun> nodeRuns = getNodeRunsByRunId(run.getRunId());
        run.setNodeRuns(nodeRuns);
        return run;
    }

    @Override
    @Transactional
    public WorkflowRun resume(Long runId) {
        WorkflowRun run = getById(runId);
        if (run == null) {
            throw new BusinessException(404, "Workflow run not found");
        }
        if (run.getState() != WorkflowState.FAILED && run.getState() != WorkflowState.PAUSED) {
            throw new BusinessException(400, "Can only resume failed or paused runs");
        }

        WorkflowDefinition workflow = workflowService.getById(run.getWorkflowId());
        if (workflow == null) {
            throw new BusinessException(404, "Workflow definition not found");
        }

        run.setState(WorkflowState.RUNNING);
        updateById(run);
        logExecution(run.getRunId(), null, "INFO", "Workflow resumed from failure", null);

        try {
            dagExecutor.resume(run, workflow);
        } catch (Exception e) {
            log.error("Workflow resume failed: {}", e.getMessage());
            run.setState(WorkflowState.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(LocalDateTime.now());
            updateById(run);
        }

        return run;
    }

    @Override
    public List<WorkflowNodeRun> getNodeRuns(Long runId) {
        WorkflowRun run = getById(runId);
        if (run == null) return List.of();
        return getNodeRunsByRunId(run.getRunId());
    }

    private List<WorkflowNodeRun> getNodeRunsByRunId(String runId) {
        return nodeRunMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeRun>()
                        .eq(WorkflowNodeRun::getRunId, runId)
                        .orderByAsc(WorkflowNodeRun::getStartTime));
    }

    private void logExecution(String runId, String nodeId, String level, String message, String contextData) {
        try {
            WorkflowExecutionLog logEntry = new WorkflowExecutionLog();
            logEntry.setRunId(runId);
            logEntry.setNodeId(nodeId);
            logEntry.setLogLevel(level);
            logEntry.setMessage(message);
            logEntry.setContextData(contextData);
            executionLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("Failed to write execution log: {}", e.getMessage());
        }
    }
}
