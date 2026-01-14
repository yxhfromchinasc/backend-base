package com.lvxin.base.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限点DO - Repository层数据对象
 * 存储权限点信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("permission")
public class PermissionDO {

    /**
     * 权限ID - 主键，自增
     * 作用：唯一标识权限点
     * 必填：是（系统自动生成）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限名称 - 权限点显示名称
     * 作用：权限点的中文名称，用于前端显示
     * 必填：是
     * 长度：最大100字符
     * 示例：用户管理、订单查看、系统设置
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 权限编码 - 权限点唯一标识
     * 作用：权限点的英文编码，用于系统内部识别和权限验证
     * 必填：是
     * 长度：最大100字符
     * 格式：模块:操作，如user:create、system:view
     * 示例：user:create、user:update、system:view、system:manage
     */
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 权限描述 - 权限点说明信息
     * 作用：权限点的详细描述，说明权限的具体作用
     * 必填：否
     * 长度：最大200字符
     * 示例：创建用户权限、查看订单权限
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间 - 记录创建时间
     * 作用：记录权限点创建时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 记录最后修改时间
     * 作用：记录权限点信息最后修改时间
     * 必填：否（系统自动填充）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
