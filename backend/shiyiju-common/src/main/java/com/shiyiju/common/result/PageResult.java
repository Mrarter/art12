package com.shiyiju.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private Long total;

    /** 当前页 */
    private Integer page;

    /** 每页大小 */
    private Integer pageSize;

    /** 总页数 */
    private Integer totalPages;

    /** 数据列表 */
    private List<T> records;

    public static <T> PageResult<T> of(Long total, Integer page, Integer pageSize, List<T> records) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotalPages(totalPages);
        result.setRecords(records);
        return result;
    }

    public static <T> PageResult<T> empty(Integer page, Integer pageSize) {
        return new PageResult<>(0L, page, pageSize, 0, List.of());
    }
}
