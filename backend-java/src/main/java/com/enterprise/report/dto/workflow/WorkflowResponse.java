package com.enterprise.report.dto.workflow;

import com.enterprise.report.enums.WorkflowState;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkflowResponse {
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private String dagDefinition;
    private String triggerType;
    private String cronExpression;
    private String config;
    private Integer version;
    private WorkflowState state;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
