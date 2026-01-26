package com.lvxin.base.web.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计划表摘要响应（用于列表展示）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarySummaryResp {

    /**
     * 计划表唯一标识（Key）
     */
    private String itineraryKey;

    /**
     * 计划表标题
     */
    private String title;

    /**
     * 封面图片URL（已废弃，保留用于兼容）
     * @deprecated 使用 coverImageUrls 代替
     */
    @Deprecated
    private String coverImageUrl;

    /**
     * 封面图片URL列表
     */
    private List<String> coverImageUrls;

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
     * 计划状态（NORMAL-正常，GENERATING-生成中，FAILED-生成失败）
     */
    private String status;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
