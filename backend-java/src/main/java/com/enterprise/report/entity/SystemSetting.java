package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_setting")
public class SystemSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String settingGroup;
    private String settingKey;
    private String settingValue;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
