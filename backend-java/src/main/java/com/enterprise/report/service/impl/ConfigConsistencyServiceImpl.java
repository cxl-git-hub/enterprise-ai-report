package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.config.*;
import com.enterprise.report.engine.consistency.DependencyValidator;
import com.enterprise.report.entity.*;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.*;
import com.enterprise.report.service.ConfigConsistencyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigConsistencyServiceImpl implements ConfigConsistencyService {

    private final SchemaDefinitionMapper schemaMapper;
    private final KpiDefinitionMapper kpiMapper;
    private final WorkflowDefinitionMapper workflowMapper;
    private final PromptTemplateMapper promptMapper;
    private final ReportTemplateMapper reportMapper;
    private final ConfigDependencyGraphMapper dependencyMapper;
    private final ConfigSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;
    private final DependencyValidator dependencyValidator;

    @Override
    public List<String> validateDependencies(Long tenantId) {
        ValidationResult result = validateStructured(tenantId);
        List<String> errors = new ArrayList<>();
        for (ValidationResult.ValidationError err : result.getErrors()) {
            errors.add("[" + err.getType() + "] " + err.getMessage() + " (" + err.getRefName() + ")");
        }
        for (ValidationResult.ValidationWarning warn : result.getWarnings()) {
            errors.add("[WARN][" + warn.getType() + "] " + warn.getMessage() + " (" + warn.getRefName() + ")");
        }
        return errors;
    }

    @Override
    public ValidationResult validateStructured(Long tenantId) {
        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        List<ValidationResult.ValidationWarning> warnings = new ArrayList<>();

        List<SchemaDefinition> schemas = schemaMapper.selectList(
                new LambdaQueryWrapper<SchemaDefinition>().eq(SchemaDefinition::getTenantId, tenantId));
        List<KpiDefinition> kpis = kpiMapper.selectList(
                new LambdaQueryWrapper<KpiDefinition>().eq(KpiDefinition::getTenantId, tenantId));
        List<WorkflowDefinition> workflows = workflowMapper.selectList(
                new LambdaQueryWrapper<WorkflowDefinition>().eq(WorkflowDefinition::getTenantId, tenantId));
        List<PromptTemplate> prompts = promptMapper.selectList(
                new LambdaQueryWrapper<PromptTemplate>().eq(PromptTemplate::getTenantId, tenantId));
        List<ReportTemplate> reports = reportMapper.selectList(
                new LambdaQueryWrapper<ReportTemplate>().eq(ReportTemplate::getTenantId, tenantId));

        Map<Long, SchemaDefinition> schemaMap = new HashMap<>();
        for (SchemaDefinition s : schemas) {
            schemaMap.put(s.getId(), s);
        }
        Map<Long, KpiDefinition> kpiMap = new HashMap<>();
        for (KpiDefinition k : kpis) {
            kpiMap.put(k.getId(), k);
        }

        // Validate KPI → Schema references
        for (KpiDefinition kpi : kpis) {
            if (kpi.getSchemaId() != null) {
                SchemaDefinition schema = schemaMap.get(kpi.getSchemaId());
                if (schema == null) {
                    errors.add(createError("kpi", "KPI '" + kpi.getName() + "' 引用了不存在的 Schema (ID: " + kpi.getSchemaId() + ")",
                            String.valueOf(kpi.getId()), kpi.getName()));
                } else if (!"active".equals(schema.getStatus())) {
                    warnings.add(createWarning("kpi", "KPI '" + kpi.getName() + "' 引用的 Schema '" + schema.getName() + "' 不是 active 状态",
                            String.valueOf(kpi.getId()), kpi.getName()));
                }
            }
        }

        // Validate Workflow → KPI references
        for (WorkflowDefinition wf : workflows) {
            try {
                dependencyValidator.validateWorkflowDependencies(wf);
            } catch (BusinessException e) {
                errors.add(createError("workflow", "工作流 '" + wf.getName() + "': " + e.getMessage(),
                        String.valueOf(wf.getId()), wf.getName()));
            }
        }

        // Validate Prompt → Schema references
        for (PromptTemplate prompt : prompts) {
            if (prompt.getSchemaId() != null) {
                SchemaDefinition schema = schemaMap.get(prompt.getSchemaId());
                if (schema == null) {
                    errors.add(createError("prompt", "Prompt '" + prompt.getName() + "' 引用了不存在的 Schema (ID: " + prompt.getSchemaId() + ")",
                            String.valueOf(prompt.getId()), prompt.getName()));
                }
            }
        }

        // Validate orphan schemas (not referenced by any KPI or Prompt)
        Set<Long> referencedSchemaIds = new HashSet<>();
        for (KpiDefinition kpi : kpis) {
            if (kpi.getSchemaId() != null) referencedSchemaIds.add(kpi.getSchemaId());
        }
        for (PromptTemplate prompt : prompts) {
            if (prompt.getSchemaId() != null) referencedSchemaIds.add(prompt.getSchemaId());
        }
        for (SchemaDefinition schema : schemas) {
            if (!referencedSchemaIds.contains(schema.getId())) {
                warnings.add(createWarning("schema", "Schema '" + schema.getName() + "' 未被任何 KPI 或 Prompt 引用",
                        String.valueOf(schema.getId()), schema.getName()));
            }
        }

        ValidationResult result = new ValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        result.setWarnings(warnings);
        return result;
    }

    @Override
    public DependencyGraph getDependencyGraph(Long tenantId) {
        List<SchemaDefinition> schemas = schemaMapper.selectList(
                new LambdaQueryWrapper<SchemaDefinition>().eq(SchemaDefinition::getTenantId, tenantId));
        List<KpiDefinition> kpis = kpiMapper.selectList(
                new LambdaQueryWrapper<KpiDefinition>().eq(KpiDefinition::getTenantId, tenantId));
        List<WorkflowDefinition> workflows = workflowMapper.selectList(
                new LambdaQueryWrapper<WorkflowDefinition>().eq(WorkflowDefinition::getTenantId, tenantId));
        List<PromptTemplate> prompts = promptMapper.selectList(
                new LambdaQueryWrapper<PromptTemplate>().eq(PromptTemplate::getTenantId, tenantId));
        List<ReportTemplate> reports = reportMapper.selectList(
                new LambdaQueryWrapper<ReportTemplate>().eq(ReportTemplate::getTenantId, tenantId));

        DependencyGraph graph = new DependencyGraph();
        List<DependencyGraph.DependencyNode> nodes = new ArrayList<>();
        List<DependencyGraph.DependencyEdge> edges = new ArrayList<>();

        // Add schema nodes
        for (SchemaDefinition s : schemas) {
            DependencyGraph.DependencyNode node = new DependencyGraph.DependencyNode();
            node.setId("schema_" + s.getId());
            node.setName(s.getName());
            node.setType("schema");
            node.setDependencies(List.of());
            nodes.add(node);
        }

        // Add KPI nodes with edges to schemas
        for (KpiDefinition kpi : kpis) {
            DependencyGraph.DependencyNode node = new DependencyGraph.DependencyNode();
            node.setId("kpi_" + kpi.getId());
            node.setName(kpi.getName());
            node.setType("kpi");
            List<String> deps = new ArrayList<>();
            if (kpi.getSchemaId() != null) {
                deps.add("schema_" + kpi.getSchemaId());
                edges.add(createEdge("schema_" + kpi.getSchemaId(), "kpi_" + kpi.getId(), "reference"));
            }
            node.setDependencies(deps);
            nodes.add(node);
        }

        // Add workflow nodes with edges to KPIs
        for (WorkflowDefinition wf : workflows) {
            DependencyGraph.DependencyNode node = new DependencyGraph.DependencyNode();
            node.setId("workflow_" + wf.getId());
            node.setName(wf.getName());
            node.setType("workflow");
            List<String> deps = new ArrayList<>();
            // Parse DAG to find KPI references
            try {
                if (wf.getDagDefinition() != null && !wf.getDagDefinition().isEmpty()) {
                    DagExecutor.DagDefinition dag = objectMapper.readValue(wf.getDagDefinition(), DagExecutor.DagDefinition.class);
                    for (DagExecutor.DagNode dagNode : dag.getNodes()) {
                        if ("kpi_calc".equals(dagNode.getType()) && dagNode.getParams() != null) {
                            Object kpiIdObj = dagNode.getParams().get("kpiId");
                            if (kpiIdObj != null) {
                                String kpiRef = "kpi_" + kpiIdObj;
                                deps.add(kpiRef);
                                edges.add(createEdge(kpiRef, "workflow_" + wf.getId(), "reference"));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            node.setDependencies(deps);
            nodes.add(node);
        }

        // Add prompt nodes with edges to schemas
        for (PromptTemplate prompt : prompts) {
            DependencyGraph.DependencyNode node = new DependencyGraph.DependencyNode();
            node.setId("prompt_" + prompt.getId());
            node.setName(prompt.getName());
            node.setType("prompt");
            List<String> deps = new ArrayList<>();
            if (prompt.getSchemaId() != null) {
                deps.add("schema_" + prompt.getSchemaId());
                edges.add(createEdge("schema_" + prompt.getSchemaId(), "prompt_" + prompt.getId(), "reference"));
            }
            node.setDependencies(deps);
            nodes.add(node);
        }

        // Add report template nodes
        for (ReportTemplate report : reports) {
            DependencyGraph.DependencyNode node = new DependencyGraph.DependencyNode();
            node.setId("report_" + report.getId());
            node.setName(report.getName());
            node.setType("report_template");
            node.setDependencies(List.of());
            nodes.add(node);
        }

        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
    }

    @Override
    @Transactional
    public ConfigSnapshot createSnapshot(Long tenantId, String name, String description) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("schemas", schemaMapper.selectList(
                new LambdaQueryWrapper<SchemaDefinition>().eq(SchemaDefinition::getTenantId, tenantId)));
        snapshot.put("kpis", kpiMapper.selectList(
                new LambdaQueryWrapper<KpiDefinition>().eq(KpiDefinition::getTenantId, tenantId)));
        snapshot.put("workflows", workflowMapper.selectList(
                new LambdaQueryWrapper<WorkflowDefinition>().eq(WorkflowDefinition::getTenantId, tenantId)));
        snapshot.put("prompts", promptMapper.selectList(
                new LambdaQueryWrapper<PromptTemplate>().eq(PromptTemplate::getTenantId, tenantId)));
        snapshot.put("reports", reportMapper.selectList(
                new LambdaQueryWrapper<ReportTemplate>().eq(ReportTemplate::getTenantId, tenantId)));

        ConfigSnapshot configSnapshot = new ConfigSnapshot();
        configSnapshot.setTenantId(tenantId);
        configSnapshot.setSnapshotName(name);
        configSnapshot.setDescription(description);
        configSnapshot.setSnapshotVersion("v" + System.currentTimeMillis());
        configSnapshot.setCreatedBy(tenantId);

        try {
            configSnapshot.setFullSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to serialize snapshot: " + e.getMessage());
        }

        snapshotMapper.insert(configSnapshot);
        return configSnapshot;
    }

    @Override
    public PageResult<ConfigSnapshot> getSnapshots(Long tenantId, Integer page, Integer pageSize) {
        Page<ConfigSnapshot> pageResult = snapshotMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ConfigSnapshot>()
                        .eq(ConfigSnapshot::getTenantId, tenantId)
                        .orderByDesc(ConfigSnapshot::getCreatedAt));
        return PageResult.from(pageResult);
    }

    @Override
    public ConfigSnapshot getSnapshot(Long snapshotId) {
        ConfigSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BusinessException(404, "Snapshot not found");
        }
        return snapshot;
    }

    @Override
    @Transactional
    public void rollback(Long snapshotId) {
        ConfigSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BusinessException(404, "Snapshot not found");
        }

        try {
            Map<String, List<?>> data = objectMapper.readValue(snapshot.getFullSnapshot(),
                    new TypeReference<Map<String, List<?>>() {});

            Long tenantId = snapshot.getTenantId();

            // Delete existing configs
            schemaMapper.delete(new LambdaQueryWrapper<SchemaDefinition>().eq(SchemaDefinition::getTenantId, tenantId));
            kpiMapper.delete(new LambdaQueryWrapper<KpiDefinition>().eq(KpiDefinition::getTenantId, tenantId));
            workflowMapper.delete(new LambdaQueryWrapper<WorkflowDefinition>().eq(WorkflowDefinition::getTenantId, tenantId));
            promptMapper.delete(new LambdaQueryWrapper<PromptTemplate>().eq(PromptTemplate::getTenantId, tenantId));
            reportMapper.delete(new LambdaQueryWrapper<ReportTemplate>().eq(ReportTemplate::getTenantId, tenantId));

            // Restore from snapshot
            restoreList(data.get("schemas"), SchemaDefinition.class, schemaMapper);
            restoreList(data.get("kpis"), KpiDefinition.class, kpiMapper);
            restoreList(data.get("workflows"), WorkflowDefinition.class, workflowMapper);
            restoreList(data.get("prompts"), PromptTemplate.class, promptMapper);
            restoreList(data.get("reports"), ReportTemplate.class, reportMapper);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to rollback: " + e.getMessage());
        }
    }

    @Override
    public SnapshotDiff diffSnapshots(Long id1, Long id2) {
        ConfigSnapshot snap1 = snapshotMapper.selectById(id1);
        ConfigSnapshot snap2 = snapshotMapper.selectById(id2);
        if (snap1 == null || snap2 == null) {
            throw new BusinessException(404, "Snapshot not found");
        }

        SnapshotDiff diff = new SnapshotDiff();
        List<SnapshotDiff.Change> changes = new ArrayList<>();

        try {
            Map<String, List<?>> data1 = objectMapper.readValue(snap1.getFullSnapshot(),
                    new TypeReference<Map<String, List<?>>() {});
            Map<String, List<?>> data2 = objectMapper.readValue(snap2.getFullSnapshot(),
                    new TypeReference<Map<String, List<?>>() {});

            // Compare each config type
            for (String key : List.of("schemas", "kpis", "workflows", "prompts", "reports")) {
                List<?> list1 = data1.getOrDefault(key, List.of());
                List<?> list2 = data2.getOrDefault(key, List.of());

                int maxSize = Math.max(list1.size(), list2.size());
                for (int i = 0; i < maxSize; i++) {
                    if (i >= list1.size()) {
                        SnapshotDiff.Change change = new SnapshotDiff.Change();
                        change.setType("added");
                        change.setPath(key + "[" + i + "]");
                        change.setNewValue(list2.get(i));
                        changes.add(change);
                    } else if (i >= list2.size()) {
                        SnapshotDiff.Change change = new SnapshotDiff.Change();
                        change.setType("removed");
                        change.setPath(key + "[" + i + "]");
                        change.setOldValue(list1.get(i));
                        changes.add(change);
                    } else {
                        String json1 = objectMapper.writeValueAsString(list1.get(i));
                        String json2 = objectMapper.writeValueAsString(list2.get(i));
                        if (!json1.equals(json2)) {
                            SnapshotDiff.Change change = new SnapshotDiff.Change();
                            change.setType("modified");
                            change.setPath(key + "[" + i + "]");
                            change.setOldValue(list1.get(i));
                            change.setNewValue(list2.get(i));
                            changes.add(change);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to diff snapshots: {}", e.getMessage());
        }

        diff.setChanges(changes);
        return diff;
    }

    @Override
    @Transactional
    public void importConfig(Long tenantId, String json, boolean merge) {
        try {
            Map<String, List<?>> data = objectMapper.readValue(json, new TypeReference<Map<String, List<?>>>() {});

            if (!merge) {
                // Full replace: delete existing configs first
                schemaMapper.delete(new LambdaQueryWrapper<SchemaDefinition>().eq(SchemaDefinition::getTenantId, tenantId));
                kpiMapper.delete(new LambdaQueryWrapper<KpiDefinition>().eq(KpiDefinition::getTenantId, tenantId));
                workflowMapper.delete(new LambdaQueryWrapper<WorkflowDefinition>().eq(WorkflowDefinition::getTenantId, tenantId));
                promptMapper.delete(new LambdaQueryWrapper<PromptTemplate>().eq(PromptTemplate::getTenantId, tenantId));
                reportMapper.delete(new LambdaQueryWrapper<ReportTemplate>().eq(ReportTemplate::getTenantId, tenantId));
            }

            // Import from JSON
            restoreList(data.get("schemas"), SchemaDefinition.class, schemaMapper);
            restoreList(data.get("kpis"), KpiDefinition.class, kpiMapper);
            restoreList(data.get("workflows"), WorkflowDefinition.class, workflowMapper);
            restoreList(data.get("prompts"), PromptTemplate.class, promptMapper);
            restoreList(data.get("reports"), ReportTemplate.class, reportMapper);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to import config: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void restoreList(List<?> data, Class<T> clazz, com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper) {
        if (data == null) return;
        for (Object item : data) {
            T entity = objectMapper.convertValue(item, clazz);
            mapper.insert(entity);
        }
    }

    private ValidationResult.ValidationError createError(String type, String message, String refId, String refName) {
        ValidationResult.ValidationError error = new ValidationResult.ValidationError();
        error.setType(type);
        error.setMessage(message);
        error.setRefId(refId);
        error.setRefName(refName);
        return error;
    }

    private ValidationResult.ValidationWarning createWarning(String type, String message, String refId, String refName) {
        ValidationResult.ValidationWarning warning = new ValidationResult.ValidationWarning();
        warning.setType(type);
        warning.setMessage(message);
        warning.setRefId(refId);
        warning.setRefName(refName);
        return warning;
    }

    private DependencyGraph.DependencyEdge createEdge(String from, String to, String type) {
        DependencyGraph.DependencyEdge edge = new DependencyGraph.DependencyEdge();
        edge.setFrom(from);
        edge.setTo(to);
        edge.setType(type);
        return edge;
    }
}
