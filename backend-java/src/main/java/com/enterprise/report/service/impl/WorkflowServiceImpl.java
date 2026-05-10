package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.engine.consistency.DependencyValidator;
import com.enterprise.report.entity.WorkflowDefinition;
import com.enterprise.report.enums.WorkflowState;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.WorkflowDefinitionMapper;
import com.enterprise.report.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl extends ServiceImpl<WorkflowDefinitionMapper, WorkflowDefinition> implements WorkflowService {

    private final DependencyValidator dependencyValidator;

    @Override
    @Transactional
    public WorkflowDefinition createWithValidation(WorkflowDefinition workflow) {
        dependencyValidator.validateWorkflowDependencies(workflow);
        workflow.setVersion(1);
        workflow.setState(WorkflowState.PENDING);
        workflow.setStatus("draft");
        save(workflow);
        return workflow;
    }

    @Override
    @Transactional
    public WorkflowDefinition updateWithValidation(Long id, WorkflowDefinition workflow) {
        WorkflowDefinition existing = getById(id);
        if (existing == null) {
            throw new BusinessException(404, "Workflow not found");
        }
        dependencyValidator.validateWorkflowDependencies(workflow);
        workflow.setId(id);
        workflow.setVersion(existing.getVersion() + 1);
        updateById(workflow);
        return workflow;
    }
}
