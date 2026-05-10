package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.ConfigType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("config_version")
public class ConfigVersion {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private ConfigType configType;
    private Long configId;
    private Integer version;
    private String configData;
    private String changeDescription;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
