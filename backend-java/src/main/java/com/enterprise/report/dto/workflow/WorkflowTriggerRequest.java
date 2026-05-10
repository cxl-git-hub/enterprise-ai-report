package com.enterprise.report.dto.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class WorkflowTriggerRequest {
    @NotNull(message = "Workflow ID is required")
    private Long workflowId;

    private Map<String, Object> inputParams;
}
