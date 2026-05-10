package com.enterprise.report.dto.schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SchemaResponse {
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private Long datasetId;
    private String columns;
    private String metrics;
    private String dimensions;
    private String config;
    private Integer version;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
