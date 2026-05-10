package com.enterprise.report.engine.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.entity.*;
import com.enterprise.report.enums.WorkflowState;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.WorkflowNodeRunMapper;
import com.enterprise.report.mapper.WorkflowRunMapper;
import com.enterprise.report.mapper.WorkflowStateSnapshotMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DagExecutor {

    private final List<NodeExecutor> nodeExecutors;
    private final WorkflowNodeRunMapper nodeRunMapper;
    private final WorkflowRunMapper runMapper;
    private final WorkflowStateSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;

    public void execute(WorkflowRun run, WorkflowDefinition workflow) {
        run.setState(WorkflowState.RUNNING);
        run.setStartTime(LocalDateTime.now());
        runMapper.updateById(run);

        try {
            DagDefinition dag = parseDag(workflow.getDagDefinition());
            List<String> executionOrder = topologicalSort(dag);

            Map<String, Object> context = new HashMap<>();
            if (run.getInputParams() != null) {
                context.putAll(objectMapper.readValue(run.getInputParams(),
                        new TypeReference<Map<String, Object>>() {}));
            }

            Set<String> completedNodes = new HashSet<>();
            Set<String> failedNodes = new HashSet<>();

            for (String nodeId : executionOrder) {
                DagNode node = findNode(dag, nodeId);
                if (node == null) continue;

                WorkflowNodeRun nodeRun = createNodeRun(run, node);
                nodeRunMapper.insert(nodeRun);

                NodeExecutor executor = getExecutor(node.getType());
                if (executor == null) {
                    nodeRun.setState(WorkflowState.FAILED);
                    nodeRun.setErrorMessage("No executor found for node type: " + node.getType());
                    nodeRun.setEndTime(LocalDateTime.now());
                    nodeRunMapper.updateById(nodeRun);
                    failedNodes.add(nodeId);
                    throw new BusinessException(500, "No executor for node type: " + node.getType());
                }

                LocalDateTime nodeStart = LocalDateTime.now();
                executor.execute(nodeRun, context);
                LocalDateTime nodeEnd = LocalDateTime.now();
                nodeRun.setStartTime(nodeStart);
                nodeRun.setEndTime(nodeEnd);
                nodeRun.setDurationMs(Duration.between(nodeStart, nodeEnd).toMillis());
                nodeRunMapper.updateById(nodeRun);

                if (nodeRun.getState() == WorkflowState.SUCCESS) {
                    completedNodes.add(nodeId);
                } else {
                    failedNodes.add(nodeId);
                    throw new BusinessException(500, "Node '" + nodeId + "' failed: " + nodeRun.getErrorMessage());
                }

                saveStateSnapshot(run, executionOrder, completedNodes, failedNodes, nodeId);
            }

            run.setState(WorkflowState.SUCCESS);
            run.setEndTime(LocalDateTime.now());
            run.setDurationMs(Duration.between(run.getStartTime(), run.getEndTime()).toMillis());
            try {
                run.setOutputResult(objectMapper.writeValueAsString(context));
            } catch (Exception e) {
                log.error("Failed to serialize output: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("DAG execution failed: {}", e.getMessage());
            run.setState(WorkflowState.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(LocalDateTime.now());
        }
        runMapper.updateById(run);
    }

    public void resume(WorkflowRun run, WorkflowDefinition workflow) {
        WorkflowStateSnapshot snapshot = getLatestSnapshot(run.getId());
        if (snapshot == null) {
            execute(run, workflow);
            return;
        }

        run.setState(WorkflowState.RUNNING);
        run.setStartTime(LocalDateTime.now());
        runMapper.updateById(run);

        try {
            DagDefinition dag = parseDag(workflow.getDagDefinition());
            List<String> executionOrder = topologicalSort(dag);

            Set<String> completedNodes = new HashSet<>();
            if (snapshot.getCompletedNodes() != null && !snapshot.getCompletedNodes().isEmpty()) {
                completedNodes.addAll(Arrays.asList(snapshot.getCompletedNodes().split(",")));
            }

            Map<String, Object> context = new HashMap<>();
            if (run.getInputParams() != null) {
                context.putAll(objectMapper.readValue(run.getInputParams(),
                        new TypeReference<Map<String, Object>>() {}));
            }

            for (String nodeId : executionOrder) {
                if (completedNodes.contains(nodeId)) {
                    continue;
                }

                DagNode node = findNode(dag, nodeId);
                if (node == null) continue;

                WorkflowNodeRun nodeRun = createNodeRun(run, node);
                nodeRunMapper.insert(nodeRun);

                NodeExecutor executor = getExecutor(node.getType());
                if (executor == null) {
                    nodeRun.setState(WorkflowState.FAILED);
                    nodeRun.setErrorMessage("No executor found for node type: " + node.getType());
                    nodeRun.setEndTime(LocalDateTime.now());
                    nodeRunMapper.updateById(nodeRun);
                    throw new BusinessException(500, "No executor for node type: " + node.getType());
                }

                LocalDateTime nodeStart = LocalDateTime.now();
                executor.execute(nodeRun, context);
                LocalDateTime nodeEnd = LocalDateTime.now();
                nodeRun.setStartTime(nodeStart);
                nodeRun.setEndTime(nodeEnd);
                nodeRun.setDurationMs(Duration.between(nodeStart, nodeEnd).toMillis());
                nodeRunMapper.updateById(nodeRun);

                if (nodeRun.getState() == WorkflowState.SUCCESS) {
                    completedNodes.add(nodeId);
                    saveStateSnapshot(run, executionOrder, completedNodes, new HashSet<>(), nodeId);
                } else {
                    throw new BusinessException(500, "Node '" + nodeId + "' failed: " + nodeRun.getErrorMessage());
                }
            }

            run.setState(WorkflowState.SUCCESS);
            run.setEndTime(LocalDateTime.now());
            run.setDurationMs(Duration.between(run.getStartTime(), run.getEndTime()).toMillis());
            try {
                run.setOutputResult(objectMapper.writeValueAsString(context));
            } catch (Exception e) {
                log.error("Failed to serialize output: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("DAG resume failed: {}", e.getMessage());
            run.setState(WorkflowState.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(LocalDateTime.now());
        }
        runMapper.updateById(run);
    }

    private DagDefinition parseDag(String dagJson) {
        try {
            return objectMapper.readValue(dagJson, DagDefinition.class);
        } catch (Exception e) {
            throw new BusinessException(400, "Invalid DAG definition: " + e.getMessage());
        }
    }

    private List<String> topologicalSort(DagDefinition dag) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (DagNode node : dag.getNodes()) {
            inDegree.putIfAbsent(node.getId(), 0);
            adjacency.putIfAbsent(node.getId(), new ArrayList<>());
        }

        for (DagEdge edge : dag.getEdges()) {
            adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
            inDegree.merge(edge.getTarget(), 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            order.add(node);
            for (String neighbor : adjacency.getOrDefault(node, List.of())) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (order.size() != dag.getNodes().size()) {
            throw new BusinessException(400, "DAG contains a cycle");
        }
        return order;
    }

    private DagNode findNode(DagDefinition dag, String nodeId) {
        return dag.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    private NodeExecutor getExecutor(String nodeType) {
        return nodeExecutors.stream()
                .filter(e -> e.getNodeType().equals(nodeType))
                .findFirst()
                .orElse(null);
    }

    private WorkflowNodeRun createNodeRun(WorkflowRun run, DagNode node) {
        WorkflowNodeRun nodeRun = new WorkflowNodeRun();
        nodeRun.setTenantId(run.getTenantId());
        nodeRun.setRunId(run.getRunId());
        nodeRun.setNodeId(node.getId());
        nodeRun.setNodeName(node.getName());
        nodeRun.setNodeType(node.getType());
        nodeRun.setState(WorkflowState.PENDING);
        nodeRun.setRetryCount(0);
        if (node.getParams() != null) {
            try {
                nodeRun.setInputData(objectMapper.writeValueAsString(node.getParams()));
            } catch (Exception e) {
                nodeRun.setInputData("{}");
            }
        }
        return nodeRun;
    }

    private void saveStateSnapshot(WorkflowRun run, List<String> executionOrder,
                                   Set<String> completedNodes, Set<String> failedNodes, String currentNodeId) {
        WorkflowStateSnapshot snapshot = new WorkflowStateSnapshot();
        snapshot.setTenantId(run.getTenantId());
        snapshot.setWorkflowRunId(run.getId());
        snapshot.setCurrentNodeId(currentNodeId);
        snapshot.setCompletedNodes(String.join(",", completedNodes));
        snapshot.setFailedNodes(String.join(",", failedNodes));
        try {
            snapshot.setSnapshotData(objectMapper.writeValueAsString(Map.of(
                    "executionOrder", executionOrder,
                    "completedNodes", completedNodes,
                    "failedNodes", failedNodes)));
        } catch (Exception e) {
            snapshot.setSnapshotData("{}");
        }
        snapshotMapper.insert(snapshot);
    }

    private WorkflowStateSnapshot getLatestSnapshot(Long runId) {
        return snapshotMapper.selectOne(
                new LambdaQueryWrapper<WorkflowStateSnapshot>()
                        .eq(WorkflowStateSnapshot::getWorkflowRunId, runId)
                        .orderByDesc(WorkflowStateSnapshot::getCreatedAt)
                        .last("LIMIT 1"));
    }

    @lombok.Data
    public static class DagDefinition {
        private List<DagNode> nodes;
        private List<DagEdge> edges;
    }

    @lombok.Data
    public static class DagNode {
        private String id;
        private String name;
        private String type;
        private Map<String, Object> params;
    }

    @lombok.Data
    public static class DagEdge {
        private String source;
        private String target;
    }
}
