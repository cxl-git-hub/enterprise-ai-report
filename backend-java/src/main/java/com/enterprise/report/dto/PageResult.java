package com.enterprise.report.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> items;         // 前端期望 items
    private long total;
    private long pageSize;
    private long page;
    private long pages;

    public static <T> PageResult<T> from(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setItems(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageSize(page.getSize());
        result.setPage(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }
}
