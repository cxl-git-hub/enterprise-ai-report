package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("workflow_execution_log")
public class WorkflowExecutionLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("run_id")
    private String runId;
    @TableField("node_id")
    private String nodeId;
    @TableField("log_level")
    private String logLevel;
    private String message;
    @TableField("context_data")
    private String contextData;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
