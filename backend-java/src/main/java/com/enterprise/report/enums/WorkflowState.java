package com.enterprise.report.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkflowState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    PAUSED,
    CANCELLED;

    @JsonCreator
    public static WorkflowState fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return switch (value.toLowerCase()) {
                case "pending", "waiting" -> PENDING;
                case "running", "executing" -> RUNNING;
                case "success", "completed", "done" -> SUCCESS;
                case "failed", "error" -> FAILED;
                case "retrying" -> RETRYING;
                case "paused" -> PAUSED;
                case "cancelled", "canceled" -> CANCELLED;
                default -> PENDING;
            };
        }
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
