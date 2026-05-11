package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Natural language alert rule.
 * Users define alerts in plain language, system converts to monitoring logic.
 */
@Data
@TableName("alert_rule")
public class AlertRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long userId;

    /** Rule name */
    private String name;
    /** Natural language rule description, e.g. "当销售额连续3天下降超过10%时通知我" */
    private String ruleExpression;
    /** Parsed monitoring config (JSON) */
    private String parsedConfig;
    /** Associated dataset ID */
    private Long datasetId;
    /** Associated KPI ID */
    private Long kpiId;

    /** Alert channel: email/webhook/inapp */
    private String notifyChannel;
    /** Notification recipients */
    private String recipients;
    /** Webhook URL */
    private String webhookUrl;

    /** Check frequency in minutes */
    private Integer checkFrequencyMin;
    /** Status: active/paused/triggered */
    private String status;
    /** Last check time */
    private LocalDateTime lastCheckAt;
    /** Last trigger time */
    private LocalDateTime lastTriggerAt;
    /** Trigger count */
    private Integer triggerCount;
    /** Last evaluation result */
    private String lastResult;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
