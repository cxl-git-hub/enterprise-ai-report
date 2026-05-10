package com.enterprise.report.dto.config;

import lombok.Data;

@Data
public class CreateSnapshotRequest {
    private String name;
    private String description;
}
