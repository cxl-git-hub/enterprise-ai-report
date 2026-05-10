package com.enterprise.report.service;

import com.enterprise.report.dto.PageResult;
import com.enterprise.report.dto.config.*;
import com.enterprise.report.entity.ConfigSnapshot;

import java.util.List;

public interface ConfigConsistencyService {
    List<String> validateDependencies(Long tenantId);
    ValidationResult validateStructured(Long tenantId);
    DependencyGraph getDependencyGraph(Long tenantId);
    ConfigSnapshot createSnapshot(Long tenantId, String name, String description);
    PageResult<ConfigSnapshot> getSnapshots(Long tenantId, Integer page, Integer pageSize);
    ConfigSnapshot getSnapshot(Long snapshotId);
    void rollback(Long snapshotId);
    SnapshotDiff diffSnapshots(Long id1, Long id2);
}
