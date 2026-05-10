package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.DataSourceType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("data_source")
public class DataSource {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String name;
    private DataSourceType type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String encryptedPassword;
    private String connectionUrl;
    private String config;
    private Integer status;
    private LocalDateTime lastTestAt;
    private String lastTestResult;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
