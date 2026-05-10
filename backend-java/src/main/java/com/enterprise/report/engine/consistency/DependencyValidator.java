package com.enterprise.report.engine.consistency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.entity.*;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DependencyValidator {

    private final SchemaDefinitionMapper schemaMapper;
    private final KpiDefinitionMapper kpiMapper;
    private final DatasetMapper datasetMapper;
    private final ObjectMapper objectMapper;

    public void validateSchemaReference(Long schemaId) {
        SchemaDefinition schema = schemaMapper.selectById(schemaId);
        if (schema == null) {
            throw new BusinessException(400, "Referenced schema not found: " + schemaId);
        }
        if (schema.getStatus() != 1) {
            throw new BusinessException(400, "Referenced schema is not active: " + schemaId);
        }
    }

    public void validateKpiReference(Long kpiId) {
        KpiDefinition kpi = kpiMapper.selectById(kpiId);
        if (kpi == null) {
            throw new BusinessException(400, "Referenced KPI not found: " + kpiId);
        }
        if (kpi.getStatus() != 1) {
            throw new BusinessException(400, "Referenced KPI is not active: " + kpiId);
        }
        if (kpi.getSchemaId() != null) {
            validateSchemaReference(kpi.getSchemaId());
        }
    }

    public void validateWorkflowDependencies(WorkflowDefinition workflow) {
        if (workflow.getDagDefinition() == null || workflow.getDagDefinition().isEmpty()) {
            return;
        }

        try {
            DagExecutor.DagDefinition dag = objectMapper.readValue(workflow.getDagDefinition(),
                    DagExecutor.DagDefinition.class);

            for (DagExecutor.DagNode node : dag.getNodes()) {
                if (node.getParams() == null) continue;

                if ("kpi_calc".equals(node.getType())) {
                    Object kpiIdObj = node.getParams().get("kpiId");
                    if (kpiIdObj != null) {
                        Long kpiId = ((Number) kpiIdObj).longValue();
                        validateKpiReference(kpiId);
                    }
                }

                if ("ai_analysis".equals(node.getType())) {
                    Object schemaIdObj = node.getParams().get("schemaId");
                    if (schemaIdObj != null) {
                        Long schemaId = ((Number) schemaIdObj).longValue();
                        validateSchemaReference(schemaId);
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse DAG definition for validation: {}", e.getMessage());
        }
    }
}
