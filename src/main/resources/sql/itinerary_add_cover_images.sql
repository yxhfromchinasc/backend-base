-- 计划表添加多封面图片支持
-- 将单封面图片字段扩展为多封面图片数组

-- 1. 添加多封面图片字段（JSON数组格式）
ALTER TABLE `itinerary` 
ADD COLUMN `cover_image_urls` TEXT DEFAULT NULL COMMENT '封面图片URL数组（JSON格式，如：["url1", "url2"]）' AFTER `cover_image_url`;

-- 2. 迁移现有数据：将单封面图片转换为数组格式
UPDATE `itinerary` 
SET `cover_image_urls` = CONCAT('["', `cover_image_url`, '"]')
WHERE `cover_image_url` IS NOT NULL 
  AND `cover_image_url` != '' 
  AND (`cover_image_urls` IS NULL OR `cover_image_urls` = '');

-- 3. 添加索引（可选，如果需要根据封面图片查询）
-- 注意：TEXT类型字段不能直接添加索引，如果需要查询可以考虑使用全文索引或其他方案
