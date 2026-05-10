package com.enterprise.report.dto.dataset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetCreateRequest {
    @NotNull(message = "DataSource ID is required")
    private Long dataSourceId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Table name is required")
    private String tableName;

    private String querySql;
    private String config;
}
