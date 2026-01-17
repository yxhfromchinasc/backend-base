-- 权限表
-- 用于存储权限点信息

CREATE TABLE IF NOT EXISTS `permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID（主键，自增）',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称（权限点的中文名称，用于前端显示，如用户管理、订单查看）',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码（权限点的英文编码，用于系统内部识别和权限验证，格式：模块:操作，如user:create、system:view）',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '权限描述（权限点的详细描述，说明权限的具体作用）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_permission_name` (`permission_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
