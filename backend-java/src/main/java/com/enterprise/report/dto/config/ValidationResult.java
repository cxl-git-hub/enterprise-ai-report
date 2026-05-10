package com.enterprise.report.dto.config;

import lombok.Data;
import java.util.List;

@Data
public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;

    @Data
    public static class ValidationError {
        private String type;       // schema/kpi/workflow/prompt/report
        private String message;
        private String refId;
        private String refName;
    }

    @Data
    public static class ValidationWarning {
        private String type;
        private String message;
        private String refId;
        private String refName;
    }

    public static ValidationResult success() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setErrors(List.of());
        result.setWarnings(List.of());
        return result;
    }
}
