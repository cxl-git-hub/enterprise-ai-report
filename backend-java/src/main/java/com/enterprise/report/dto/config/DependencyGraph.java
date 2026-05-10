package com.enterprise.report.dto.config;

import lombok.Data;
import java.util.List;

@Data
public class DependencyGraph {
    private List<DependencyNode> nodes;
    private List<DependencyEdge> edges;

    @Data
    public static class DependencyNode {
        private String id;
        private String name;
        private String type;       // schema/kpi/workflow/prompt/report_template
        private List<String> dependencies;
    }

    @Data
    public static class DependencyEdge {
        private String from;
        private String to;
        private String type;       // reference/composition/trigger
    }
}
