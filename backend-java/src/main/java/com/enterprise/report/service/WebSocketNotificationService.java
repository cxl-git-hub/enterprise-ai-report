package com.enterprise.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending real-time WebSocket notifications to connected clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to a specific user
     */
    public void sendToUser(Long userId, String type, String title, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("title", title);
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), "/queue/notifications", payload);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send notification to all users in a tenant
     */
    public void sendToTenant(Long tenantId, String type, String title, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("title", title);
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/tenant/" + tenantId + "/notifications", payload);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to tenant {}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * Send workflow status update
     */
    public void sendWorkflowUpdate(Long tenantId, Long userId, Long workflowRunId, String status, String workflowName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "workflow_update");
        payload.put("workflowRunId", workflowRunId);
        payload.put("status", status);
        payload.put("workflowName", workflowName);
        payload.put("timestamp", System.currentTimeMillis());

        if (userId != null) {
            sendToUser(userId, "workflow_update", "工作流状态更新",
                    "工作流 " + workflowName + " 状态: " + status);
        }
        sendToTenant(tenantId, "workflow_update", "工作流状态更新",
                "工作流 " + workflowName + " 状态: " + status);
    }
}
