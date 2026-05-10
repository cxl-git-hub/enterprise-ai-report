package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.ReportTemplate;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.ReportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report-templates")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final ReportTemplateService reportTemplateService;

    @GetMapping
    public ApiResponse<PageResult<ReportTemplate>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ReportTemplate::getName, keyword);
        }
        wrapper.orderByDesc(ReportTemplate::getCreatedAt);
        return ApiResponse.success(PageResult.from(reportTemplateService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportTemplate> get(@PathVariable Long id) {
        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            throw new BusinessException(404, "Report template not found");
        }
        return ApiResponse.success(template);
    }

    @PostMapping
    public ApiResponse<ReportTemplate> create(@RequestBody ReportTemplate template) {
        template.setTenantId(TenantContext.getTenantId());
        template.setVersion(1);
        template.setStatus("active");
        reportTemplateService.save(template);
        return ApiResponse.success(template);
    }

    @PutMapping("/{id}")
    public ApiResponse<ReportTemplate> update(@PathVariable Long id, @RequestBody ReportTemplate template) {
        template.setId(id);
        reportTemplateService.updateById(template);
        return ApiResponse.success(template);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null || !template.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Report template not found");
        }
        reportTemplateService.removeById(id);
        return ApiResponse.success();
    }
}
