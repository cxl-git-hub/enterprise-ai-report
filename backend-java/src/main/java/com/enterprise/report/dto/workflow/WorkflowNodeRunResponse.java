package com.enterprise.report.dto.workflow;

import com.enterprise.report.enums.WorkflowState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WorkflowNodeRunResponse {
    private Long id;
    private String runId;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private String status;           // 前端期望 status
    private String input;            // 前端期望 input (对应 input_data)
    private String output;           // 前端期望 output (对应 output_data)
    private String logs;             // 日志
    private String error;            // 前端期望 error (对应 error_message)
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long duration;
    private Long tokensUsed;
    private BigDecimal cost;
}
