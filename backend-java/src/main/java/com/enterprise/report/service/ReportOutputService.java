package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.ReportOutput;

public interface ReportOutputService extends IService<ReportOutput> {
    String getDownloadUrl(Long id);
}
