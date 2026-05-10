package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.ConfigType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("config_dependency_graph")
public class ConfigDependencyGraph {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private ConfigType sourceType;
    private Long sourceId;
    private String sourceName;
    private ConfigType targetType;
    private Long targetId;
    private String targetName;
    private String dependencyType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
