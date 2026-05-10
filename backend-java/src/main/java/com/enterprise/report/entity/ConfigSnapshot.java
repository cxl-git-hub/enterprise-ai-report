package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("config_snapshot")
public class ConfigSnapshot {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    @TableField("snapshot_name")
    private String snapshotName;
    @TableField("snapshot_version")
    private String snapshotVersion;
    private String description;
    @TableField("full_snapshot")
    private String fullSnapshot;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
