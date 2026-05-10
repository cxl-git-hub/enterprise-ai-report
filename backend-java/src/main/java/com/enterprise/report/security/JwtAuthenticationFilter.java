package com.enterprise.report.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.entity.SysRole;
import com.enterprise.report.entity.SysPermission;
import com.enterprise.report.mapper.SysRoleMapper;
import com.enterprise.report.mapper.SysRolePermissionMapper;
import com.enterprise.report.mapper.SysUserRoleMapper;
import com.enterprise.report.mapper.SysPermissionMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                String tokenType = tokenProvider.getTokenType(token);
                if (!"access".equals(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = tokenProvider.getUserIdFromToken(token);
                Long tenantId = tokenProvider.getTenantIdFromToken(token);
                String username = tokenProvider.getUsernameFromToken(token);

                TenantContext.setTenantId(tenantId);

                // Load user roles and permissions from database
                List<String> roles = getUserRoles(userId);
                List<String> permissions = getUserPermissions(userId);

                UserDetailsImpl userDetails = new UserDetailsImpl(
                        userId, tenantId, username, "", "", "", 1,
                        roles, permissions
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private List<String> getUserRoles(Long userId) {
        try {
            List<Long> roleIds = userRoleMapper.selectList(
                    new LambdaQueryWrapper<com.enterprise.report.entity.SysUserRole>()
                            .eq(com.enterprise.report.entity.SysUserRole::getUserId, userId))
                    .stream()
                    .map(com.enterprise.report.entity.SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            if (roleIds.isEmpty()) return new ArrayList<>();
            return roleMapper.selectBatchIds(roleIds).stream()
                    .map(SysRole::getRoleCode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load user roles: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> getUserPermissions(Long userId) {
        try {
            List<String> roles = getUserRoles(userId);
            if (roles.contains("SUPER_ADMIN")) {
                return List.of("*"); // Super admin has all permissions
            }
            // Load permissions through role-permission mapping
            List<Long> roleIds = userRoleMapper.selectList(
                    new LambdaQueryWrapper<com.enterprise.report.entity.SysUserRole>()
                            .eq(com.enterprise.report.entity.SysUserRole::getUserId, userId))
                    .stream()
                    .map(com.enterprise.report.entity.SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            if (roleIds.isEmpty()) return new ArrayList<>();

            List<Long> permissionIds = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<com.enterprise.report.entity.SysRolePermission>()
                            .in(com.enterprise.report.entity.SysRolePermission::getRoleId, roleIds))
                    .stream()
                    .map(com.enterprise.report.entity.SysRolePermission::getPermissionId)
                    .collect(Collectors.toList());
            if (permissionIds.isEmpty()) return new ArrayList<>();

            return permissionMapper.selectBatchIds(permissionIds).stream()
                    .map(SysPermission::getPermCode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load user permissions: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
