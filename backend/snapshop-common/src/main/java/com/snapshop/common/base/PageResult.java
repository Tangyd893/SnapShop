package com.snapshop.common.base;

import lombok.Data;

import java.util.List;

/**
 * 分页响应模型
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long pageNo;
    private long pageSize;
    private long total;
    private long pages;

    public static <T> PageResult<T> of(List<T> records, long pageNo, long pageSize, long total) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.pageNo = pageNo;
        result.pageSize = pageSize;
        result.total = total;
        result.pages = (total + pageSize - 1) / pageSize;
        return result;
    }
}
