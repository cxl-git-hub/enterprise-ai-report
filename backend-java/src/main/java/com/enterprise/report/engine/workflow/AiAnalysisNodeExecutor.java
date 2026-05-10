package com.enterprise.report.engine.workflow;

import com.enterprise.report.entity.AiExecutionTrace;
import com.enterprise.report.entity.WorkflowNodeRun;
import com.enterprise.report.enums.WorkflowState;
import com.enterprise.report.mapper.AiExecutionTraceMapper;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.AiPolicyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisNodeExecutor implements NodeExecutor {

    private final AiPolicyService aiPolicyService;
    private final AiExecutionTraceMapper traceMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Override
    public String getNodeType() {
        return "ai_analysis";
    }

    @Override
    public void execute(WorkflowNodeRun nodeRun, Map<String, Object> context) {
        nodeRun.setState(WorkflowState.RUNNING);
        nodeRun.setStartedAt(LocalDateTime.now());

        Long tenantId = TenantContext.getTenantId();

        AiExecutionTrace trace = new AiExecutionTrace();
        trace.setTenantId(tenantId);
        trace.setOperationType("workflow_analysis");
        trace.setStatus(0);

        try {
            if (!aiPolicyService.checkSqlGenerationAllowed(tenantId)) {
                throw new RuntimeException("SQL generation is not allowed by AI policy");
            }

            Map<String, Object> params = new HashMap<>();
            if (nodeRun.getInputParams() != null) {
                params = objectMapper.readValue(nodeRun.getInputParams(),
                        new TypeReference<Map<String, Object>>() {});
            }
            params.putAll(context);

            trace.setInputPrompt(params.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    aiServiceUrl + "/api/analyze",
                    HttpMethod.POST,
                    request,
                    String.class);

            trace.setAiResponse(response.getBody());
            trace.setStatus(1);

            nodeRun.setOutputResult(response.getBody());
            nodeRun.setState(WorkflowState.SUCCESS);
            context.put("ai_result", response.getBody());
        } catch (Exception e) {
            log.error("AI analysis node failed: {}", e.getMessage());
            nodeRun.setState(WorkflowState.FAILED);
            nodeRun.setErrorMessage(e.getMessage());
            trace.setErrorMessage(e.getMessage());
            trace.setStatus(-1);
        } finally {
            nodeRun.setFinishedAt(LocalDateTime.now());
            if (nodeRun.getStartedAt() != null) {
                nodeRun.setDurationMs(java.time.Duration.between(nodeRun.getStartedAt(), nodeRun.getFinishedAt()).toMillis());
            }
            trace.setDurationMs(nodeRun.getDurationMs());
            traceMapper.insert(trace);
        }
    }
}
