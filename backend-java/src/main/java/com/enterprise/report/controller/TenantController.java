package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.Tenant;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public ApiResponse<PageResult<Tenant>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Tenant::getName, keyword)
                    .or().like(Tenant::getCode, keyword);
        }
        wrapper.orderByDesc(Tenant::getCreatedAt);
        return ApiResponse.success(PageResult.from(tenantService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Tenant> get(@PathVariable Long id) {
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            throw new BusinessException(404, "Tenant not found");
        }
        return ApiResponse.success(tenant);
    }

    @PostMapping
    public ApiResponse<Tenant> create(@RequestBody Tenant tenant) {
        if (tenantService.getByCode(tenant.getCode()) != null) {
            throw new BusinessException(400, "Tenant code already exists");
        }
        tenantService.save(tenant);
        return ApiResponse.success(tenant);
    }

    @PutMapping("/{id}")
    public ApiResponse<Tenant> update(@PathVariable Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        tenantService.updateById(tenant);
        return ApiResponse.success(tenant);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tenantService.removeById(id);
        return ApiResponse.success();
    }
}
