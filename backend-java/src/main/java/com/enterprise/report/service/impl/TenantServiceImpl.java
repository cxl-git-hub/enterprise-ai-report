package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.Tenant;
import com.enterprise.report.mapper.TenantMapper;
import com.enterprise.report.service.TenantService;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    @Override
    public Tenant getByCode(String code) {
        return getOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getCode, code));
    }
}
