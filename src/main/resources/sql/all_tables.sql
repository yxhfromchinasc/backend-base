-- ============================================
-- 旅心基础服务系统 - 所有数据表创建脚本
-- ============================================
-- 执行顺序：按依赖关系，先创建基础表，再创建业务表
-- ============================================

-- 1. 用户表
-- ============================================
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

-- 2. 权限表
-- ============================================
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

-- 3. 角色表
-- ============================================
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

-- 4. 系统配置表
-- ============================================
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

-- 5. 计划表
-- ============================================
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

-- ============================================
-- 执行完成
-- ============================================
-- 表创建顺序说明：
-- 1. user - 用户表（基础表，无依赖）
-- 2. permission - 权限表（基础表，无依赖）
-- 3. role - 角色表（基础表，无依赖）
-- 4. system_config - 系统配置表（基础表，无依赖）
-- 5. itinerary - 计划表（依赖user表）
-- ============================================
