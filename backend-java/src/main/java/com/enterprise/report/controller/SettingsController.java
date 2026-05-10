package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.entity.SystemSetting;
import com.enterprise.report.mapper.SystemSettingMapper;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.security.UserDetailsImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SystemSettingMapper settingMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @GetMapping
    public ApiResponse<Map<String, Object>> getSettings(@AuthenticationPrincipal UserDetailsImpl user) {
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> settings = new HashMap<>();

        // Load each settings group
        for (String group : List.of("appearance", "ai", "notifications", "security", "advanced")) {
            Map<String, Object> groupSettings = loadSettingGroup(tenantId, group);
            if (!groupSettings.isEmpty()) {
                settings.put(group, groupSettings);
            }
        }

        // Apply defaults for missing groups
        applyDefaults(settings);

        return ApiResponse.success(settings);
    }

    @PutMapping("/appearance")
    public ApiResponse<Void> saveAppearance(@RequestBody Map<String, Object> appearance,
                                            @AuthenticationPrincipal UserDetailsImpl user) {
        saveSettingGroup(TenantContext.getTenantId(), "appearance", appearance);
        return ApiResponse.success();
    }

    @PutMapping("/ai")
    public ApiResponse<Void> saveAiConfig(@RequestBody Map<String, Object> aiConfig,
                                          @AuthenticationPrincipal UserDetailsImpl user) {
        saveSettingGroup(TenantContext.getTenantId(), "ai", aiConfig);
        return ApiResponse.success();
    }

    @PutMapping("/notifications")
    public ApiResponse<Void> saveNotifications(@RequestBody Map<String, Object> notifSettings,
                                               @AuthenticationPrincipal UserDetailsImpl user) {
        saveSettingGroup(TenantContext.getTenantId(), "notifications", notifSettings);
        return ApiResponse.success();
    }

    @PutMapping("/security")
    public ApiResponse<Void> saveSecurity(@RequestBody Map<String, Object> securitySettings,
                                          @AuthenticationPrincipal UserDetailsImpl user) {
        saveSettingGroup(TenantContext.getTenantId(), "security", securitySettings);
        return ApiResponse.success();
    }

    @PutMapping("/advanced")
    public ApiResponse<Void> saveAdvanced(@RequestBody Map<String, Object> advancedSettings,
                                          @AuthenticationPrincipal UserDetailsImpl user) {
        saveSettingGroup(TenantContext.getTenantId(), "advanced", advancedSettings);
        return ApiResponse.success();
    }

    @PutMapping("/clear-cache")
    public ApiResponse<Void> clearCache() {
        try {
            Set<String> keys = redisTemplate.keys("settings:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to clear settings cache: {}", e.getMessage());
        }
        return ApiResponse.success();
    }

    private Map<String, Object> loadSettingGroup(Long tenantId, String group) {
        // Try cache first
        String cacheKey = "settings:" + tenantId + ":" + group;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed: {}", e.getMessage());
        }

        List<SystemSetting> settings = settingMapper.selectList(
                new LambdaQueryWrapper<SystemSetting>()
                        .eq(SystemSetting::getTenantId, tenantId)
                        .eq(SystemSetting::getSettingGroup, group));

        Map<String, Object> result = new HashMap<>();
        for (SystemSetting setting : settings) {
            try {
                result.put(setting.getSettingKey(),
                        objectMapper.readValue(setting.getSettingValue(), Object.class));
            } catch (Exception e) {
                result.put(setting.getSettingKey(), setting.getSettingValue());
            }
        }

        // Cache for 5 minutes
        try {
            redisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(result), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache write failed: {}", e.getMessage());
        }

        return result;
    }

    private void saveSettingGroup(Long tenantId, String group, Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            try {
                String valueJson = objectMapper.writeValueAsString(entry.getValue());
                SystemSetting existing = settingMapper.selectOne(
                        new LambdaQueryWrapper<SystemSetting>()
                                .eq(SystemSetting::getTenantId, tenantId)
                                .eq(SystemSetting::getSettingGroup, group)
                                .eq(SystemSetting::getSettingKey, entry.getKey()));
                if (existing != null) {
                    existing.setSettingValue(valueJson);
                    settingMapper.updateById(existing);
                } else {
                    SystemSetting setting = new SystemSetting();
                    setting.setTenantId(tenantId);
                    setting.setSettingGroup(group);
                    setting.setSettingKey(entry.getKey());
                    setting.setSettingValue(valueJson);
                    settingMapper.insert(setting);
                }
            } catch (Exception e) {
                log.error("Failed to save setting {}:{}: {}", group, entry.getKey(), e.getMessage());
            }
        }
        // Invalidate cache
        try {
            redisTemplate.delete("settings:" + tenantId + ":" + group);
        } catch (Exception e) {
            log.warn("Cache invalidation failed: {}", e.getMessage());
        }
    }

    private void applyDefaults(Map<String, Object> settings) {
        if (!settings.containsKey("appearance")) {
            Map<String, Object> appearance = new HashMap<>();
            appearance.put("theme", "light");
            appearance.put("primaryColor", "#1677ff");
            appearance.put("sidebarPosition", "left");
            appearance.put("enableAnimation", true);
            appearance.put("compactMode", false);
            settings.put("appearance", appearance);
        }
        if (!settings.containsKey("ai")) {
            Map<String, Object> ai = new HashMap<>();
            ai.put("model", "gpt-4");
            ai.put("baseUrl", "https://api.openai.com/v1");
            ai.put("temperature", 0.7);
            ai.put("maxTokens", 4096);
            ai.put("timeout", 60);
            ai.put("dailyLimit", 10000);
            settings.put("ai", ai);
        }
        if (!settings.containsKey("security")) {
            Map<String, Object> security = new HashMap<>();
            security.put("sessionTimeout", 120);
            security.put("twoFactor", false);
            settings.put("security", security);
        }
        if (!settings.containsKey("advanced")) {
            Map<String, Object> advanced = new HashMap<>();
            advanced.put("dataRetention", 90);
            advanced.put("maxConcurrentWorkflows", 5);
            advanced.put("maxReportStorage", 1024);
            advanced.put("debugMode", false);
            settings.put("advanced", advanced);
        }
    }
}
