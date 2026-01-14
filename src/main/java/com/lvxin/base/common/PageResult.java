package com.lvxin.base.common;

import lombok.*;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public static <T> PageResult<T> empty(int page, int pageSize) {
        PageResult<T> r = new PageResult<>();
        r.setList(Collections.emptyList());
        r.setTotal(0L);
        r.setPage(page);
        r.setPageSize(pageSize);
        return r;
    }

    /**
     * 获取总数的长整型值
     *
     * @return 总数
     */
    public Long getTotalLong() {
        return total;
    }

    /**
     * 获取页码的整数值
     *
     * @return 页码
     */
    public Integer getPageInt() {
        return page;
    }

    /**
     * 获取页大小的整数值
     *
     * @return 页大小
     */
    public Integer getPageSizeInt() {
        return pageSize;
    }

}
