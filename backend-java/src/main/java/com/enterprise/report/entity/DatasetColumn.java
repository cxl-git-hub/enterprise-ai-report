package com.enterprise.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dataset_column")
public class DatasetColumn {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long datasetId;
    private String columnName;
    private String columnType;
    private String displayName;
    private Integer isPrimaryKey;
    private Integer isNullable;
    private String description;
    private String sampleValues;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
