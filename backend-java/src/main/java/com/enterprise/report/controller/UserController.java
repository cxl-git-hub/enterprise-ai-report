package com.enterprise.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.dto.PageResult;
import com.enterprise.report.entity.SysUser;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getEmail, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        return ApiResponse.success(PageResult.from(userService.page(new Page<>(page, size), wrapper)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysUser> get(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @PostMapping
    public ApiResponse<SysUser> create(@RequestBody SysUser user) {
        if (userService.existsByUsername(user.getUsername())) {
            throw new BusinessException(400, "Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setTenantId(TenantContext.getTenantId());
        userService.save(user);
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @PutMapping("/{id}")
    public ApiResponse<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return ApiResponse.success(user);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return ApiResponse.success();
    }
}
