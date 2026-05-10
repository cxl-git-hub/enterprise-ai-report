package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.WorkflowState;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("workflow_node_run")
public class WorkflowNodeRun {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    @TableField("run_id")
    private String runId;
    @TableField("node_id")
    private String nodeId;
    @TableField("node_type")
    private String nodeType;
    @TableField("node_name")
    private String nodeName;
    private WorkflowState state;
    @TableField("input_data")
    private String inputData;
    @TableField("output_data")
    private String outputData;
    @TableField("error_message")
    private String errorMessage;
    @TableField("retry_count")
    private Integer retryCount;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("end_time")
    private LocalDateTime endTime;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("tokens_used")
    private Long tokensUsed;
    private BigDecimal cost;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
