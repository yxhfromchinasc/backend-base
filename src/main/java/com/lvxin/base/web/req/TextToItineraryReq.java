package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文本转计划表请求
 *
 * @author lvxin
 */
@Data
public class TextToItineraryReq {
    /**
     * 文本格式的旅行计划
     */
    @NotBlank(message = "计划文本不能为空")
    private String text;
}
