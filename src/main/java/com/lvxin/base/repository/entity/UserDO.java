package com.lvxin.base.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户DO - Repository层数据对象
 * 存储用户基本信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class UserDO {

    /**
     * 用户ID - 主键，自增
     * 作用：唯一标识用户
     * 必填：是（系统自动生成）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名 - 用户登录名
     * 作用：用户登录时的用户名，全局唯一
     * 必填：否
     * 长度：最大50字符
     */
    @TableField("username")
    private String username;

    /**
     * 手机号 - 用户联系方式
     * 作用：用户手机号，用于登录和接收通知，全局唯一
     * 必填：是
     * 格式：11位手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱 - 用户邮箱地址
     * 作用：用户邮箱，用于接收通知和找回密码
     * 必填：否
     * 格式：标准邮箱格式
     */
    @TableField("email")
    private String email;

    /**
     * 密码 - 用户登录密码
     * 作用：用户登录时的密码，已加密存储
     * 必填：否
     * 存储：BCrypt加密后的密文
     */
    @TableField("password")
    private String password;

    /**
     * 昵称 - 用户显示名称
     * 作用：用户在前端显示的昵称
     * 必填：否
     * 默认：与用户名相同
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL - 用户头像地址
     * 作用：用户头像的存储地址
     * 必填：否
     * 格式：完整的URL地址
     */
    @TableField("avatar_url")
    private String avatarUrl;


    /**
     * 用户角色集合 - JSON格式存储
     * 作用：用户拥有的角色列表，用于权限控制
     * 必填：是
     * 格式：JSON数组，如["SUPER_ADMIN", "USER"]
     * 示例：["admin", "user", "manager"]
     */
    @TableField("roles")
    private String roles;

    /**
     * 用户状态 - 用户账户状态
     * 作用：控制用户账户是否可用
     * 必填：是
     * 值：1-正常，0-禁用，-1-锁定
     * 默认：1（正常）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间 - 记录创建时间
     * 作用：记录用户注册时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 记录最后修改时间
     * 作用：记录用户信息最后修改时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
