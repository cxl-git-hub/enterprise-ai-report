package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("parent_id")
    private Long parentId;
    @TableField("perm_code")
    private String permCode;
    @TableField("perm_name")
    private String permName;
    @TableField("perm_type")
    private String permType;
    private String path;
    private String icon;
    @TableField("sort_order")
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
