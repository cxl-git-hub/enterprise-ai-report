package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean read,
            @AuthenticationPrincipal UserDetailsImpl user) {
        // In production, query from notification table
        List<Map<String, Object>> notifications = new ArrayList<>();
        Map<String, Object> sample = new HashMap<>();
        sample.put("id", "1");
        sample.put("type", "info");
        sample.put("message", "欢迎使用 Enterprise AI Report 平台");
        sample.put("read", false);
        sample.put("createdAt", new Date().toString());
        sample.put("link", "/dashboard");
        notifications.add(sample);
        return ApiResponse.success(notifications);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable String id) {
        // In production, update notification record
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal UserDetailsImpl user) {
        // In production, update all notifications for user
        return ApiResponse.success();
    }

    @PutMapping("/clear")
    public ApiResponse<Void> clearAll(@AuthenticationPrincipal UserDetailsImpl user) {
        // In production, delete all notifications for user
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        // In production, delete notification record
        return ApiResponse.success();
    }
}
