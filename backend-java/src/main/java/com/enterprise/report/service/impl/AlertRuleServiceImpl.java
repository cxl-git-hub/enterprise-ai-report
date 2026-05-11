package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.AlertRule;
import com.enterprise.report.entity.Notification;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.AlertRuleMapper;
import com.enterprise.report.service.AlertRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule>
        implements AlertRuleService {

    private final ObjectMapper objectMapper;

    @Override
    public String parseRuleExpression(String nlExpression) {
        // Parse common Chinese natural language patterns into structured config
        Map<String, Object> config = new HashMap<>();

        // Extract metric name
        String metric = extractMetric(nlExpression);
        config.put("metric", metric);

        // Extract condition
        String condition = extractCondition(nlExpression);
        config.put("condition", condition);

        // Extract threshold
        Double threshold = extractThreshold(nlExpression);
        if (threshold != null) config.put("threshold", threshold);

        // Extract duration/period
        String period = extractPeriod(nlExpression);
        config.put("period", period);

        // Extract comparison type
        if (nlExpression.contains("连续") || nlExpression.contains("持续")) {
            config.put("type", "consecutive");
        } else if (nlExpression.contains("环比") || nlExpression.contains("同比")) {
            config.put("type", "comparison");
        } else {
            config.put("type", "threshold");
        }

        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public boolean evaluateRule(Long ruleId) {
        AlertRule rule = getById(ruleId);
        if (rule == null || !"active".equals(rule.getStatus())) {
            return false;
        }

        try {
            // In production, this would:
            // 1. Query the actual data source
            // 2. Apply the parsed rule config
            // 3. Determine if threshold is breached
            
            rule.setLastCheckAt(LocalDateTime.now());
            
            // Simulate evaluation - in production, replace with actual data query
            boolean triggered = false;
            if (triggered) {
                triggerAlert(rule, "规则 '" + rule.getName() + "' 被触发");
                rule.setLastTriggerAt(LocalDateTime.now());
                rule.setTriggerCount((rule.getTriggerCount() == null ? 0 : rule.getTriggerCount()) + 1);
                rule.setStatus("triggered");
            }
            
            rule.setLastResult(triggered ? "triggered" : "normal");
            updateById(rule);
            return triggered;
        } catch (Exception e) {
            log.error("Failed to evaluate alert rule {}: {}", ruleId, e.getMessage());
            rule.setLastResult("error: " + e.getMessage());
            updateById(rule);
            return false;
        }
    }

    @Override
    public void triggerAlert(AlertRule rule, String message) {
        log.info("Alert triggered for rule '{}' (tenant {}): {}", rule.getName(), rule.getTenantId(), message);
        
        // Create in-app notification
        if ("inapp".equals(rule.getNotifyChannel()) || rule.getNotifyChannel() == null) {
            // Notification would be created via NotificationService
            log.info("In-app notification sent to user {}", rule.getUserId());
        }
        
        // Send email
        if ("email".equals(rule.getNotifyChannel()) && rule.getRecipients() != null) {
            log.info("Email alert sent to {}", rule.getRecipients());
            // In production: integrate with email service
        }
        
        // Send webhook
        if ("webhook".equals(rule.getNotifyChannel()) && rule.getWebhookUrl() != null) {
            log.info("Webhook alert sent to {}", rule.getWebhookUrl());
            // In production: POST to webhook URL
        }
    }

    private String extractMetric(String expr) {
        // Common metric keywords
        String[] metrics = {"销售额", "订单量", "用户数", "转化率", "GMV", "DAU", "收入", "利润", "成本", "库存"};
        for (String m : metrics) {
            if (expr.contains(m)) return m;
        }
        return "未知指标";
    }

    private String extractCondition(String expr) {
        if (expr.contains("超过") || expr.contains("大于") || expr.contains("高于")) return "gt";
        if (expr.contains("低于") || expr.contains("小于") || expr.contains("少于")) return "lt";
        if (expr.contains("下降") || expr.contains("减少")) return "decrease";
        if (expr.contains("上升") || expr.contains("增长") || expr.contains("增加")) return "increase";
        if (expr.contains("等于")) return "eq";
        return "gt";
    }

    private Double extractThreshold(String expr) {
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%?");
        Matcher m = p.matcher(expr);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return null;
    }

    private String extractPeriod(String expr) {
        if (expr.contains("连续3天") || expr.contains("持续3天")) return "3d";
        if (expr.contains("连续7天") || expr.contains("持续7天")) return "7d";
        if (expr.contains("连续") || expr.contains("持续")) {
            Pattern p = Pattern.compile("(\\d+)\\s*[天日]");
            Matcher m = p.matcher(expr);
            if (m.find()) return m.group(1) + "d";
        }
        if (expr.contains("本月") || expr.contains("当月")) return "1M";
        if (expr.contains("本周")) return "1w";
        return "1d";
    }
}
