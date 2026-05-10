package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.WorkflowRun;
import com.enterprise.report.entity.WorkflowNodeRun;
import java.util.List;
import java.util.Map;

public interface WorkflowExecutionService extends IService<WorkflowRun> {
    WorkflowRun trigger(Long workflowId, Map<String, Object> inputParams);
    WorkflowRun getRunDetail(Long runId);
    WorkflowRun resume(Long runId);
    List<WorkflowNodeRun> getNodeRuns(Long runId);
}
