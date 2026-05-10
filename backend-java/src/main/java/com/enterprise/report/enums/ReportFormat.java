package com.enterprise.report.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportFormat {
    WORD,
    PPT,
    PDF,
    EXCEL,
    HTML;

    @JsonCreator
    public static ReportFormat fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return switch (value.toLowerCase()) {
                case "pdf" -> PDF;
                case "word", "docx", "doc" -> WORD;
                case "ppt", "pptx" -> PPT;
                case "excel", "xlsx", "xls" -> EXCEL;
                case "html", "htm" -> HTML;
                default -> PDF;
            };
        }
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
