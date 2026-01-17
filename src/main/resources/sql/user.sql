-- 用户表
-- 用于存储用户基本信息

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键，自增）',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名（用户登录名，全局唯一）',
    `phone` VARCHAR(11) NOT NULL COMMENT '手机号（用于登录和接收通知，全局唯一）',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱（用于接收通知和找回密码）',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '密码（BCrypt加密后的密文）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称（用户显示名称）',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL（用户头像的存储地址）',
    `roles` TEXT NOT NULL COMMENT '用户角色集合（JSON格式存储，如["SUPER_ADMIN", "USER"]）',
    `status` INT NOT NULL DEFAULT 1 COMMENT '用户状态（1-正常，0-禁用，-1-锁定）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
