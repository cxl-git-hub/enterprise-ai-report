package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.ReportOutput;
import com.enterprise.report.service.ReportOutputService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report-outputs")
@RequiredArgsConstructor
public class ReportOutputController {

    private final ReportOutputService reportOutputService;

    @GetMapping
    public ApiResponse<PageResult<ReportOutput>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long workflowRunId) {
        LambdaQueryWrapper<ReportOutput> wrapper = new LambdaQueryWrapper<>();
        if (workflowRunId != null) {
            wrapper.eq(ReportOutput::getWorkflowRunId, workflowRunId);
        }
        wrapper.orderByDesc(ReportOutput::getCreatedAt);
        return ApiResponse.success(PageResult.from(reportOutputService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportOutput> get(@PathVariable Long id) {
        return ApiResponse.success(reportOutputService.getById(id));
    }

    @GetMapping("/{id}/download")
    public ApiResponse<String> download(@PathVariable Long id) {
        return ApiResponse.success(reportOutputService.getDownloadUrl(id));
    }
}
