package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.ReportOutput;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.MinioService;
import com.enterprise.report.service.ReportOutputService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report-outputs")
@RequiredArgsConstructor
public class ReportOutputController {

    private final ReportOutputService reportOutputService;
    private final MinioService minioService;

    @GetMapping
    public ApiResponse<PageResult<ReportOutput>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long workflowRunId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<ReportOutput> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportOutput::getTenantId, tenantId);
        if (workflowRunId != null) {
            wrapper.eq(ReportOutput::getWorkflowRunId, workflowRunId);
        }
        wrapper.orderByDesc(ReportOutput::getCreatedAt);
        return ApiResponse.success(PageResult.from(reportOutputService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportOutput> get(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        ReportOutput output = reportOutputService.getById(id);
        if (output == null || !output.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Report not found");
        }
        return ApiResponse.success(output);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        ReportOutput output = reportOutputService.getById(id);
        if (output == null) {
            throw new BusinessException(404, "Report not found");
        }

        try {
            InputStreamResource resource = minioService.downloadFile(output.getFileKey());
            String contentType = getContentType(output.getFormat());
            String fileName = output.getFileName() != null ? output.getFileName() : "report";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to download report: " + e.getMessage());
        }
    }

    private String getContentType(com.enterprise.report.enums.ReportFormat format) {
        if (format == null) return "application/octet-stream";
        return switch (format) {
            case WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case PPT -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case PDF -> "application/pdf";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case HTML -> "text/html";
        };
    }
}
