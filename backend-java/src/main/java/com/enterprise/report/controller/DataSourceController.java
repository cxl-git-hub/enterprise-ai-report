package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.DataSource;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.DataSourceService;
import com.enterprise.report.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @GetMapping
    public ApiResponse<PageResult<DataSource>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DataSource::getName, keyword);
        }
        wrapper.orderByDesc(DataSource::getCreatedAt);
        Page<DataSource> result = dataSourceService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(ds -> ds.setEncryptedPassword(null));
        return ApiResponse.success(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<DataSource> get(@PathVariable Long id) {
        DataSource ds = dataSourceService.getById(id);
        if (ds == null) {
            throw new BusinessException(404, "DataSource not found");
        }
        ds.setEncryptedPassword(null);
        return ApiResponse.success(ds);
    }

    @PostMapping
    public ApiResponse<DataSource> create(@RequestBody DataSource dataSource) {
        if (dataSource.getEncryptedPassword() != null) {
            dataSource.setEncryptedPassword(EncryptionUtil.encrypt(dataSource.getEncryptedPassword()));
        }
        dataSource.setTenantId(TenantContext.getTenantId());
        dataSourceService.save(dataSource);
        dataSource.setEncryptedPassword(null);
        return ApiResponse.success(dataSource);
    }

    @PutMapping("/{id}")
    public ApiResponse<DataSource> update(@PathVariable Long id, @RequestBody DataSource dataSource) {
        dataSource.setId(id);
        if (dataSource.getEncryptedPassword() != null && !dataSource.getEncryptedPassword().isEmpty()) {
            dataSource.setEncryptedPassword(EncryptionUtil.encrypt(dataSource.getEncryptedPassword()));
        } else {
            dataSource.setEncryptedPassword(null);
        }
        dataSourceService.updateById(dataSource);
        return ApiResponse.success(dataSource);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dataSourceService.removeById(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/test")
    public ApiResponse<Boolean> testConnection(@PathVariable Long id) {
        return ApiResponse.success(dataSourceService.testConnection(id));
    }
}
