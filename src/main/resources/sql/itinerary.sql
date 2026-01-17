-- 计划表数据表
-- 用于存储用户的行程计划表信息

CREATE TABLE IF NOT EXISTS `itinerary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '计划表ID（主键，自增）',
    `itinerary_key` VARCHAR(36) NOT NULL COMMENT '计划表唯一标识（Key，UUID格式，前端使用）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（关联user表）',
    `title` VARCHAR(200) NOT NULL COMMENT '计划表标题',
    `cover_image_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `json_data` LONGTEXT NOT NULL COMMENT '计划表JSON数据（包括days等所有信息）',
    `version` INT NOT NULL DEFAULT 1 COMMENT '用户版本号（每次保存时递增）',
    `component_version` VARCHAR(10) NOT NULL DEFAULT 'v1' COMMENT '组件版本号（如v1、v2）',
    `is_current_version` TINYINT NOT NULL DEFAULT 1 COMMENT '是否是当前使用版本（0-历史版本，1-当前版本）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_itinerary_key_version` (`itinerary_key`, `version`),
    KEY `idx_itinerary_key` (`itinerary_key`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_updated_at` (`updated_at`),
    KEY `idx_is_deleted` (`is_deleted`),
    KEY `idx_is_current_version` (`is_current_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划表数据表';
