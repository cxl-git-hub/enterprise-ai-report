package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.PromptTemplate;

public interface PromptTemplateService extends IService<PromptTemplate> {
    PromptTemplate createWithValidation(PromptTemplate template);
}
