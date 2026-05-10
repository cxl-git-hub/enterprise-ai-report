package com.enterprise.report.dto.kpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KpiCreateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Schema ID is required")
    private Long schemaId;

    @NotNull(message = "Dataset ID is required")
    private Long datasetId;

    @NotBlank(message = "Expression is required")
    private String expression;

    private String unit;
    private String aggregationType;
    private String filterCondition;
    private String groupBy;
    private String config;
}
