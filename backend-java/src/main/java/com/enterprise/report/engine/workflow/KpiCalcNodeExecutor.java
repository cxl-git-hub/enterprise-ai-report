package com.enterprise.report.engine.workflow;

import com.enterprise.report.dto.kpi.KpiExecuteRequest;
import com.enterprise.report.entity.WorkflowNodeRun;
import com.enterprise.report.enums.WorkflowState;
import com.enterprise.report.service.KpiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KpiCalcNodeExecutor implements NodeExecutor {

    private final KpiService kpiService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "kpi_calc";
    }

    @Override
    public void execute(WorkflowNodeRun nodeRun, Map<String, Object> context) {
        nodeRun.setState(WorkflowState.RUNNING);
        nodeRun.setStartedAt(LocalDateTime.now());

        try {
            Map<String, Object> params = new HashMap<>();
            if (nodeRun.getInputParams() != null) {
                params = objectMapper.readValue(nodeRun.getInputParams(),
                        new TypeReference<Map<String, Object>>() {});
            }
            params.putAll(context);

            Long kpiId = ((Number) params.get("kpiId")).longValue();
            KpiExecuteRequest request = new KpiExecuteRequest();
            request.setKpiId(kpiId);
            request.setParameters(params);

            BigDecimal result = kpiService.executeKpi(request);

            Map<String, Object> output = new HashMap<>();
            output.put("kpiId", kpiId);
            output.put("value", result);
            nodeRun.setOutputResult(objectMapper.writeValueAsString(output));
            nodeRun.setState(WorkflowState.SUCCESS);
            context.put("kpi_" + kpiId, result);
        } catch (Exception e) {
            log.error("KPI calc node failed: {}", e.getMessage());
            nodeRun.setState(WorkflowState.FAILED);
            nodeRun.setErrorMessage(e.getMessage());
        } finally {
            nodeRun.setFinishedAt(LocalDateTime.now());
            if (nodeRun.getStartedAt() != null) {
                nodeRun.setDurationMs(java.time.Duration.between(nodeRun.getStartedAt(), nodeRun.getFinishedAt()).toMillis());
            }
        }
    }
}
