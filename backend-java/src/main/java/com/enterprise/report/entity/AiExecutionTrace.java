package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_execution_trace")
public class AiExecutionTrace {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String operationType;
    private String inputPrompt;
    private String generatedSql;
    private String aiModel;
    private String aiResponse;
    private Integer tokenCount;
    private Long durationMs;
    private Integer status;
    private String errorMessage;
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
