package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.WorkflowState;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("workflow_definition")
public class WorkflowDefinition {
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
