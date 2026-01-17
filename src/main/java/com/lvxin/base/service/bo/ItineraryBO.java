package com.lvxin.base.service.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 计划表业务对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryBO {

    /**
     * 计划表ID（数据库主键）
     */
    private Long id;

    /**
     * 计划表唯一标识（Key，UUID格式，前端使用）
     */
    private String itineraryKey;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 计划表标题
     */
    private String title;

    /**
     * 封面图片URL
     */
    private String coverImageUrl;

    /**
     * 计划表JSON数据
     */
    private String jsonData;

    /**
     * 用户版本号
     */
    private Integer version;

    /**
     * 组件版本号
     */
    private String componentVersion;

    /**
     * 是否是当前使用版本（0-历史版本，1-当前版本）
     */
    private Integer isCurrentVersion;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
