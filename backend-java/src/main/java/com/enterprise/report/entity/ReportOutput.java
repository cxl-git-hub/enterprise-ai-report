package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.ReportFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report_output")
public class ReportOutput {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long workflowRunId;
    private Long reportTemplateId;
    private String name;
    private ReportFormat format;
    private String fileKey;
    private String fileName;
    private Long fileSize;
    private String filePath;
    private Integer status;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
