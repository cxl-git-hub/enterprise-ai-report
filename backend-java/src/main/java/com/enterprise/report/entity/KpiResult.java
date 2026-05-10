package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kpi_result")
public class KpiResult {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long kpiId;
    private Long workflowRunId;
    private BigDecimal value;
    private String formattedValue;
    private String periodStart;
    private String periodEnd;
    private String dimensions;
    private String metadata;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
