package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.DataSource;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.DataSourceService;
import com.enterprise.report.service.MinioService;
import com.enterprise.report.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final MinioService minioService;

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

    @PostMapping("/upload")
    public ApiResponse<DataSource> uploadFile(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ext.matches("xlsx|xls|csv|json")) {
            throw new BusinessException(400, "不支持的文件格式，仅支持 Excel、CSV、JSON");
        }

        // Upload to MinIO
        String objectName = "uploads/" + TenantContext.getTenantId() + "/" + System.currentTimeMillis() + "_" + originalFilename;
        String filePath;
        try {
            filePath = minioService.uploadFile(objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }

        // Create data source record
        DataSource ds = new DataSource();
        ds.setTenantId(TenantContext.getTenantId());
        ds.setName(name);
        ds.setDescription(description != null ? description : "上传文件: " + originalFilename);
        ds.setType("file");
        ds.setConfig("{\"filePath\":\"" + filePath + "\",\"fileName\":\"" + originalFilename + "\",\"fileType\":\"" + ext + "\"}");
        ds.setStatus(1);
        dataSourceService.save(ds);

        ds.setEncryptedPassword(null);
        return ApiResponse.success(ds);
    }
}
