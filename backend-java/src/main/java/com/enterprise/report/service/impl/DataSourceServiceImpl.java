package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.DataSource;
import com.enterprise.report.enums.DataSourceType;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.DataSourceMapper;
import com.enterprise.report.service.DataSourceService;
import com.enterprise.report.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Slf4j
@Service
public class DataSourceServiceImpl extends ServiceImpl<DataSourceMapper, DataSource> implements DataSourceService {

    @Override
    public boolean testConnection(Long id) {
        DataSource ds = getById(id);
        if (ds == null) {
            throw new BusinessException(404, "DataSource not found");
        }

        boolean success = false;
        String result;

        try {
            if (ds.getType() == DataSourceType.MYSQL || ds.getType() == DataSourceType.POSTGRESQL) {
                String url = ds.getConnectionUrl();
                String password = EncryptionUtil.decrypt(ds.getEncryptedPassword());
                try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password)) {
                    success = conn.isValid(5);
                }
            } else {
                success = true;
            }
            result = success ? "Connection successful" : "Connection failed";
        } catch (SQLException e) {
            result = "Connection failed: " + e.getMessage();
            log.error("DataSource connection test failed: {}", e.getMessage());
        }

        ds.setLastTestAt(LocalDateTime.now());
        ds.setLastTestResult(result);
        updateById(ds);

        return success;
    }
}
