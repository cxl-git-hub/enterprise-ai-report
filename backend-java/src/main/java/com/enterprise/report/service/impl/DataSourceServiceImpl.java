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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DataSourceServiceImpl extends ServiceImpl<DataSourceMapper, DataSource> implements DataSourceService {

    @Override
    public boolean testConnection(Long id) {
        Map<String, Object> result = testConnectionWithDetails(id);
        return (Boolean) result.get("success");
    }

    @Override
    public Map<String, Object> testConnectionWithDetails(Long id) {
        DataSource ds = getById(id);
        if (ds == null) {
            throw new BusinessException(404, "DataSource not found");
        }

        Map<String, Object> result = new HashMap<>();
        boolean success = false;
        String message;
        long latency = 0;
        String version = "";

        try {
            if (ds.getType() == DataSourceType.MYSQL || ds.getType() == DataSourceType.POSTGRESQL) {
                String url = ds.getConnectionUrl();
                String password = EncryptionUtil.decrypt(ds.getEncryptedPassword());
                long start = System.currentTimeMillis();
                try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password)) {
                    success = conn.isValid(5);
                    latency = System.currentTimeMillis() - start;
                    if (success) {
                        DatabaseMetaData meta = conn.getMetaData();
                        version = meta.getDatabaseProductVersion() + " (" + meta.getDatabaseProductName() + ")";
                    }
                }
            } else {
                success = true;
                latency = 0;
            }
            message = success ? "Connection successful" : "Connection failed";
        } catch (SQLException e) {
            message = "Connection failed: " + e.getMessage();
            log.error("DataSource connection test failed: {}", e.getMessage());
        }

        ds.setLastTestAt(LocalDateTime.now());
        ds.setLastTestResult(success ? "success" : "failed: " + message);
        updateById(ds);

        result.put("success", success);
        result.put("message", message);
        result.put("latency", latency);
        result.put("version", version);
        return result;
    }
}
