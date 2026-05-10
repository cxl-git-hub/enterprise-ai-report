package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("workflow_state_snapshot")
public class WorkflowStateSnapshot {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long workflowRunId;
    private String snapshotData;
    private String currentNodeId;
    private String completedNodes;
    private String failedNodes;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
