package com.enterprise.report.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowCreateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "DAG definition is required")
    private String dagDefinition;

    private String triggerType;
    private String cronExpression;
    private String config;
}
