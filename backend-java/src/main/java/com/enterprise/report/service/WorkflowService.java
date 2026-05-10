package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.WorkflowDefinition;

public interface WorkflowService extends IService<WorkflowDefinition> {
    WorkflowDefinition createWithValidation(WorkflowDefinition workflow);
    WorkflowDefinition updateWithValidation(Long id, WorkflowDefinition workflow);
}
