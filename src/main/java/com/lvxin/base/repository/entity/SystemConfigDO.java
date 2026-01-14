package com.lvxin.base.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置DO - Repository层数据对象
 * 存储系统配置信息（key-value结构，足够灵活）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_config")
public class SystemConfigDO {

    /**
     * 配置ID - 主键，自增
     * 作用：唯一标识配置项
     * 必填：是（系统自动生成）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键 - 唯一标识配置项
     * 作用：配置的唯一标识符
     * 必填：是
     * 长度：最大100字符
     * 示例：customer_service_phone、app_name
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置值 - 配置的具体值
     * 作用：存储配置的实际值
     * 必填：否（可为空）
     * 类型：TEXT（支持长文本）
     * 示例：400-123-4567、{"key": "value"}
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 配置类型 - 配置值的数据类型
     * 作用：标识配置值的数据类型，便于前端解析
     * 必填：否（默认string）
     * 值：string、number、boolean、json
     * 默认：string
     */
    @TableField("config_type")
    private String configType;

    /**
     * 配置描述 - 配置的说明信息
     * 作用：描述配置的用途和含义
     * 必填：否
     * 长度：最大500字符
     */
    @TableField("description")
    private String description;

    /**
     * 是否公开 - 是否对用户可见
     * 作用：标识配置是否对普通用户可见
     * 必填：是（默认0）
     * 值：0-仅管理员可见，1-用户可见
     * 默认：0
     */
    @TableField("is_public")
    private Integer isPublic;

    /**
     * 创建时间 - 记录创建时间
     * 作用：记录配置创建时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 记录最后修改时间
     * 作用：记录配置最后修改时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

