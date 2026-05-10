package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.AiPolicy;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.AiPolicyMapper;
import com.enterprise.report.service.AiPolicyService;
import org.springframework.stereotype.Service;

@Service
public class AiPolicyServiceImpl extends ServiceImpl<AiPolicyMapper, AiPolicy> implements AiPolicyService {

    @Override
    public AiPolicy getActivePolicy(Long tenantId) {
        AiPolicy policy = getOne(new LambdaQueryWrapper<AiPolicy>()
                .eq(AiPolicy::getTenantId, tenantId)
                .eq(AiPolicy::getStatus, 1)
                .last("LIMIT 1"));
        if (policy == null) {
            AiPolicy defaultPolicy = new AiPolicy();
            defaultPolicy.setTenantId(tenantId);
            defaultPolicy.setName("Default Policy");
            defaultPolicy.setAllowSqlGeneration(true);
            defaultPolicy.setAllowCrossDatasetJoin(false);
            defaultPolicy.setAllowDataModification(false);
            defaultPolicy.setMaxRowsReturned(1000);
            defaultPolicy.setMaxExecutionTime(30);
            defaultPolicy.setStatus(1);
            save(defaultPolicy);
            return defaultPolicy;
        }
        return policy;
    }

    @Override
    public boolean checkSqlGenerationAllowed(Long tenantId) {
        AiPolicy policy = getActivePolicy(tenantId);
        return Boolean.TRUE.equals(policy.getAllowSqlGeneration());
    }

    @Override
    public boolean checkCrossDatasetJoinAllowed(Long tenantId) {
        AiPolicy policy = getActivePolicy(tenantId);
        return Boolean.TRUE.equals(policy.getAllowCrossDatasetJoin());
    }
}
