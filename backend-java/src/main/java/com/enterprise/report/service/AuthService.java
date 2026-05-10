package com.enterprise.report.service;

import com.enterprise.report.dto.auth.LoginRequest;
import com.enterprise.report.dto.auth.LoginResponse;
import com.enterprise.report.dto.auth.RegisterRequest;
import com.enterprise.report.dto.auth.UserInfoResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    LoginResponse refreshToken(String refreshToken);
    UserInfoResponse getCurrentUser(Long userId);
}
