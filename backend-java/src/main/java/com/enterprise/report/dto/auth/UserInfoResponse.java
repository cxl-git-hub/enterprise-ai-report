package com.enterprise.report.dto.auth;

import lombok.Data;
import java.util.List;

@Data
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private String realName;
    private Long tenantId;
    private List<String> roles;
    private List<String> permissions;
}
