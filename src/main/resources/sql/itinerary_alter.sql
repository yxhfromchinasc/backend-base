-- 为现有表添加 is_current_version 字段并调整唯一键约束
-- 如果表已存在，执行此脚本添加新字段和调整约束

-- 1. 如果字段名是 itinerary_id，需要重命名为 itinerary_key
-- 注意：MySQL不支持直接重命名，需要先添加新字段，迁移数据，删除旧字段
-- 如果表是新创建的，直接使用 itinerary_key 即可

-- 2. 添加 is_current_version 字段
ALTER TABLE `itinerary` 
ADD COLUMN IF NOT EXISTS `is_current_version` TINYINT NOT NULL DEFAULT 1 COMMENT '是否是当前使用版本（0-历史版本，1-当前版本）' AFTER `component_version`;

-- 3. 添加新的复合唯一键：确保同一个计划表的同一个版本只有一条记录
ALTER TABLE `itinerary` 
ADD UNIQUE KEY IF NOT EXISTS `uk_itinerary_key_version` (`itinerary_key`, `version`);

-- 4. 添加索引
ALTER TABLE `itinerary` 
ADD INDEX IF NOT EXISTS `idx_is_current_version` (`is_current_version`),
ADD INDEX IF NOT EXISTS `idx_itinerary_key` (`itinerary_key`);

-- 5. 将现有数据标记为当前使用版本（如果之前没有这个字段，所有数据都应该是当前版本）
UPDATE `itinerary` SET `is_current_version` = 1 WHERE `is_current_version` IS NULL OR `is_current_version` = 0;
