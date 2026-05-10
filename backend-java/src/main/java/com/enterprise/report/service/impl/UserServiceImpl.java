package com.enterprise.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.report.entity.SysRole;
import com.enterprise.report.entity.SysUser;
import com.enterprise.report.entity.SysUserRole;
import com.enterprise.report.entity.SysRolePermission;
import com.enterprise.report.entity.SysPermission;
import com.enterprise.report.mapper.SysUserMapper;
import com.enterprise.report.mapper.SysUserRoleMapper;
import com.enterprise.report.mapper.SysRoleMapper;
import com.enterprise.report.mapper.SysRolePermissionMapper;
import com.enterprise.report.mapper.SysPermissionMapper;
import com.enterprise.report.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    public List<SysRole> getUserRoles(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        List<SysRole> roles = getUserRoles(userId);
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());

        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, roleIds));
        if (rolePermissions.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());

        List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
        return permissions.stream()
                .map(SysPermission::getPermCode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0;
    }
}
