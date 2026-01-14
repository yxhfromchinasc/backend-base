package com.lvxin.base.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色DO - Repository层数据对象
 * 存储角色信息，包含权限点集合
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("role")
public class RoleDO {

    /**
     * 角色ID - 主键，自增
     * 作用：唯一标识角色
     * 必填：是（系统自动生成）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称 - 角色显示名称
     * 作用：角色的中文名称，用于前端显示
     * 必填：是
     * 长度：最大50字符
     * 示例：超级管理员、普通用户、客服
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色编码 - 角色唯一标识
     * 作用：角色的英文编码，用于系统内部识别
     * 必填：是
     * 长度：最大50字符
     * 格式：大写字母和下划线
     * 示例：SUPER_ADMIN、USER、CUSTOMER_SERVICE
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 角色描述 - 角色说明信息
     * 作用：角色的详细描述，说明角色的职责和权限范围
     * 必填：否
     * 长度：最大200字符
     */
    @TableField("description")
    private String description;

    /**
     * 权限点集合 - JSON格式存储
     * 作用：角色拥有的权限点列表，用于权限控制
     * 必填：是
     * 格式：JSON数组，如["user:create", "user:update"]
     * 示例：["user:create", "user:update", "system:view"]
     * 特殊值：["*"] 表示拥有所有权限
     */
    @TableField("permissions")
    private String permissions;

    /**
     * 角色状态 - 角色是否启用
     * 作用：控制角色是否可用
     * 必填：是
     * 值：1-正常，0-禁用
     * 默认：1（正常）
     */
    @TableField("status")
    private Integer status;

    /**
     * 是否系统角色
     * 作用：标识系统内置角色（如超级管理员、普通用户），不可修改或删除
     * 值：1-是，0-否
     * 默认：0（否）
     */
    @TableField("is_system")
    private Integer isSystem;

    /**
     * 创建时间 - 记录创建时间
     * 作用：记录角色创建时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 记录最后修改时间
     * 作用：记录角色信息最后修改时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
