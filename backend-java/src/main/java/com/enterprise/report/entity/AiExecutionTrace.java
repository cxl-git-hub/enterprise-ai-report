package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_execution_trace")
public class AiExecutionTrace {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String traceId;
    private String runId;
    private String nodeId;
    private String aiTaskType;
    private String inputPrompt;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String modelName;
    private String modelConfig;
    private String rawOutput;
    private String validatedOutput;
    private Integer validationPassed;
    private String validationErrors;
    private Integer retryCount;
    private Long latencyMs;
    private BigDecimal cost;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
