package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.report.enums.ReportFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Scheduled report configuration.
 * Defines a report that should be generated and distributed on a schedule.
 */
@Data
@TableName("report_schedule")
public class ReportSchedule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    
    /** Schedule name */
    private String name;
    /** Description */
    private String description;
    /** Associated report template ID */
    private Long reportTemplateId;
    /** Output format */
    private ReportFormat format;
    
    /** Cron expression for scheduling */
    private String cronExpression;
    /** Timezone for schedule */
    private String timezone;
    
    /** Comma-separated recipient email addresses */
    private String recipients;
    /** CC email addresses */
    private String ccRecipients;
    /** Email subject template */
    private String emailSubject;
    /** Email body template */
    private String emailBody;
    
    /** Whether to include AI disclaimer */
    private Boolean includeDisclaimer;
    /** Whether to include data lineage */
    private Boolean includeLineage;
    
    /** Schedule status: active/paused/error */
    private String status;
    /** Last execution time */
    private LocalDateTime lastRunAt;
    /** Next scheduled execution time */
    private LocalDateTime nextRunAt;
    /** Last error message */
    private String lastError;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
