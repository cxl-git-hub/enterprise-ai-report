package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.WorkflowState;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("workflow_run")
public class WorkflowRun {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private Integer workflowVersion;
    @TableField("run_id")
    private String runId;
    private String triggerType;
    private Long triggeredBy;
    private WorkflowState state;
    @TableField("current_node_id")
    private String currentNodeId;
    @TableField("input_params")
    private String inputParams;
    @TableField("output_result")
    private String outputResult;
    @TableField("error_message")
    private String errorMessage;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("end_time")
    private LocalDateTime endTime;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("total_tokens")
    private Long totalTokens;
    @TableField("total_cost")
    private java.math.BigDecimal totalCost;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 非DB字段 - 运行时填充
    @TableField(exist = false)
    private java.util.List<WorkflowNodeRun> nodeRuns;
}
