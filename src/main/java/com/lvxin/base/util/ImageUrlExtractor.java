package com.lvxin.base.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 图片URL提取工具类
 * 负责从文本和HTML中提取图片URL，并验证URL是否为有效的图片
 *
 * @author lvxin
 */
@Slf4j
@Component
public class ImageUrlExtractor {

    // 图片文件扩展名（不区分大小写）
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico"
    );

    // 图片Content-Type前缀
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", 
            "image/webp", "image/bmp", "image/svg+xml", "image/x-icon"
    );

    // URL匹配正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s<>\"'()]+)",
            Pattern.CASE_INSENSITIVE
    );

    // HTML img标签匹配
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
    );

    // CSS background-image匹配
    private static final Pattern BACKGROUND_IMAGE_PATTERN = Pattern.compile(
            "background-image\\s*:\\s*url\\([\"']?([^\"'()]+)[\"']?\\)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 从文本中提取图片URL
     *
     * @param text 文本内容
     * @return 图片URL列表（已去重和验证）
     */
    public List<String> extractImageUrlsFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        Set<String> imageUrls = new LinkedHashSet<>();

        // 提取所有URL
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        while (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            // 清理URL末尾的标点符号
            url = url.replaceAll("[。！？，；：\\s]+$", "");
            
            // 过滤掉data URI（base64内联图片）
            if (isDataUri(url)) {
                log.debug("跳过data URI: {}", url.substring(0, Math.min(50, url.length())));
                continue;
            }
            
            if (isImageUrl(url)) {
                imageUrls.add(url);
            }
        }

        log.debug("从文本中提取到 {} 个图片URL", imageUrls.size());
        return new ArrayList<>(imageUrls);
    }

    /**
     * 从HTML中提取图片URL
     *
     * @param html HTML内容
     * @return 图片URL列表（已去重和验证）
     */
    public List<String> extractImageUrlsFromHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return Collections.emptyList();
        }

        Set<String> imageUrls = new LinkedHashSet<>();

        // 1. 提取img标签的src属性
        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(html);
        while (imgMatcher.find()) {
            String url = imgMatcher.group(1);
            // 过滤掉data URI
            if (isDataUri(url)) {
                log.debug("跳过data URI: {}", url.substring(0, Math.min(50, url.length())));
                continue;
            }
            if (isImageUrl(url)) {
                imageUrls.add(normalizeUrl(url, html));
            }
        }

        // 2. 提取CSS background-image中的URL
        Matcher bgMatcher = BACKGROUND_IMAGE_PATTERN.matcher(html);
        while (bgMatcher.find()) {
            String url = bgMatcher.group(1);
            // 过滤掉data URI
            if (isDataUri(url)) {
                log.debug("跳过data URI: {}", url.substring(0, Math.min(50, url.length())));
                continue;
            }
            if (isImageUrl(url)) {
                imageUrls.add(normalizeUrl(url, html));
            }
        }

        // 3. 从文本中提取URL（作为补充）
        List<String> textUrls = extractImageUrlsFromText(html);
        imageUrls.addAll(textUrls);

        log.debug("从HTML中提取到 {} 个图片URL", imageUrls.size());
        return new ArrayList<>(imageUrls);
    }

    /**
     * 检查是否为data URI（base64内联图片）
     *
     * @param url URL
     * @return 是否为data URI
     */
    private boolean isDataUri(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String lowerUrl = url.toLowerCase().trim();
        return lowerUrl.startsWith("data:image/") || lowerUrl.startsWith("data:image%2f");
    }

    /**
     * 验证URL是否为图片
     * 先检查扩展名，如果没有扩展名则通过HTTP HEAD请求验证Content-Type
     *
     * @param url 图片URL
     * @return 是否为图片
     */
    public boolean isImageUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }

        // 过滤掉data URI（base64内联图片）
        if (isDataUri(url)) {
            return false;
        }

        // 过滤掉明显不是图片的URL
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("avatar") || lowerUrl.contains("profile") || 
            lowerUrl.contains("icon") || lowerUrl.contains("logo") ||
            lowerUrl.contains(".html") || lowerUrl.contains(".htm") ||
            lowerUrl.contains("/api/") || lowerUrl.contains("/api?")) {
            return false;
        }

        // 1. 检查扩展名
        if (hasImageExtension(url)) {
            return true;
        }

        // 2. 对于没有扩展名的URL，通过HTTP HEAD请求验证
        return validateImageUrl(url);
    }

    /**
     * 检查URL是否有图片扩展名
     *
     * @param url URL
     * @return 是否有图片扩展名
     */
    private boolean hasImageExtension(String url) {
        String lowerUrl = url.toLowerCase();
        // 移除查询参数和锚点
        String path = lowerUrl.split("[?#]")[0];
        
        for (String ext : IMAGE_EXTENSIONS) {
            if (path.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通过HTTP HEAD请求验证URL是否为图片
     *
     * @param url URL
     * @return 是否为图片
     */
    private boolean validateImageUrl(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            HttpResponse<Void> response = client.send(request, 
                    HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200) {
                String contentType = response.headers().firstValue("Content-Type")
                        .orElse("")
                        .toLowerCase()
                        .split(";")[0]
                        .trim();

                boolean isImage = IMAGE_CONTENT_TYPES.contains(contentType);
                if (!isImage) {
                    log.debug("URL Content-Type不是图片: {} -> {}", url, contentType);
                }
                return isImage;
            }

            return false;
        } catch (Exception e) {
            log.debug("验证图片URL失败: {}, 错误: {}", url, e.getMessage());
            // 验证失败时，如果URL看起来像图片就返回true（降级策略）
            return url.toLowerCase().contains("image") || 
                   url.toLowerCase().contains("photo") || 
                   url.toLowerCase().contains("pic");
        }
    }

    /**
     * 规范化URL（处理相对路径）
     *
     * @param url 原始URL
     * @param baseHtml HTML内容（用于提取base URL）
     * @return 规范化后的URL
     */
    private String normalizeUrl(String url, String baseHtml) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        // 处理相对路径
        if (url.startsWith("//")) {
            return "https:" + url;
        }

        if (url.startsWith("/")) {
            // 尝试从HTML中提取base URL
            Pattern basePattern = Pattern.compile(
                    "<base[^>]+href=[\"']([^\"']+)[\"']",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher baseMatcher = basePattern.matcher(baseHtml);
            if (baseMatcher.find()) {
                String baseUrl = baseMatcher.group(1);
                return baseUrl.replaceAll("/$", "") + url;
            }
        }

        return url;
    }

    /**
     * 过滤和排序图片URL
     * 优先选择内容图片，过滤掉头像、图标等
     *
     * @param urls 图片URL列表
     * @return 过滤后的图片URL列表
     */
    public List<String> filterAndSortImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        return urls.stream()
                .filter(this::isContentImage)
                .distinct()
                .sorted(this::compareImageUrls)
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为内容图片（非头像、图标等）
     *
     * @param url 图片URL
     * @return 是否为内容图片
     */
    private boolean isContentImage(String url) {
        String lowerUrl = url.toLowerCase();
        
        // 排除头像、图标等
        if (lowerUrl.contains("avatar") || lowerUrl.contains("profile") ||
            lowerUrl.contains("icon") || lowerUrl.contains("logo") ||
            lowerUrl.contains("favicon")) {
            return false;
        }

        // 优先选择内容图片
        return lowerUrl.contains("image") || lowerUrl.contains("photo") ||
               lowerUrl.contains("pic") || lowerUrl.contains("img") ||
               lowerUrl.contains("photo") || hasImageExtension(url);
    }

    /**
     * 比较图片URL的优先级（用于排序）
     *
     * @param url1 URL1
     * @param url2 URL2
     * @return 比较结果
     */
    private int compareImageUrls(String url1, String url2) {
        String lower1 = url1.toLowerCase();
        String lower2 = url2.toLowerCase();

        // 优先选择包含特定关键词的URL
        boolean hasKeyword1 = lower1.contains("image") || lower1.contains("photo");
        boolean hasKeyword2 = lower2.contains("image") || lower2.contains("photo");

        if (hasKeyword1 && !hasKeyword2) {
            return -1;
        }
        if (!hasKeyword1 && hasKeyword2) {
            return 1;
        }

        return 0;
    }
}
