package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String username;
    private String password;
    @TableField("real_name")
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
    @TableField("last_login_ip")
    private String lastLoginIp;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
