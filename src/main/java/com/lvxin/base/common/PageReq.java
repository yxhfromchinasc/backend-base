package com.lvxin.base.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分页查询请求基类
 * 所有需要分页的查询请求都应该继承此类
 */
@Data
public class PageReq {

    /**
     * 页码，从1开始
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    /**
     * 页大小，默认10，最大100
     */
    @NotNull(message = "页大小不能为空")
    @Min(value = 1, message = "页大小必须大于等于1")
    private Integer pageSize = 10;

    /**
     * 获取页码的整数值
     *
     * @return 页码
     */
    public Integer getPageNumInt() {
        return pageNum;
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