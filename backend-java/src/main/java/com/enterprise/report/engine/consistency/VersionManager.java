package com.enterprise.report.engine.consistency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.entity.ConfigVersion;
import com.enterprise.report.enums.ConfigType;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.ConfigVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VersionManager {

    private final ConfigVersionMapper configVersionMapper;
    private final ObjectMapper objectMapper;

    public <T> void saveVersion(ConfigType configType, Long configId, Integer version, T entity, Long userId) {
        try {
            ConfigVersion configVersion = new ConfigVersion();
            configVersion.setTenantId(userId);
            configVersion.setConfigType(configType);
            configVersion.setConfigId(configId);
            configVersion.setVersion(version);
            configVersion.setConfigData(objectMapper.writeValueAsString(entity));
            configVersion.setCreatedBy(userId);
            configVersionMapper.insert(configVersion);
        } catch (Exception e) {
            log.error("Failed to save config version: {}", e.getMessage());
        }
    }

    public <T> T getVersion(ConfigType configType, Long configId, Integer version, Class<T> clazz) {
        ConfigVersion configVersion = configVersionMapper.selectOne(
                new LambdaQueryWrapper<ConfigVersion>()
                        .eq(ConfigVersion::getConfigType, configType)
                        .eq(ConfigVersion::getConfigId, configId)
                        .eq(ConfigVersion::getVersion, version));
        if (configVersion == null) {
            throw new BusinessException(404, "Version not found");
        }
        try {
            return objectMapper.readValue(configVersion.getConfigData(), clazz);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to deserialize version: " + e.getMessage());
        }
    }

    public List<ConfigVersion> getVersionHistory(ConfigType configType, Long configId) {
        return configVersionMapper.selectList(
                new LambdaQueryWrapper<ConfigVersion>()
                        .eq(ConfigVersion::getConfigType, configType)
                        .eq(ConfigVersion::getConfigId, configId)
                        .orderByDesc(ConfigVersion::getVersion));
    }

    public Integer getNextVersion(ConfigType configType, Long configId) {
        ConfigVersion latest = configVersionMapper.selectOne(
                new LambdaQueryWrapper<ConfigVersion>()
                        .eq(ConfigVersion::getConfigType, configType)
                        .eq(ConfigVersion::getConfigId, configId)
                        .orderByDesc(ConfigVersion::getVersion)
                        .last("LIMIT 1"));
        return latest != null ? latest.getVersion() + 1 : 1;
    }
}
