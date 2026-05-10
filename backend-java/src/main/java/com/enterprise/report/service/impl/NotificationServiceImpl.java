package com.enterprise.report.service.impl;

import com.enterprise.report.entity.Notification;
import com.enterprise.report.mapper.NotificationMapper;
import com.enterprise.report.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void createNotification(Long tenantId, Long userId, String type, String title, String message, String link) {
        try {
            Notification notification = new Notification();
            notification.setTenantId(tenantId);
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setLink(link);
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        } catch (Exception e) {
            log.error("Failed to create notification: {}", e.getMessage());
        }
    }

    @Override
    public void notifyWorkflowComplete(Long tenantId, Long userId, String workflowName, Long runId) {
        createNotification(tenantId, userId, "success",
                "工作流执行完成",
                String.format("工作流「%s」执行完成", workflowName),
                "/workflow/run/" + runId);
    }

    @Override
    public void notifyWorkflowFailed(Long tenantId, Long userId, String workflowName, Long runId, String error) {
        createNotification(tenantId, userId, "error",
                "工作流执行失败",
                String.format("工作流「%s」执行失败: %s", workflowName, error),
                "/workflow/run/" + runId);
    }

    @Override
    public void notifyReportReady(Long tenantId, Long userId, String reportName, Long reportId) {
        createNotification(tenantId, userId, "success",
                "报表生成完成",
                String.format("报表「%s」已生成，可前往下载", reportName),
                "/output/reports");
    }

    @Override
    public void notifySystemAlert(Long tenantId, Long userId, String message) {
        createNotification(tenantId, userId, "warning",
                "系统告警", message, null);
    }
}
