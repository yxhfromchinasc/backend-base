package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设置配置请求
 */
@Data
public class SetConfigReq {

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型（string、number、boolean、json）
     */
    private String configType;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 是否公开（0-仅管理员可见，1-用户可见）
     */
    private Integer isPublic;
}
