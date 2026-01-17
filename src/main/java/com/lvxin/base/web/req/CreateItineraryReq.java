package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
     * 封面图片URL（可选）
     */
    private String coverImageUrl;

    /**
     * 计划表JSON数据
     */
    @NotBlank(message = "计划表JSON数据不能为空")
    private String jsonData;
}
