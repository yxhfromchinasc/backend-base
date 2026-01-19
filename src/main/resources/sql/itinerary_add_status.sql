-- 计划表添加状态和原始计划字段
-- 用于支持异步生成计划功能

-- 1. 添加状态字段
ALTER TABLE `itinerary` 
ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '计划状态（NORMAL-正常，GENERATING-生成中，FAILED-生成失败）' AFTER `is_current_version`;

-- 2. 添加原始计划字段（存储用户输入的原始文本或AI对话的markdown）
ALTER TABLE `itinerary` 
ADD COLUMN `original_plan` LONGTEXT DEFAULT NULL COMMENT '原始计划内容（用户输入的文本或AI对话的markdown）' AFTER `json_data`;

-- 3. 添加错误信息字段（用于记录生成失败的原因）
ALTER TABLE `itinerary` 
ADD COLUMN `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息（生成失败时记录失败原因）' AFTER `status`;

-- 4. 添加状态索引（用于查询不同状态的计划）
ALTER TABLE `itinerary` 
ADD INDEX `idx_status` (`status`);

-- 5. 更新现有数据的状态为NORMAL
UPDATE `itinerary` SET `status` = 'NORMAL' WHERE `status` IS NULL OR `status` = '';
