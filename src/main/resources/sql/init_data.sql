-- ============================================
-- 系统初始化数据脚本
-- ============================================
-- 用途：插入系统初始角色和超级管理员用户
-- 执行时机：系统首次部署时执行
-- ============================================

-- 1. 插入系统角色
-- ============================================

-- 超级管理员角色
INSERT INTO `role` (`role_name`, `role_code`, `description`, `permissions`, `status`, `is_system`, `created_at`, `updated_at`)
VALUES (
    '超级管理员',
    'SUPER_ADMIN',
    '系统超级管理员，拥有所有权限',
    '["*"]',
    1,
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- 普通用户角色
INSERT INTO `role` (`role_name`, `role_code`, `description`, `permissions`, `status`, `is_system`, `created_at`, `updated_at`)
VALUES (
    '用户',
    'USER',
    '普通用户角色，拥有基本权限',
    '["user:view", "itinerary:create", "itinerary:view", "itinerary:update", "itinerary:delete"]',
    1,
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- 2. 插入超级管理员用户
-- ============================================
-- 手机号：18782886931
-- 密码：123456（已加密）
-- 角色：SUPER_ADMIN
-- 注意：密码使用 PasswordUtil.encode("123456") 生成的加密值
-- 由于每次加密都会生成不同的盐，这里使用一个已生成的加密密码

INSERT INTO `user` (`username`, `phone`, `email`, `password`, `nickname`, `roles`, `status`, `created_at`, `updated_at`)
VALUES (
    'admin',
    '18782886931',
    NULL,
    'jBw2fPnE0FQxc5caI1MKDQ==:9ukVvsKDpiLng/ItApVfTAgRwf4r9mECbcq8OHm6aSk=',  -- 密码：123456（已加密）
    '超级管理员',
    '["SUPER_ADMIN"]',
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE 
    `username` = VALUES(`username`),
    `password` = VALUES(`password`),
    `roles` = VALUES(`roles`);
