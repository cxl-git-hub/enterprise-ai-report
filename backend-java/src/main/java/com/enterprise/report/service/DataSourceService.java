package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.DataSource;

import java.util.Map;

public interface DataSourceService extends IService<DataSource> {
    boolean testConnection(Long id);
    Map<String, Object> testConnectionWithDetails(Long id);
}
