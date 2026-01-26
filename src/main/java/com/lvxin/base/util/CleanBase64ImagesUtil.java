package com.lvxin.base.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 清理base64图片数据的工具类
 * 用于清理数据库中已存在的data:image格式的base64数据
 *
 * @author lvxin
 */
@Slf4j
@Component
public class CleanBase64ImagesUtil {

    /**
     * 清理JSON数组中的base64数据URI
     * 只保留HTTP/HTTPS的图片URL
     *
     * @param coverImageUrlsJson JSON数组字符串，如：["url1", "data:image/png;base64,...", "url2"]
     * @return 清理后的JSON数组字符串，如：["url1", "url2"]
     */
    public String cleanBase64Images(String coverImageUrlsJson) {
        if (coverImageUrlsJson == null || coverImageUrlsJson.trim().isEmpty()) {
            return null;
        }

        try {
            // 解析JSON数组
            JSONArray jsonArray = JSON.parseArray(coverImageUrlsJson);
            List<String> cleanedUrls = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                String url = jsonArray.getString(i);
                if (url != null && !url.trim().isEmpty()) {
                    // 过滤掉data URI
                    String lowerUrl = url.toLowerCase().trim();
                    if (!lowerUrl.startsWith("data:image/") && !lowerUrl.startsWith("data:image%2f")) {
                        cleanedUrls.add(url);
                    } else {
                        log.debug("过滤掉base64数据URI: {}", url.substring(0, Math.min(50, url.length())));
                    }
                }
            }

            // 如果清理后没有URL了，返回null
            if (cleanedUrls.isEmpty()) {
                return null;
            }

            // 返回清理后的JSON数组
            return JSON.toJSONString(cleanedUrls);
        } catch (Exception e) {
            log.error("清理base64图片数据失败: {}", coverImageUrlsJson, e);
            // 如果解析失败，返回null（或者可以返回原值，根据业务需求）
            return null;
        }
    }

    /**
     * 检查是否包含base64数据
     *
     * @param coverImageUrlsJson JSON数组字符串
     * @return 是否包含base64数据
     */
    public boolean containsBase64(String coverImageUrlsJson) {
        if (coverImageUrlsJson == null || coverImageUrlsJson.trim().isEmpty()) {
            return false;
        }

        String lowerJson = coverImageUrlsJson.toLowerCase();
        return lowerJson.contains("data:image/") || lowerJson.contains("data:image%2f");
    }
}
