package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Tracks data lineage: which data sources/fields were used
 * in AI-generated outputs (reports, analyses, SQL queries).
 */
@Data
@TableName("data_lineage")
public class DataLineage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    
    /** Reference type: report_output, analysis, nl2sql, suggestion */
    private String refType;
    /** Reference ID (e.g., report output ID, trace ID) */
    private String refId;
    
    /** Source dataset ID */
    private Long datasetId;
    /** Source schema ID */
    private Long schemaId;
    /** Source name (human-readable) */
    private String sourceName;
    /** Comma-separated field names used */
    private String fields;
    /** Time range of data used */
    private String timeRange;
    /** Number of rows involved */
    private Long rowCount;
    /** Data quality assessment */
    private String quality;
    /** SQL generated from this source */
    private String generatedSql;
    /** Confidence score (0-100) */
    private Integer confidenceScore;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
