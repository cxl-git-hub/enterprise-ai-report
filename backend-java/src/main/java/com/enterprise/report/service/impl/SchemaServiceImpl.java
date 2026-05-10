package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.SchemaDefinition;
import com.enterprise.report.entity.ConfigVersion;
import com.enterprise.report.enums.ConfigType;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.SchemaDefinitionMapper;
import com.enterprise.report.mapper.ConfigVersionMapper;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.SchemaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaServiceImpl extends ServiceImpl<SchemaDefinitionMapper, SchemaDefinition> implements SchemaService {

    private final ConfigVersionMapper configVersionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public boolean save(SchemaDefinition entity) {
        if (entity.getId() == null) {
            entity.setVersion(1);
            entity.setStatus(1);
            boolean saved = super.save(entity);
            if (saved) {
                saveConfigVersion(entity);
            }
            return saved;
        } else {
            SchemaDefinition existing = getById(entity.getId());
            if (existing == null) {
                throw new BusinessException(404, "Schema not found");
            }
            entity.setVersion(existing.getVersion() + 1);
            boolean updated = updateById(entity);
            if (updated) {
                saveConfigVersion(entity);
            }
            return updated;
        }
    }

    @Override
    public List<SchemaDefinition> getVersions(Long schemaId) {
        List<ConfigVersion> versions = configVersionMapper.selectList(
                new LambdaQueryWrapper<ConfigVersion>()
                        .eq(ConfigVersion::getConfigType, ConfigType.SCHEMA)
                        .eq(ConfigVersion::getConfigId, schemaId)
                        .orderByDesc(ConfigVersion::getVersion));
        return versions.stream()
                .map(v -> {
                    try {
                        return objectMapper.readValue(v.getConfigData(), SchemaDefinition.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize schema version: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(s -> s != null)
                .toList();
    }

    @Override
    @Transactional
    public SchemaDefinition activateVersion(Long schemaId, Integer version) {
        ConfigVersion configVersion = configVersionMapper.selectOne(
                new LambdaQueryWrapper<ConfigVersion>()
                        .eq(ConfigVersion::getConfigType, ConfigType.SCHEMA)
                        .eq(ConfigVersion::getConfigId, schemaId)
                        .eq(ConfigVersion::getVersion, version));
        if (configVersion == null) {
            throw new BusinessException(404, "Version not found");
        }

        try {
            SchemaDefinition schema = objectMapper.readValue(configVersion.getConfigData(), SchemaDefinition.class);
            schema.setStatus(1);
            super.updateById(schema);
            return schema;
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to activate version: " + e.getMessage());
        }
    }

    private void saveConfigVersion(SchemaDefinition entity) {
        try {
            ConfigVersion version = new ConfigVersion();
            version.setTenantId(entity.getTenantId());
            version.setConfigType(ConfigType.SCHEMA);
            version.setConfigId(entity.getId());
            version.setVersion(entity.getVersion());
            version.setConfigData(objectMapper.writeValueAsString(entity));
            version.setCreatedBy(TenantContext.getTenantId());
            configVersionMapper.insert(version);
        } catch (Exception e) {
            log.error("Failed to save config version: {}", e.getMessage());
        }
    }
}
