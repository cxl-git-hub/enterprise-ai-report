package com.enterprise.report.dto.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SchemaCreateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Dataset ID is required")
    private Long datasetId;

    private String columns;
    private String metrics;
    private String dimensions;
    private String config;
}
