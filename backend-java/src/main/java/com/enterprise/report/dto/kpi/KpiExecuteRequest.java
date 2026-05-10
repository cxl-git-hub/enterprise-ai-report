package com.enterprise.report.dto.kpi;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class KpiExecuteRequest {
    @NotNull(message = "KPI ID is required")
    private Long kpiId;

    private String periodStart;
    private String periodEnd;
    private Map<String, Object> parameters;
}
