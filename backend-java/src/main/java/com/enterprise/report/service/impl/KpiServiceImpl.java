package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.dto.kpi.KpiExecuteRequest;
import com.enterprise.report.engine.kpi.KpiDslEvaluator;
import com.enterprise.report.entity.KpiDefinition;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.KpiDefinitionMapper;
import com.enterprise.report.service.KpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiServiceImpl extends ServiceImpl<KpiDefinitionMapper, KpiDefinition> implements KpiService {

    private final KpiDslEvaluator dslEvaluator;

    @Override
    public BigDecimal executeKpi(KpiExecuteRequest request) {
        KpiDefinition kpi = getById(request.getKpiId());
        if (kpi == null) {
            throw new BusinessException(404, "KPI not found");
        }
        if (!"active".equals(kpi.getStatus())) {
            throw new BusinessException(400, "KPI is not active");
        }

        Map<String, Object> params = request.getParameters();
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        if (request.getPeriodStart() != null) {
            params.put("start", request.getPeriodStart());
        }
        if (request.getPeriodEnd() != null) {
            params.put("end", request.getPeriodEnd());
        }

        return evaluateExpression(kpi.getExpression(), kpi.getDatasetId(), params);
    }

    @Override
    public BigDecimal evaluateExpression(String expression, Long datasetId, Map<String, Object> params) {
        try {
            return dslEvaluator.evaluate(expression, datasetId, params);
        } catch (Exception e) {
            log.error("KPI expression evaluation failed: {}", e.getMessage());
            throw new BusinessException(500, "KPI evaluation failed: " + e.getMessage());
        }
    }
}
