package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.ReportSchedule;

public interface ReportScheduleService extends IService<ReportSchedule> {
    
    /**
     * Execute a scheduled report immediately.
     */
    void executeSchedule(Long scheduleId);
    
    /**
     * Pause a schedule.
     */
    void pauseSchedule(Long scheduleId);
    
    /**
     * Resume a schedule.
     */
    void resumeSchedule(Long scheduleId);
}
