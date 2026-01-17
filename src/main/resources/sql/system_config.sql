-- 系统配置表
-- 用于存储系统配置信息（key-value结构，足够灵活）

CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID（主键，自增）',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键（唯一标识配置项，如customer_service_phone、app_name）',
    `config_value` TEXT DEFAULT NULL COMMENT '配置值（配置的具体值，支持长文本，如400-123-4567、{"key": "value"}）',
    `config_type` VARCHAR(20) DEFAULT 'string' COMMENT '配置类型（string、number、boolean、json，默认string）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '配置描述（配置的用途和含义）',
    `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开（0-仅管理员可见，1-用户可见）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
