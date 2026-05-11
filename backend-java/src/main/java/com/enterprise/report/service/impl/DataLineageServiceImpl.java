package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.DataLineage;
import com.enterprise.report.mapper.DataLineageMapper;
import com.enterprise.report.service.DataLineageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataLineageServiceImpl extends ServiceImpl<DataLineageMapper, DataLineage> 
        implements DataLineageService {

    @Override
    public void recordLineage(Long tenantId, String refType, String refId,
                               Long datasetId, Long schemaId, String sourceName,
                               List<String> fields, String timeRange, Long rowCount,
                               String generatedSql, Integer confidenceScore) {
        DataLineage lineage = new DataLineage();
        lineage.setTenantId(tenantId);
        lineage.setRefType(refType);
        lineage.setRefId(refId);
        lineage.setDatasetId(datasetId);
        lineage.setSchemaId(schemaId);
        lineage.setSourceName(sourceName);
        lineage.setFields(fields != null ? String.join(",", fields) : null);
        lineage.setTimeRange(timeRange);
        lineage.setRowCount(rowCount);
        lineage.setGeneratedSql(generatedSql);
        lineage.setConfidenceScore(confidenceScore);
        lineage.setQuality(assessQuality(rowCount));
        save(lineage);
    }

    @Override
    public List<DataLineage> getLineageForOutput(String refType, String refId) {
        return list(new LambdaQueryWrapper<DataLineage>()
                .eq(DataLineage::getRefType, refType)
                .eq(DataLineage::getRefId, refId));
    }

    @Override
    public List<DataLineage> getLineageForDataset(Long datasetId) {
        return list(new LambdaQueryWrapper<DataLineage>()
                .eq(DataLineage::getDatasetId, datasetId)
                .orderByDesc(DataLineage::getCreatedAt));
    }

    private String assessQuality(Long rowCount) {
        if (rowCount == null) return "unknown";
        if (rowCount >= 1000) return "high";
        if (rowCount >= 100) return "medium";
        return "low";
    }
}
