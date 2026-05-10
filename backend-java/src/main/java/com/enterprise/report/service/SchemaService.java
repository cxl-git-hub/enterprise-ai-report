package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.SchemaDefinition;
import java.util.List;

public interface SchemaService extends IService<SchemaDefinition> {
    List<SchemaDefinition> getVersions(Long schemaId);
    SchemaDefinition activateVersion(Long schemaId, Integer version);
}
