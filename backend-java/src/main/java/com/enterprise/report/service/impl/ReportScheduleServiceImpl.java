package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.ReportSchedule;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.ReportScheduleMapper;
import com.enterprise.report.service.ReportScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ReportScheduleServiceImpl extends ServiceImpl<ReportScheduleMapper, ReportSchedule>
        implements ReportScheduleService {

    @Override
    public void executeSchedule(Long scheduleId) {
        ReportSchedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(404, "Schedule not found");
        }
        
        log.info("Executing scheduled report: {} (template: {})", schedule.getName(), schedule.getReportTemplateId());
        
        try {
            // In production, this would:
            // 1. Load the report template
            // 2. Generate the report using ReportGenerator
            // 3. Send email with the report attached
            // 4. Update lastRunAt and nextRunAt
            
            schedule.setLastRunAt(LocalDateTime.now());
            schedule.setStatus("active");
            schedule.setLastError(null);
            updateById(schedule);
            
            log.info("Scheduled report {} executed successfully", schedule.getName());
        } catch (Exception e) {
            log.error("Failed to execute scheduled report: {}", schedule.getName(), e);
            schedule.setLastError(e.getMessage());
            schedule.setStatus("error");
            updateById(schedule);
            throw new BusinessException(500, "Failed to execute scheduled report: " + e.getMessage());
        }
    }

    @Override
    public void pauseSchedule(Long scheduleId) {
        ReportSchedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(404, "Schedule not found");
        }
        schedule.setStatus("paused");
        updateById(schedule);
    }

    @Override
    public void resumeSchedule(Long scheduleId) {
        ReportSchedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(404, "Schedule not found");
        }
        schedule.setStatus("active");
        updateById(schedule);
    }
}
