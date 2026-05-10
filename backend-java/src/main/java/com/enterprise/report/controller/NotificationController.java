package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.entity.Notification;
import com.enterprise.report.mapper.NotificationMapper;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationMapper notificationMapper;

    @GetMapping
    public ApiResponse<List<Notification>> list(
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean read,
            @AuthenticationPrincipal UserDetailsImpl user) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTenantId, tenantId)
                .eq(Notification::getUserId, user.getId());
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notification::getType, type);
        }
        if (read != null) {
            wrapper.eq(Notification::getIsRead, read ? 1 : 0);
        }
        wrapper.orderByDesc(Notification::getCreatedAt)
                .last("LIMIT " + limit);
        return ApiResponse.success(notificationMapper.selectList(wrapper));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount(@AuthenticationPrincipal UserDetailsImpl user) {
        Long tenantId = TenantContext.getTenantId();
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTenantId, tenantId)
                        .eq(Notification::getUserId, user.getId())
                        .eq(Notification::getIsRead, 0));
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal UserDetailsImpl user) {
        Long tenantId = TenantContext.getTenantId();
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTenantId, tenantId)
                        .eq(Notification::getUserId, user.getId())
                        .eq(Notification::getIsRead, 0));
        return ApiResponse.success();
    }

    @PutMapping("/clear")
    public ApiResponse<Void> clearAll(@AuthenticationPrincipal UserDetailsImpl user) {
        Long tenantId = TenantContext.getTenantId();
        notificationMapper.delete(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getTenantId, tenantId)
                        .eq(Notification::getUserId, user.getId()));
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationMapper.deleteById(id);
        return ApiResponse.success();
    }
}
