# 计划表数据表 SQL 执行指南

## 情况1：新建数据库表（推荐）

如果数据库中没有 `itinerary` 表，直接执行以下SQL创建新表：

```sql
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
```

## 情况2：表已存在，需要迁移

如果表已存在（之前使用的是 `itinerary_id` 字段），需要执行以下迁移SQL：

### 步骤1：检查表结构

```sql
-- 查看当前表结构
DESC itinerary;
SHOW CREATE TABLE itinerary;
```

### 步骤2：执行迁移（如果字段名是 itinerary_id，需要重命名）

**注意**：MySQL不支持直接重命名字段，需要按以下步骤操作：

```sql
-- 1. 添加新字段 itinerary_key
ALTER TABLE `itinerary` 
ADD COLUMN `itinerary_key` VARCHAR(36) NULL COMMENT '计划表唯一标识（Key，UUID格式）' AFTER `id`;

-- 2. 将 itinerary_id 的数据复制到 itinerary_key
UPDATE `itinerary` 
SET `itinerary_key` = `itinerary_id` 
WHERE `itinerary_key` IS NULL;

-- 3. 修改 itinerary_key 为 NOT NULL
ALTER TABLE `itinerary` 
MODIFY COLUMN `itinerary_key` VARCHAR(36) NOT NULL COMMENT '计划表唯一标识（Key，UUID格式）';

-- 4. 删除旧的唯一键约束（如果存在）
ALTER TABLE `itinerary` 
DROP INDEX IF EXISTS `uk_itinerary_id`;

-- 5. 删除旧字段 itinerary_id（可选，建议先备份）
-- ALTER TABLE `itinerary` DROP COLUMN `itinerary_id`;

-- 6. 添加 is_current_version 字段（如果不存在）
ALTER TABLE `itinerary` 
ADD COLUMN IF NOT EXISTS `is_current_version` TINYINT NOT NULL DEFAULT 1 COMMENT '是否是当前使用版本（0-历史版本，1-当前版本）' AFTER `component_version`;

-- 7. 添加新的复合唯一键
ALTER TABLE `itinerary` 
ADD UNIQUE KEY `uk_itinerary_key_version` (`itinerary_key`, `version`);

-- 8. 添加索引
ALTER TABLE `itinerary` 
ADD INDEX IF NOT EXISTS `idx_itinerary_key` (`itinerary_key`),
ADD INDEX IF NOT EXISTS `idx_is_current_version` (`is_current_version`);

-- 9. 将现有数据标记为当前使用版本
UPDATE `itinerary` 
SET `is_current_version` = 1 
WHERE `is_current_version` IS NULL OR `is_current_version` = 0;
```

### 步骤3：如果表已存在但字段名已经是 itinerary_key

如果表已存在且字段名已经是 `itinerary_key`，只需要添加新字段和索引：

```sql
-- 1. 添加 is_current_version 字段（如果不存在）
ALTER TABLE `itinerary` 
ADD COLUMN IF NOT EXISTS `is_current_version` TINYINT NOT NULL DEFAULT 1 COMMENT '是否是当前使用版本（0-历史版本，1-当前版本）' AFTER `component_version`;

-- 2. 删除旧的唯一键约束（如果存在）
ALTER TABLE `itinerary` 
DROP INDEX IF EXISTS `uk_itinerary_id`;

-- 3. 添加新的复合唯一键（如果不存在）
ALTER TABLE `itinerary` 
ADD UNIQUE KEY IF NOT EXISTS `uk_itinerary_key_version` (`itinerary_key`, `version`);

-- 4. 添加索引
ALTER TABLE `itinerary` 
ADD INDEX IF NOT EXISTS `idx_itinerary_key` (`itinerary_key`),
ADD INDEX IF NOT EXISTS `idx_is_current_version` (`is_current_version`);

-- 5. 将现有数据标记为当前使用版本
UPDATE `itinerary` 
SET `is_current_version` = 1 
WHERE `is_current_version` IS NULL OR `is_current_version` = 0;
```

## 验证SQL

执行完SQL后，可以使用以下SQL验证表结构：

```sql
-- 查看表结构
DESC itinerary;

-- 查看索引
SHOW INDEX FROM itinerary;

-- 查看表注释
SELECT TABLE_COMMENT 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'itinerary';
```

## 注意事项

1. **备份数据**：在执行任何ALTER TABLE操作前，建议先备份数据
2. **字段重命名**：如果表已存在且字段名是 `itinerary_id`，需要按照步骤2的流程进行迁移
3. **唯一键约束**：新的复合唯一键 `(itinerary_key, version)` 确保同一个计划表的同一个版本只有一条记录
4. **索引优化**：已添加必要的索引以优化查询性能
