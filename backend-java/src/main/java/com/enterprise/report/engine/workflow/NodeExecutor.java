package com.enterprise.report.engine.workflow;

import com.enterprise.report.entity.WorkflowNodeRun;

import java.util.Map;

public interface NodeExecutor {
    String getNodeType();
    void execute(WorkflowNodeRun nodeRun, Map<String, Object> context);
}
