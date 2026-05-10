package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.ReportFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report_template")
public class ReportTemplate {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private ReportFormat format;
    private String templateFile;
    private String variables;
    private String config;
    private Integer version;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
