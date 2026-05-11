package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.AlertRule;

public interface AlertRuleService extends IService<AlertRule> {

    /**
     * Parse natural language rule expression into structured config.
     */
    String parseRuleExpression(String nlExpression);

    /**
     * Evaluate an alert rule against current data.
     */
    boolean evaluateRule(Long ruleId);

    /**
     * Trigger an alert notification.
     */
    void triggerAlert(AlertRule rule, String message);
}
