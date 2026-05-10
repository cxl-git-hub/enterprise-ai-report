package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.auth.*;
import com.enterprise.report.security.JwtTokenProvider;
import com.enterprise.report.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 前端发送 { refreshToken: string } 在body中
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ApiResponse.error(400, "refreshToken is required");
        }
        return ApiResponse.success(authService.refreshToken(refreshToken));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = tokenProvider.getUserIdFromToken(token);
        return ApiResponse.success(authService.getCurrentUser(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, Object> profile,
                                           @RequestHeader("Authorization") String authorization) {
        // In production, update user profile
        return ApiResponse.success();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> passwordData,
                                            @RequestHeader("Authorization") String authorization) {
        // In production, verify old password and update
        return ApiResponse.success();
    }

    @PutMapping("/notification-preferences")
    public ApiResponse<Void> saveNotificationPreferences(@RequestBody Map<String, Object> prefs,
                                                         @RequestHeader("Authorization") String authorization) {
        // In production, save user notification preferences
        return ApiResponse.success();
    }

    @GetMapping("/my-stats")
    public ApiResponse<Map<String, Object>> getMyStats(@RequestHeader("Authorization") String authorization) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("kpiCount", 0);
        stats.put("workflowCount", 0);
        stats.put("reportCount", 0);
        return ApiResponse.success(stats);
    }
}
