package com.enterprise.report.dto.workflow;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowRunResponse {
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private String workflowName;
    private Integer workflowVersion;
    private String runId;
    private String triggerType;
    private Long triggeredBy;
    private String status;           // 前端期望 status 而非 state
    private String currentNodeId;
    private String inputParams;
    private String outputResult;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long duration;
    private Long totalTokens;
    private BigDecimal totalCost;
    private LocalDateTime createdAt;
    private List<WorkflowNodeRunResponse> nodeRuns;
    private List<StateSnapshotResponse> stateSnapshots;

    @Data
    public static class StateSnapshotResponse {
        private String state;
        private String timestamp;
        private String currentNodeId;
        private String completedNodes;
        private String failedNodes;
    }
}
