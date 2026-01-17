package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建或更新计划表请求
 */
@Data
public class UpsertItineraryReq {

    /**
     * 计划表唯一标识（Key）
     * 新增时：为null或空字符串
     * 更新时：必须提供有效的itineraryKey
     */
    private String itineraryKey;

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

    /**
     * 当前版本号（用于并发控制）
     * 新增时：不需要提供
     * 更新时：必须提供当前版本号，后端会校验是否为最新版本
     */
    private Integer currentVersion;
}
