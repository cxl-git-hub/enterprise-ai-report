package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.Dataset;
import com.enterprise.report.entity.DatasetColumn;
import java.util.List;
import java.util.Map;

public interface DatasetService extends IService<Dataset> {
    List<DatasetColumn> getColumns(Long datasetId);
    void syncColumns(Long datasetId);
    Map<String, Object> preview(Long datasetId, Integer limit);
}
