package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.engine.consistency.DependencyValidator;
import com.enterprise.report.entity.PromptTemplate;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.PromptTemplateMapper;
import com.enterprise.report.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate> implements PromptTemplateService {

    private final DependencyValidator dependencyValidator;

    @Override
    public PromptTemplate createWithValidation(PromptTemplate template) {
        if (template.getSchemaId() != null) {
            dependencyValidator.validateSchemaReference(template.getSchemaId());
        }
        template.setVersion(1);
        template.setStatus(1);
        save(template);
        return template;
    }
}
