package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.AiPolicy;

public interface AiPolicyService extends IService<AiPolicy> {
    AiPolicy getActivePolicy(Long tenantId);
    boolean checkSqlGenerationAllowed(Long tenantId);
    boolean checkCrossDatasetJoinAllowed(Long tenantId);
}
