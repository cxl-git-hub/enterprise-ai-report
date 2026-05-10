package com.enterprise.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.report.entity.SysUser;
import com.enterprise.report.entity.SysRole;
import java.util.List;

public interface UserService extends IService<SysUser> {
    SysUser getByUsername(String username);
    List<SysRole> getUserRoles(Long userId);
    List<String> getUserPermissions(Long userId);
    void assignRoles(Long userId, List<Long> roleIds);
    boolean existsByUsername(String username);
}
