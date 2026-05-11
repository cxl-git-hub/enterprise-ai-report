package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.ReportSchedule;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.ReportScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report-schedules")
@RequiredArgsConstructor
public class ReportScheduleController {

    private final ReportScheduleService reportScheduleService;

    @GetMapping
    public ApiResponse<PageResult<ReportSchedule>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<ReportSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSchedule::getTenantId, tenantId)
               .orderByDesc(ReportSchedule::getCreatedAt);
        return ApiResponse.success(PageResult.from(reportScheduleService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportSchedule> get(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        ReportSchedule schedule = reportScheduleService.getById(id);
        if (schedule == null || !schedule.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Schedule not found");
        }
        return ApiResponse.success(schedule);
    }

    @PostMapping
    public ApiResponse<ReportSchedule> create(@RequestBody ReportSchedule schedule) {
        Long tenantId = TenantContext.getTenantId();
        schedule.setTenantId(tenantId);
        schedule.setStatus("active");
        reportScheduleService.save(schedule);
        return ApiResponse.success(schedule);
    }

    @PutMapping("/{id}")
    public ApiResponse<ReportSchedule> update(@PathVariable Long id, @RequestBody ReportSchedule schedule) {
        Long tenantId = TenantContext.getTenantId();
        ReportSchedule existing = reportScheduleService.getById(id);
        if (existing == null || !existing.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Schedule not found");
        }
        schedule.setId(id);
        schedule.setTenantId(tenantId);
        reportScheduleService.updateById(schedule);
        return ApiResponse.success(schedule);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        ReportSchedule schedule = reportScheduleService.getById(id);
        if (schedule == null || !schedule.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "Schedule not found");
        }
        reportScheduleService.removeById(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<Void> executeNow(@PathVariable Long id) {
        reportScheduleService.executeSchedule(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<Void> pause(@PathVariable Long id) {
        reportScheduleService.pauseSchedule(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<Void> resume(@PathVariable Long id) {
        reportScheduleService.resumeSchedule(id);
        return ApiResponse.success(null);
    }
}
