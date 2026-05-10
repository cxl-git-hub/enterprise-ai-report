package com.enterprise.report.service.impl;

import com.enterprise.report.dto.auth.LoginRequest;
import com.enterprise.report.dto.auth.LoginResponse;
import com.enterprise.report.dto.auth.RegisterRequest;
import com.enterprise.report.dto.auth.UserInfoResponse;
import com.enterprise.report.entity.SysUser;
import com.enterprise.report.entity.SysRole;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.JwtTokenProvider;
import com.enterprise.report.security.TenantContext;
import com.enterprise.report.service.AuthService;
import com.enterprise.report.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "Invalid username or password");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid username or password");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "Account is disabled");
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getUsername());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getTenantId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "Username already exists");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setTenantId(request.getTenantId() != null ? request.getTenantId() : 1L);
        user.setStatus(1);
        userService.save(user);

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getUsername());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getTenantId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .build();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(401, "Invalid refresh token");
        }
        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(401, "Invalid token type");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        Long tenantId = tokenProvider.getTenantIdFromToken(refreshToken);
        String username = tokenProvider.getUsernameFromToken(refreshToken);

        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(401, "User not found or disabled");
        }

        String newAccessToken = tokenProvider.generateAccessToken(userId, tenantId, username);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId, tenantId, username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .userId(userId)
                .tenantId(tenantId)
                .username(username)
                .realName(user.getRealName())
                .build();
    }

    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }

        List<SysRole> roles = userService.getUserRoles(userId);
        List<String> permissions = userService.getUserPermissions(userId);

        UserInfoResponse info = new UserInfoResponse();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setRealName(user.getRealName());
        info.setTenantId(user.getTenantId());
        info.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        info.setPermissions(permissions);
        return info;
    }
}
