package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.DataSource;

public interface DataSourceService extends IService<DataSource> {
    boolean testConnection(Long id);
}
