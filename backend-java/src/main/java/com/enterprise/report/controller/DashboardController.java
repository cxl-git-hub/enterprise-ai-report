package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.entity.*;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DataSourceService dataSourceService;
    private final KpiService kpiService;
    private final WorkflowService workflowService;
    private final ReportOutputService reportOutputService;
    private final WorkflowExecutionService workflowExecutionService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> stats = new HashMap<>();

        stats.put("datasourceCount", dataSourceService.count(
                new LambdaQueryWrapper<DataSource>().eq(DataSource::getTenantId, tenantId)));
        stats.put("kpiCount", kpiService.count(
                new LambdaQueryWrapper<KpiDefinition>().eq(KpiDefinition::getTenantId, tenantId)));
        stats.put("workflowCount", workflowService.count(
                new LambdaQueryWrapper<WorkflowDefinition>().eq(WorkflowDefinition::getTenantId, tenantId)));
        stats.put("reportCount", reportOutputService.count(
                new LambdaQueryWrapper<ReportOutput>().eq(ReportOutput::getTenantId, tenantId)));

        return ApiResponse.success(stats);
    }

    @GetMapping("/recent-runs")
    public ApiResponse<List<Map<String, Object>>> getRecentRuns(
            @RequestParam(defaultValue = "5") Integer limit) {
        Long tenantId = TenantContext.getTenantId();
        List<WorkflowRun> runs = workflowExecutionService.list(
                new LambdaQueryWrapper<WorkflowRun>()
                        .eq(WorkflowRun::getTenantId, tenantId)
                        .orderByDesc(WorkflowRun::getCreatedAt)
                        .last("LIMIT " + limit));

        // Collect workflowIds to resolve names
        Set<Long> workflowIds = new HashSet<>();
        for (WorkflowRun run : runs) {
            if (run.getWorkflowId() != null) workflowIds.add(run.getWorkflowId());
        }
        Map<Long, String> workflowNames = new HashMap<>();
        if (!workflowIds.isEmpty()) {
            List<WorkflowDefinition> workflows = workflowService.listByIds(workflowIds);
            for (WorkflowDefinition wf : workflows) {
                workflowNames.put(wf.getId(), wf.getName());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkflowRun run : runs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", run.getId());
            item.put("workflowId", run.getWorkflowId());
            item.put("workflowName", workflowNames.getOrDefault(run.getWorkflowId(), "工作流 #" + run.getWorkflowId()));
            item.put("status", run.getState() != null ? run.getState().name().toLowerCase() : "unknown");
            item.put("startedAt", run.getStartTime());
            item.put("duration", run.getDurationMs());
            result.add(item);
        }

        return ApiResponse.success(result);
    }

    @GetMapping("/run-status-distribution")
    public ApiResponse<List<Map<String, Object>>> getRunStatusDistribution() {
        Long tenantId = TenantContext.getTenantId();
        List<WorkflowRun> runs = workflowExecutionService.list(
                new LambdaQueryWrapper<WorkflowRun>().eq(WorkflowRun::getTenantId, tenantId));

        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("success", 0);
        distribution.put("failed", 0);
        distribution.put("running", 0);
        distribution.put("pending", 0);
        for (WorkflowRun run : runs) {
            String state = run.getState() != null ? run.getState().name().toLowerCase() : "unknown";
            distribution.merge(state, 1, Integer::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        distribution.forEach((status, count) -> {
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("status", status);
                item.put("count", count);
                result.add(item);
            }
        });

        return ApiResponse.success(result);
    }
}
