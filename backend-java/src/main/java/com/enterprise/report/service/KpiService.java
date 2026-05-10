package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.KpiDefinition;
import com.enterprise.report.entity.KpiResult;
import com.enterprise.report.dto.kpi.KpiExecuteRequest;
import java.math.BigDecimal;
import java.util.Map;

public interface KpiService extends IService<KpiDefinition> {
    BigDecimal executeKpi(KpiExecuteRequest request);
    BigDecimal evaluateExpression(String expression, Long datasetId, Map<String, Object> params);
}
