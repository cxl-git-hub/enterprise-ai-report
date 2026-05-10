package com.enterprise.report.engine.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.entity.WorkflowDefinition;
import com.enterprise.report.entity.WorkflowRun;
import com.enterprise.report.service.WorkflowExecutionService;
import com.enterprise.report.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cron scheduler for automated workflow execution.
 * Checks every minute for workflows with cron expressions that should run.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class WorkflowCronScheduler {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService executionService;

    // Track last execution time for each workflow to prevent duplicate runs
    private final Map<Long, LocalDateTime> lastRunTime = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 60000) // Check every minute
    public void scheduleWorkflows() {
        List<WorkflowDefinition> workflows = workflowService.list(
                new LambdaQueryWrapper<WorkflowDefinition>()
                        .eq(WorkflowDefinition::getStatus, "active")
                        .isNotNull(WorkflowDefinition::getCronExpression)
                        .ne(WorkflowDefinition::getCronExpression, ""));

        LocalDateTime now = LocalDateTime.now();

        for (WorkflowDefinition workflow : workflows) {
            try {
                if (shouldExecute(workflow, now)) {
                    log.info("Triggering scheduled workflow: {} (cron: {})", workflow.getName(), workflow.getCronExpression());

                    Map<String, Object> params = new HashMap<>();
                    params.put("triggerType", "scheduled");
                    params.put("scheduledTime", now.toString());

                    WorkflowRun run = executionService.trigger(workflow.getId(), params);
                    lastRunTime.put(workflow.getId(), now);

                    log.info("Scheduled workflow triggered, runId: {}", run.getRunId());
                }
            } catch (Exception e) {
                log.error("Failed to trigger scheduled workflow {}: {}", workflow.getName(), e.getMessage());
            }
        }
    }

    private boolean shouldExecute(WorkflowDefinition workflow, LocalDateTime now) {
        // Prevent duplicate runs within the same minute
        LocalDateTime lastRun = lastRunTime.get(workflow.getId());
        if (lastRun != null && lastRun.plusMinutes(1).isAfter(now)) {
            return false;
        }

        // Parse cron expression and check if it matches current time
        String cron = workflow.getCronExpression();
        if (cron == null || cron.isEmpty()) {
            return false;
        }

        return matchesCron(cron, now);
    }

    /**
     * Simple cron expression matcher.
     * Supports format: second minute hour day month weekday
     * Examples: "0 0 9 * * ?" (every day at 9am), "0 */5 * * * ?" (every 5 minutes)
     */
    private boolean matchesCron(String cron, LocalDateTime now) {
        try {
            String[] parts = cron.trim().split("\\s+");
            if (parts.length < 5) {
                // Try 5-field format (minute hour day month weekday)
                return matchesCron5Field(cron, now);
            }
            // 6-field format (second minute hour day month weekday)
            return matchesCron6Field(parts, now);
        } catch (Exception e) {
            log.warn("Invalid cron expression: {}", cron);
            return false;
        }
    }

    private boolean matchesCron5Field(String cron, LocalDateTime now) {
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 5) return false;

        return matchesField(parts[0], now.getMinute()) &&
               matchesField(parts[1], now.getHour()) &&
               matchesField(parts[2], now.getDayOfMonth()) &&
               matchesField(parts[3], now.getMonthValue()) &&
               matchesDayOfWeek(parts[4], now.getDayOfWeek().getValue());
    }

    private boolean matchesCron6Field(String[] parts, LocalDateTime now) {
        return matchesField(parts[0], now.getSecond()) &&
               matchesField(parts[1], now.getMinute()) &&
               matchesField(parts[2], now.getHour()) &&
               matchesField(parts[3], now.getDayOfMonth()) &&
               matchesField(parts[4], now.getMonthValue()) &&
               matchesDayOfWeek(parts[5], now.getDayOfWeek().getValue());
    }

    private boolean matchesField(String pattern, int value) {
        if ("*".equals(pattern) || "?".equals(pattern)) return true;

        // Handle */N (every N)
        if (pattern.startsWith("*/")) {
            int interval = Integer.parseInt(pattern.substring(2));
            return value % interval == 0;
        }

        // Handle exact number
        try {
            return Integer.parseInt(pattern) == value;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean matchesDayOfWeek(String pattern, int dayOfWeek) {
        if ("*".equals(pattern) || "?".equals(pattern)) return true;
        // Java: 1=Monday..7=Sunday, Cron: 0=Sunday..6=Saturday
        try {
            int cronDay = Integer.parseInt(pattern);
            int javaDay = dayOfWeek % 7; // Convert: 7(Sunday) -> 0
            return cronDay == javaDay;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
