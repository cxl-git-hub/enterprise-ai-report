package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.AuditLog;
import com.enterprise.report.mapper.AuditLogMapper;
import com.enterprise.report.service.AuditService;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditService {

    @Override
    public void log(Long tenantId, Long userId, String username, String action,
                    String resourceType, Long resourceId, String resourceName,
                    String details, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(tenantId);
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setResourceName(resourceName);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setStatus(1);
        save(auditLog);
    }
}
