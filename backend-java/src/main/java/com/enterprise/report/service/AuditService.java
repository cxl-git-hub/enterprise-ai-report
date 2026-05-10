package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.AuditLog;

public interface AuditService extends IService<AuditLog> {
    void log(Long tenantId, Long userId, String username, String action, 
             String resourceType, Long resourceId, String resourceName, 
             String details, String ipAddress);
}
