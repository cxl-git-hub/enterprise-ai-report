package com.enterprise.report.dto.kpi;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KpiResponse {
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private Long schemaId;
    private Long datasetId;
    private String expression;
    private String unit;
    private String aggregationType;
    private String filterCondition;
    private String groupBy;
    private String config;
    private Integer version;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
