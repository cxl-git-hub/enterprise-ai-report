package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.ReportTemplate;
import com.enterprise.report.mapper.ReportTemplateMapper;
import com.enterprise.report.service.ReportTemplateService;
import org.springframework.stereotype.Service;

@Service
public class ReportTemplateServiceImpl extends ServiceImpl<ReportTemplateMapper, ReportTemplate> implements ReportTemplateService {
}
