package com.lvxin.base.service.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置业务对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigBO {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 配置键
     */
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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

