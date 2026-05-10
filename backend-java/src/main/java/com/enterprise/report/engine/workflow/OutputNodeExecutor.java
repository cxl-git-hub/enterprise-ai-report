package com.enterprise.report.engine.workflow;

import com.enterprise.report.entity.ReportOutput;
import com.enterprise.report.entity.WorkflowNodeRun;
import com.enterprise.report.enums.ReportFormat;
import com.enterprise.report.enums.WorkflowState;
import com.enterprise.report.engine.output.ReportGenerator;
import com.enterprise.report.mapper.ReportOutputMapper;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.MinioService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutputNodeExecutor implements NodeExecutor {

    private final ReportGenerator reportGenerator;
    private final MinioService minioService;
    private final ReportOutputMapper reportOutputMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "output";
    }

    @Override
    public void execute(WorkflowNodeRun nodeRun, Map<String, Object> context) {
        nodeRun.setState(WorkflowState.RUNNING);
        nodeRun.setStartedAt(LocalDateTime.now());

        try {
            Map<String, Object> params = new HashMap<>();
            if (nodeRun.getInputParams() != null) {
                params = objectMapper.readValue(nodeRun.getInputParams(),
                        new TypeReference<Map<String, Object>>() {});
            }
            params.putAll(context);

            String format = (String) params.getOrDefault("format", "PDF");
            String templateName = (String) params.getOrDefault("template", "default");
            ReportFormat reportFormat = ReportFormat.valueOf(format.toUpperCase());

            byte[] content = reportGenerator.generate(reportFormat, templateName, context);

            String fileName = "report_" + System.currentTimeMillis() + "." + format.toLowerCase();
            String objectKey = "reports/" + TenantContext.getTenantId() + "/" + fileName;

            minioService.uploadFile(objectKey, new ByteArrayInputStream(content),
                    getContentType(reportFormat));

            ReportOutput output = new ReportOutput();
            output.setTenantId(TenantContext.getTenantId());
            output.setName(fileName);
            output.setFormat(reportFormat);
            output.setFileKey(objectKey);
            output.setFileName(fileName);
            output.setFileSize((long) content.length);
            output.setStatus(1);
            reportOutputMapper.insert(output);

            Map<String, Object> result = new HashMap<>();
            result.put("outputId", output.getId());
            result.put("fileKey", objectKey);
            result.put("fileName", fileName);
            nodeRun.setOutputResult(objectMapper.writeValueAsString(result));
            nodeRun.setState(WorkflowState.SUCCESS);
            context.put("report_output", result);
        } catch (Exception e) {
            log.error("Output node failed: {}", e.getMessage());
            nodeRun.setState(WorkflowState.FAILED);
            nodeRun.setErrorMessage(e.getMessage());
        } finally {
            nodeRun.setFinishedAt(LocalDateTime.now());
            if (nodeRun.getStartedAt() != null) {
                nodeRun.setDurationMs(java.time.Duration.between(nodeRun.getStartedAt(), nodeRun.getFinishedAt()).toMillis());
            }
        }
    }

    private String getContentType(ReportFormat format) {
        return switch (format) {
            case WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case PPT -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case PDF -> "application/pdf";
        };
    }
}
