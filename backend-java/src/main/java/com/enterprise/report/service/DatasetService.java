package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.Dataset;
import com.enterprise.report.entity.DatasetColumn;
import java.util.List;

public interface DatasetService extends IService<Dataset> {
    List<DatasetColumn> getColumns(Long datasetId);
    void syncColumns(Long datasetId);
}
