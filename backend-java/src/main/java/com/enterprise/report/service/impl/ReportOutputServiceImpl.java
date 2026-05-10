package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.ReportOutput;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.ReportOutputMapper;
import com.enterprise.report.service.MinioService;
import com.enterprise.report.service.ReportOutputService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportOutputServiceImpl extends ServiceImpl<ReportOutputMapper, ReportOutput> implements ReportOutputService {

    private final MinioService minioService;

    @Override
    public String getDownloadUrl(Long id) {
        ReportOutput output = getById(id);
        if (output == null) {
            throw new BusinessException(404, "Report output not found");
        }
        if (output.getFileKey() == null) {
            throw new BusinessException(400, "Report file not available");
        }
        return minioService.getPresignedUrl(output.getFileKey());
    }
}
