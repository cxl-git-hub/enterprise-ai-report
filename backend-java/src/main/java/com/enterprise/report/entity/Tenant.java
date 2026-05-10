package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class Tenant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String code;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String planType;
    private Integer status;
    private Integer maxUsers;
    private Integer maxDatasources;
    private Integer maxDatasets;
    private Integer maxAiCallsPerDay;
    private LocalDateTime expireTime;
    private String config;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
