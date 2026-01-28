package com.lvxin.base.web.req;

import lombok.Data;

import java.util.List;

/**
 * 更新计划表请求
 */
@Data
public class UpdateItineraryReq {

    /**
     * 计划表标题（可选）
     */
    private String title;

    /**
     * 封面图片URL列表（可选）
     */
    private List<String> coverImageUrls;

    /**
     * 计划表JSON数据（可选）
     */
    private String jsonData;
}
