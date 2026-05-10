package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.Tenant;

public interface TenantService extends IService<Tenant> {
    Tenant getByCode(String code);
}
