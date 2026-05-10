package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_policy")
public class AiPolicy {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private Boolean allowSqlGeneration;
    private Boolean allowCrossDatasetJoin;
    private Boolean allowDataModification;
    private Integer maxRowsReturned;
    private Integer maxExecutionTime;
    private String allowedDatasets;
    private String blockedTables;
    private String config;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
