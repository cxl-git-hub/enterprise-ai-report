package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.DataLineage;

import java.util.List;

public interface DataLineageService extends IService<DataLineage> {
    
    /**
     * Record data lineage for an AI-generated output.
     */
    void recordLineage(Long tenantId, String refType, String refId, 
                       Long datasetId, Long schemaId, String sourceName,
                       List<String> fields, String timeRange, Long rowCount,
                       String generatedSql, Integer confidenceScore);
    
    /**
     * Get lineage records for a specific output.
     */
    List<DataLineage> getLineageForOutput(String refType, String refId);
    
    /**
     * Get all lineage records for a dataset (reverse lookup).
     */
    List<DataLineage> getLineageForDataset(Long datasetId);
}
