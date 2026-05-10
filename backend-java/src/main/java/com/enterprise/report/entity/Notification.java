package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String type;
    private String title;
    private String message;
    private String link;
    private Integer isRead;
    private String sourceType;
    private String sourceId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
