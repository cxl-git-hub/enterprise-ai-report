package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.Dataset;
import com.enterprise.report.entity.DatasetColumn;
import com.enterprise.report.entity.DataSource;
import com.enterprise.report.enums.DataSourceType;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.DatasetMapper;
import com.enterprise.report.mapper.DatasetColumnMapper;
import com.enterprise.report.mapper.DataSourceMapper;
import com.enterprise.report.service.DatasetService;
import com.enterprise.report.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetServiceImpl extends ServiceImpl<DatasetMapper, Dataset> implements DatasetService {

    private final DatasetColumnMapper columnMapper;
    private final DataSourceMapper dataSourceMapper;

    @Override
    public List<DatasetColumn> getColumns(Long datasetId) {
        return columnMapper.selectList(
                new LambdaQueryWrapper<DatasetColumn>()
                        .eq(DatasetColumn::getDatasetId, datasetId)
                        .orderByAsc(DatasetColumn::getSort));
    }

    @Override
    @Transactional
    public void syncColumns(Long datasetId) {
        Dataset dataset = getById(datasetId);
        if (dataset == null) {
            throw new BusinessException(404, "Dataset not found");
        }

        DataSource dataSource = dataSourceMapper.selectById(dataset.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(404, "DataSource not found");
        }

        if (dataSource.getType() != DataSourceType.MYSQL && dataSource.getType() != DataSourceType.POSTGRESQL) {
            throw new BusinessException(400, "Column sync only supported for SQL databases");
        }

        String tableName = dataset.getTableName();
        List<DatasetColumn> columns = new ArrayList<>();

        try {
            String password = EncryptionUtil.decrypt(dataSource.getEncryptedPassword());
            try (Connection conn = DriverManager.getConnection(dataSource.getConnectionUrl(), dataSource.getUsername(), password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                    int sort = 0;
                    while (rs.next()) {
                        DatasetColumn column = new DatasetColumn();
                        column.setTenantId(dataset.getTenantId());
                        column.setDatasetId(datasetId);
                        column.setName(rs.getString("COLUMN_NAME"));
                        column.setDisplayName(rs.getString("COLUMN_NAME"));
                        column.setDataType(rs.getString("TYPE_NAME"));
                        column.setLength(rs.getInt("COLUMN_SIZE"));
                        column.setNullable(rs.getInt("NULLABLE"));
                        column.setDescription(rs.getString("REMARKS"));
                        column.setSort(sort++);
                        columns.add(column);
                    }
                }
            }
        } catch (SQLException e) {
            throw new BusinessException(500, "Failed to sync columns: " + e.getMessage());
        }

        columnMapper.delete(new LambdaQueryWrapper<DatasetColumn>()
                .eq(DatasetColumn::getDatasetId, datasetId));
        for (DatasetColumn column : columns) {
            columnMapper.insert(column);
        }

        dataset.setLastSyncAt(java.time.LocalDateTime.now());
        updateById(dataset);
    }
}
