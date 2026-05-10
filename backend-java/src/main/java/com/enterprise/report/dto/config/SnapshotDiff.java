package com.enterprise.report.dto.config;

import lombok.Data;
import java.util.List;

@Data
public class SnapshotDiff {
    private List<Change> changes;

    @Data
    public static class Change {
        private String type;       // added/removed/modified
        private String path;       // e.g. "kpi.order_count"
        private Object oldValue;
        private Object newValue;
    }
}
