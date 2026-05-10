package com.enterprise.report.service;

import com.enterprise.report.entity.Notification;

public interface NotificationService {
    void createNotification(Long tenantId, Long userId, String type, String title, String message, String link);
    void notifyWorkflowComplete(Long tenantId, Long userId, String workflowName, Long runId);
    void notifyWorkflowFailed(Long tenantId, Long userId, String workflowName, Long runId, String error);
    void notifyReportReady(Long tenantId, Long userId, String reportName, Long reportId);
    void notifySystemAlert(Long tenantId, Long userId, String message);
}
