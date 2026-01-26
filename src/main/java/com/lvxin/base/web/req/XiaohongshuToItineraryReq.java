package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小红书链接转计划表请求
 *
 * @author lvxin
 */
@Data
public class XiaohongshuToItineraryReq {
    /**
     * 小红书链接
     */
    @NotBlank(message = "小红书链接不能为空")
    private String url;
}
