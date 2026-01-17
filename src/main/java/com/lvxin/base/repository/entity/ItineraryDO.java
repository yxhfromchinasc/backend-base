package com.lvxin.base.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 计划表DO - Repository层数据对象
 * 存储用户行程计划表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("itinerary")
public class ItineraryDO {

    /**
     * 计划表ID - 主键，自增
     * 作用：唯一标识计划表
     * 必填：是（系统自动生成）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 计划表唯一标识（Key）- UUID格式
     * 作用：前端使用的唯一标识，与前端JSON中的id字段对应
     * 必填：是
     * 格式：UUID字符串
     * 说明：同一个计划表的不同版本共享相同的itineraryKey（作为key），但每个版本是独立的数据库记录
     * 唯一约束：通过复合唯一键 (itinerary_key, version) 确保同一个计划表的同一个版本只有一条记录
     */
    @TableField("itinerary_key")
    private String itineraryKey;

    /**
     * 用户ID - 计划表所属用户
     * 作用：关联用户表，标识计划表的创建者
     * 必填：是
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 计划表标题
     * 作用：计划表的显示标题
     * 必填：是
     * 长度：最大200字符
     */
    @TableField("title")
    private String title;

    /**
     * 封面图片URL
     * 作用：计划表的封面图片地址
     * 必填：否
     * 格式：完整的URL地址
     */
    @TableField("cover_image_url")
    private String coverImageUrl;

    /**
     * 计划表JSON数据
     * 作用：存储完整的计划表JSON数据（包括days等所有信息）
     * 必填：是
     * 类型：LONGTEXT（支持大文本）
     */
    @TableField("json_data")
    private String jsonData;

    /**
     * 用户版本号
     * 作用：每次用户保存时递增，用于版本管理
     * 必填：是
     * 默认：1
     */
    @TableField("version")
    private Integer version;

    /**
     * 组件版本号
     * 作用：标识生成该计划表的组件版本（如v1、v2）
     * 必填：是
     * 默认：v1
     */
    @TableField("component_version")
    private String componentVersion;

    /**
     * 是否是当前使用版本
     * 作用：标记该版本是否为当前正在使用的版本（0-历史版本，1-当前版本）
     * 必填：是
     * 默认：1（当前版本）
     * 说明：同一个itinerary_id可能有多个版本记录，但只有一个版本是当前使用版本
     */
    @TableField("is_current_version")
    private Integer isCurrentVersion;

    /**
     * 创建时间 - 记录创建时间
     * 作用：记录计划表创建时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 记录最后修改时间
     * 作用：记录计划表最后修改时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除 - 软删除标记
     * 作用：标记计划表是否已删除（0-未删除，1-已删除）
     * 必填：是
     * 默认：0（未删除）
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
