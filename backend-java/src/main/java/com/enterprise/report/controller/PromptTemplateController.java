package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.PromptTemplate;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping
    public ApiResponse<PageResult<PromptTemplate>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<PromptTemplate> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(PromptTemplate::getName, keyword);
        }
        wrapper.orderByDesc(PromptTemplate::getCreatedAt);
        return ApiResponse.success(PageResult.from(promptTemplateService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<PromptTemplate> get(@PathVariable Long id) {
        PromptTemplate template = promptTemplateService.getById(id);
        if (template == null) {
            throw new BusinessException(404, "Prompt template not found");
        }
        return ApiResponse.success(template);
    }

    @PostMapping
    public ApiResponse<PromptTemplate> create(@RequestBody PromptTemplate template) {
        template.setTenantId(TenantContext.getTenantId());
        return ApiResponse.success(promptTemplateService.createWithValidation(template));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromptTemplate> update(@PathVariable Long id, @RequestBody PromptTemplate template) {
        template.setId(id);
        promptTemplateService.updateById(template);
        return ApiResponse.success(template);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        PromptTemplate template = promptTemplateService.getById(id);
        if (template == null || !template.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Prompt template not found");
        }
        promptTemplateService.removeById(id);
        return ApiResponse.success();
    }
}
