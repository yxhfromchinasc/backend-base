-- 清理计划表中的base64内联图片数据
-- 只保留HTTP/HTTPS的图片URL，移除data:image格式的base64数据

-- 1. 查看包含base64数据的记录
SELECT 
    id, 
    itinerary_key, 
    title,
    cover_image_urls,
    CASE 
        WHEN cover_image_urls LIKE '%data:image%' THEN '包含base64'
        ELSE '正常'
    END AS status
FROM itinerary 
WHERE cover_image_urls IS NOT NULL 
  AND cover_image_urls LIKE '%data:image%'
ORDER BY updated_at DESC;

-- 2. 清理base64数据：从JSON数组中移除所有data:image开头的元素
-- 注意：这个SQL比较复杂，建议在应用层处理，或者使用存储过程
-- 这里提供一个示例，实际执行时可能需要根据MySQL版本调整

-- 方法1：使用JSON函数（MySQL 5.7+）
UPDATE itinerary 
SET cover_image_urls = JSON_REMOVE(
    cover_image_urls,
    JSON_UNQUOTE(
        JSON_SEARCH(cover_image_urls, 'one', 'data:image%', NULL, '$[*]')
    )
)
WHERE cover_image_urls IS NOT NULL 
  AND cover_image_urls LIKE '%data:image%';

-- 方法2：如果上面的方法不工作，可以清空包含base64的cover_image_urls字段
-- UPDATE itinerary 
-- SET cover_image_urls = NULL
-- WHERE cover_image_urls IS NOT NULL 
--   AND cover_image_urls LIKE '%data:image%';

-- 3. 验证清理结果
SELECT 
    id, 
    itinerary_key, 
    title,
    cover_image_urls
FROM itinerary 
WHERE cover_image_urls IS NOT NULL 
ORDER BY updated_at DESC
LIMIT 10;
