package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.auth.*;
import com.enterprise.report.entity.SysUser;
import com.enterprise.report.security.JwtTokenProvider;
import com.enterprise.report.service.AuthService;
import com.enterprise.report.service.UserService;
import com.enterprise.report.mapper.KpiDefinitionMapper;
import com.enterprise.report.mapper.WorkflowDefinitionMapper;
import com.enterprise.report.mapper.ReportOutputMapper;
import com.enterprise.report.mapper.SystemSettingMapper;
import com.enterprise.report.entity.SystemSetting;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.enterprise.report.entity.KpiDefinition;
import com.enterprise.report.entity.WorkflowDefinition;
import com.enterprise.report.entity.ReportOutput;
import com.enterprise.report.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final KpiDefinitionMapper kpiDefinitionMapper;
    private final WorkflowDefinitionMapper workflowDefinitionMapper;
    private final ReportOutputMapper reportOutputMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final ObjectMapper objectMapper;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ApiResponse.error(400, "refreshToken is required");
        }
        return ApiResponse.success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = tokenProvider.getUserIdFromToken(token);
        return ApiResponse.success(authService.getCurrentUser(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, Object> profile,
                                           @RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = tokenProvider.getUserIdFromToken(token);
        SysUser user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.error(404, "User not found");
        }
        if (profile.containsKey("displayName")) {
            user.setRealName((String) profile.get("displayName"));
        }
        if (profile.containsKey("realName")) {
            user.setRealName((String) profile.get("realName"));
        }
        if (profile.containsKey("email")) {
            user.setEmail((String) profile.get("email"));
        }
        if (profile.containsKey("phone")) {
            user.setPhone((String) profile.get("phone"));
        }
        userService.updateById(user);
        return ApiResponse.success();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> passwordData,
                                            @RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = tokenProvider.getUserIdFromToken(token);
        SysUser user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.error(404, "User not found");
        }
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return ApiResponse.error(400, "Old and new passwords are required");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ApiResponse.error(400, "Old password is incorrect");
        }
        if (newPassword.length() < 6) {
            return ApiResponse.error(400, "New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        return ApiResponse.success();
    }

    @PutMapping("/notification-preferences")
    public ApiResponse<Void> saveNotificationPreferences(@RequestBody Map<String, Object> prefs,
                                                         @RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = tokenProvider.getUserIdFromToken(token);
        Long tenantId = TenantContext.getTenantId();

        try {
            String prefsJson = objectMapper.writeValueAsString(prefs);
            SystemSetting existing = systemSettingMapper.selectOne(
                    new LambdaQueryWrapper<SystemSetting>()
                            .eq(SystemSetting::getTenantId, tenantId)
                            .eq(SystemSetting::getSettingGroup, "user_notifications")
                            .eq(SystemSetting::getSettingKey, "user_" + userId));
            if (existing != null) {
                existing.setSettingValue(prefsJson);
                systemSettingMapper.updateById(existing);
            } else {
                SystemSetting setting = new SystemSetting();
                setting.setTenantId(tenantId);
                setting.setSettingGroup("user_notifications");
                setting.setSettingKey("user_" + userId);
                setting.setSettingValue(prefsJson);
                systemSettingMapper.insert(setting);
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "Failed to save preferences: " + e.getMessage());
        }
        return ApiResponse.success();
    }

    @GetMapping("/my-stats")
    public ApiResponse<Map<String, Object>> getMyStats(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = tokenProvider.getUserIdFromToken(token);
        Long tenantId = TenantContext.getTenantId();

        Map<String, Object> stats = new HashMap<>();
        // Count tenant-level stats (createdBy filtering requires DB migration for created_by column)
        stats.put("kpiCount", kpiDefinitionMapper.selectCount(
                new LambdaQueryWrapper<KpiDefinition>()
                        .eq(KpiDefinition::getTenantId, tenantId)));
        stats.put("workflowCount", workflowDefinitionMapper.selectCount(
                new LambdaQueryWrapper<WorkflowDefinition>()
                        .eq(WorkflowDefinition::getTenantId, tenantId)));
        stats.put("reportCount", reportOutputMapper.selectCount(
                new LambdaQueryWrapper<ReportOutput>()
                        .eq(ReportOutput::getTenantId, tenantId)));
        return ApiResponse.success(stats);
    }
}
