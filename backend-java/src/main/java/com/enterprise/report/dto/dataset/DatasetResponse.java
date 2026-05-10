package com.enterprise.report.dto.dataset;

import com.enterprise.report.entity.DatasetColumn;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DatasetResponse {
    private Long id;
    private Long tenantId;
    private Long dataSourceId;
    private String name;
    private String description;
    private String tableName;
    private String querySql;
    private String config;
    private Integer status;
    private LocalDateTime lastSyncAt;
    private Long rowCount;
    private LocalDateTime createdAt;
    private List<DatasetColumn> columns;
}
