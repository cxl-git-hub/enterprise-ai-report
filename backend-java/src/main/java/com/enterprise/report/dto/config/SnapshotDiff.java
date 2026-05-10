package com.enterprise.report.dto.config;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SnapshotDiff {
    private String snapshot1Name;
    private String snapshot2Name;
    private LocalDateTime snapshot1Time;
    private LocalDateTime snapshot2Time;
    private List<Change> changes;
    private int totalChanges;

    @Data
    public static class Change {
        private String type;       // added/removed/modified/changed
        private String path;       // e.g. "kpi.order_count"
        private String section;    // e.g. "kpis", "schemas"
        private Object oldValue;
        private Object newValue;
    }
}
