package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建计划表请求
 */
@Data
public class CreateItineraryReq {

    /**
     * 计划表标题
     */
    @NotBlank(message = "计划表标题不能为空")
    private String title;

    /**
     * 封面图片URL（已废弃，保留用于兼容）
     * @deprecated 使用 coverImageUrls 代替
     */
    @Deprecated
    private String coverImageUrl;

    /**
     * 封面图片URL列表（可选）
     */
    private List<String> coverImageUrls;

    /**
     * 计划表JSON数据
     */
    @NotBlank(message = "计划表JSON数据不能为空")
    private String jsonData;
}
