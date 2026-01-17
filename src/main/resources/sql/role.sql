-- 角色表
-- 用于存储角色信息，包含权限点集合

CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID（主键，自增）',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称（角色的中文名称，用于前端显示）',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（角色的英文编码，用于系统内部识别，如SUPER_ADMIN、USER）',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述（角色的详细描述，说明角色的职责和权限范围）',
    `permissions` TEXT NOT NULL COMMENT '权限点集合（JSON格式存储，如["user:create", "user:update"]，["*"]表示拥有所有权限）',
    `status` INT NOT NULL DEFAULT 1 COMMENT '角色状态（1-正常，0-禁用）',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色（1-是，0-否，系统角色不可修改或删除）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`),
    KEY `idx_is_system` (`is_system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
