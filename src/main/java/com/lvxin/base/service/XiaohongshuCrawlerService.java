package com.lvxin.base.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小红书内容获取服务
 * 负责从小红书链接中提取旅游攻略内容
 *
 * @author lvxin
 */
@Slf4j
@Service
public class XiaohongshuCrawlerService {

    private final com.lvxin.base.util.ImageUrlExtractor imageUrlExtractor;
    private final com.lvxin.base.service.MediaService mediaService;

    public XiaohongshuCrawlerService(
            com.lvxin.base.util.ImageUrlExtractor imageUrlExtractor,
            com.lvxin.base.service.MediaService mediaService) {
        this.imageUrlExtractor = imageUrlExtractor;
        this.mediaService = mediaService;
    }

    private static final String XIAOHONGSHU_DOMAIN = "xiaohongshu.com";
    private static final String XHSLINK_DOMAIN = "xhslink.com";
    
    // 匹配所有可能的URL（包括短链接）
    private static final Pattern URL_EXTRACTION_PATTERN = Pattern.compile(
            "(https?://[^\\s]+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 从文本中提取并规范化小红书链接
     * 支持从包含文本的输入中提取链接，支持短链接
     *
     * @param input 用户输入的文本（可能包含链接和其他文本）
     * @return 提取并规范化后的链接，如果未找到则返回null
     */
    public String extractAndNormalizeUrl(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }

        String trimmed = input.trim();
        
        // 首先尝试提取所有URL
        Matcher urlMatcher = URL_EXTRACTION_PATTERN.matcher(trimmed);
        String extractedUrl = null;
        
        while (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            // 优先匹配小红书相关链接
            if (url.contains(XIAOHONGSHU_DOMAIN) || url.contains(XHSLINK_DOMAIN)) {
                extractedUrl = url;
                break;
            }
            // 如果没有找到小红书链接，记录第一个URL（可能是短链接）
            if (extractedUrl == null) {
                extractedUrl = url;
            }
        }
        
        if (extractedUrl == null) {
            // 如果没有找到URL，检查整个输入是否就是一个链接（可能没有http://前缀）
            if (trimmed.contains(XIAOHONGSHU_DOMAIN) || trimmed.contains(XHSLINK_DOMAIN)) {
                // 尝试添加http://前缀
                if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    extractedUrl = "https://" + trimmed;
                } else {
                    extractedUrl = trimmed;
                }
            }
        }
        
        if (extractedUrl != null) {
            // 清理URL末尾可能存在的标点符号
            extractedUrl = extractedUrl.replaceAll("[。！？\\s]+$", "");
            log.info("从输入中提取链接: {} -> {}", input.substring(0, Math.min(50, input.length())), extractedUrl);
        }
        
        return extractedUrl;
    }

    /**
     * 解析短链接，获取真实的小红书链接
     * 通过HTTP请求获取重定向后的真实URL
     * 支持处理302重定向和相对路径Location头
     *
     * @param shortUrl 短链接
     * @return 真实的小红书链接，如果无法解析或重定向到登录页面则返回null
     */
    private String resolveShortUrl(String shortUrl) {
        try {
            log.info("解析短链接: {}", shortUrl);
            
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.put("Referer", "https://www.xiaohongshu.com/");
            
            // 创建不跟随重定向的HttpClient，手动处理重定向链
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            
            String currentUrl = shortUrl;
            int maxRedirects = 5; // 最多跟随5次重定向
            int redirectCount = 0;
            
            while (redirectCount < maxRedirects) {
                java.net.URI uri = java.net.URI.create(currentUrl);
                
                // 发送HEAD请求
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(uri)
                        .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                        .timeout(java.time.Duration.ofSeconds(10))
                        .headers(headers.entrySet().stream()
                                .map(e -> new String[]{e.getKey(), e.getValue()})
                                .flatMap(java.util.Arrays::stream)
                                .toArray(String[]::new))
                        .build();
                
                java.net.http.HttpResponse<Void> response = client.send(request, 
                        java.net.http.HttpResponse.BodyHandlers.discarding());
                
                int statusCode = response.statusCode();
                log.debug("短链接请求状态码: {}, URL: {}", statusCode, currentUrl);
                
                // 检查重定向
                if (statusCode >= 300 && statusCode < 400) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null) {
                        // 尝试从其他可能的头字段获取
                        location = response.headers().firstValue("location").orElse(null);
                    }
                    
                    if (location != null) {
                        // 处理相对路径
                        if (!location.startsWith("http://") && !location.startsWith("https://")) {
                            if (location.startsWith("//")) {
                                location = "https:" + location;
                            } else if (location.startsWith("/")) {
                                location = uri.getScheme() + "://" + uri.getHost() + location;
                            } else {
                                location = uri.resolve(location).toString();
                            }
                        }
                        
                        log.debug("重定向到: {}", location);
                        
                        // 检查是否重定向到登录页面
                        if (location.contains("/login") || location.contains("/signin") || 
                            location.contains("passport") || location.contains("account")) {
                            log.warn("短链接重定向到登录页面，可能需要登录: {}", location);
                            // 继续尝试，可能登录页面会再次重定向
                        }
                        
                        // 如果重定向到小红书域名，检查是否是有效的内容页面
                        if (location.contains(XIAOHONGSHU_DOMAIN)) {
                            // 检查是否是登录页面
                            if (!location.contains("/login") && !location.contains("/signin") && 
                                !location.contains("/passport") && !location.contains("/account")) {
                                log.info("短链接解析成功: {} -> {}", shortUrl, location);
                                return location;
                            }
                        }
                        
                        // 继续跟随重定向
                        currentUrl = location;
                        redirectCount++;
                        continue;
                    }
                }
                
                // 如果是200，说明已经是最终页面，但HEAD可能无法获取内容，尝试GET
                if (statusCode == 200) {
                    // HEAD请求成功，但我们需要获取真实URL，尝试GET请求跟随重定向
                    return resolveShortUrlWithGet(shortUrl, headers);
                }
                
                // 其他状态码，尝试GET请求
                break;
            }
            
            // 如果HEAD请求失败，尝试GET请求（跟随重定向）
            return resolveShortUrlWithGet(shortUrl, headers);
            
        } catch (javax.net.ssl.SSLHandshakeException | java.net.SocketException e) {
            log.warn("解析短链接时SSL握手失败: {}, 尝试备选方案", shortUrl);
            // SSL失败时，尝试使用HttpUtil
            try {
                Map<String, String> fallbackHeaders = new HashMap<>();
                fallbackHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                fallbackHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                return resolveShortUrlWithHttpUtil(shortUrl, fallbackHeaders);
            } catch (Exception e2) {
                log.error("备选方案也失败: {}", shortUrl, e2);
                return null;
            }
        } catch (Exception e) {
            log.warn("解析短链接时发生异常: {}, 尝试GET请求", shortUrl, e);
            // 尝试使用GET请求
            try {
                Map<String, String> fallbackHeaders = new HashMap<>();
                fallbackHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                fallbackHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                return resolveShortUrlWithGet(shortUrl, fallbackHeaders);
            } catch (Exception e2) {
                log.error("GET请求解析短链接也失败: {}", shortUrl, e2);
                return null; // 返回null表示无法解析
            }
        }
    }
    
    /**
     * 使用GET请求解析短链接，跟随重定向
     * 使用HttpUtil作为备选方案，因为它有重试机制
     */
    private String resolveShortUrlWithGet(String shortUrl, Map<String, String> headers) {
        // 方法1：尝试使用HttpClient（支持自动跟随重定向）
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(shortUrl))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .headers(headers.entrySet().stream()
                            .map(e -> new String[]{e.getKey(), e.getValue()})
                            .flatMap(java.util.Arrays::stream)
                            .toArray(String[]::new))
                    .build();
            
            // 只读取响应头，不读取body（节省资源）
            java.net.http.HttpResponse<Void> response = client.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.discarding());
            
            String finalUrl = response.uri().toString();
            log.info("GET请求解析短链接成功: {} -> {}", shortUrl, finalUrl);
            
            // 检查是否是登录页面
            if (finalUrl.contains("/login") || finalUrl.contains("/signin") || 
                finalUrl.contains("/passport") || finalUrl.contains("/account")) {
                log.warn("短链接最终重定向到登录页面: {}", finalUrl);
            }
            
            return finalUrl;
            
        } catch (javax.net.ssl.SSLHandshakeException | java.net.SocketException e) {
            log.warn("SSL握手失败，尝试使用HttpUtil作为备选方案: {}", shortUrl);
            // SSL失败时，使用HttpUtil重试（它有重试机制）
            return resolveShortUrlWithHttpUtil(shortUrl, headers);
        } catch (Exception e) {
            log.warn("HttpClient解析短链接失败，尝试使用HttpUtil: {} - {}", shortUrl, e.getMessage());
            // 其他异常时，也尝试HttpUtil
            return resolveShortUrlWithHttpUtil(shortUrl, headers);
        }
    }
    
    /**
     * 使用HttpUtil解析短链接（备选方案）
     * HttpUtil有重试机制，可能在某些情况下更稳定
     */
    private String resolveShortUrlWithHttpUtil(String shortUrl, Map<String, String> headers) {
        try {
            // HttpUtil会自动跟随重定向，但我们需要获取最终URL
            // 由于HttpUtil只返回响应体，我们需要手动处理重定向
            // 先尝试获取响应，然后检查Location头
            
            // 创建一个不跟随重定向的请求来获取Location头
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(shortUrl))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .headers(headers.entrySet().stream()
                            .map(e -> new String[]{e.getKey(), e.getValue()})
                            .flatMap(java.util.Arrays::stream)
                            .toArray(String[]::new))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            // 检查重定向
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (location == null) {
                    location = response.headers().firstValue("location").orElse(null);
                }
                
                if (location != null) {
                    // 处理相对路径
                    if (!location.startsWith("http://") && !location.startsWith("https://")) {
                        if (location.startsWith("//")) {
                            location = "https:" + location;
                        } else if (location.startsWith("/")) {
                            java.net.URI uri = java.net.URI.create(shortUrl);
                            location = uri.getScheme() + "://" + uri.getHost() + location;
                        } else {
                            location = java.net.URI.create(shortUrl).resolve(location).toString();
                        }
                    }
                    
                    log.info("HttpUtil备选方案解析短链接成功: {} -> {}", shortUrl, location);
                    return location;
                }
            }
            
            // 如果不是重定向，返回原URL
            log.warn("HttpUtil备选方案未找到重定向，返回原URL: {}", shortUrl);
            return shortUrl;
            
        } catch (Exception e) {
            log.error("HttpUtil备选方案也失败: {}", shortUrl, e);
            return null;
        }
    }

    /**
     * 验证是否为有效的小红书链接
     *
     * @param url 链接地址
     * @return 是否为有效链接
     */
    public boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return url.contains(XIAOHONGSHU_DOMAIN) || url.contains(XHSLINK_DOMAIN);
    }

    /**
     * 从小红书链接获取内容
     * 支持完整链接和短链接，支持从包含文本的输入中提取链接
     *
     * @param input 用户输入（可能是链接，也可能是包含链接的文本）
     * @return 获取的文本内容
     */
    public CrawlResult crawlContent(String input) {
        if (!StringUtils.hasText(input)) {
            return CrawlResult.failure("输入不能为空");
        }

        // 从输入中提取链接
        String url = extractAndNormalizeUrl(input);
        if (url == null) {
            return CrawlResult.failure("未能从输入中提取到有效的小红书链接");
        }

        // 如果是短链接，先解析为真实链接
        if (url.contains(XHSLINK_DOMAIN)) {
            String resolvedUrl = resolveShortUrl(url);
            if (resolvedUrl == null) {
                // 如果短链接解析失败，尝试直接使用短链接（某些情况下短链接可以直接访问）
                log.warn("短链接解析失败，尝试直接使用短链接: {}", url);
                // 不返回错误，继续尝试访问短链接本身
                // 某些短链接服务可能允许直接访问
            } else {
                url = resolvedUrl;
            }
        }

        // 验证链接有效性
        if (!isValidUrl(url)) {
            return CrawlResult.failure("无效的小红书链接: " + url);
        }
        
        // 检查是否是登录页面
        boolean isLoginPage = url.contains("/login") || url.contains("/signin") || 
                             url.contains("/passport") || url.contains("/account");
        
            if (isLoginPage) {
                log.warn("检测到登录页面，尝试从页面中提取原始链接: {}", url);
                // 尝试从登录页面HTML中提取原始链接
                String originalUrl = extractOriginalUrlFromLoginPage(url);
                if (originalUrl != null && !originalUrl.equals(url)) {
                    log.info("从登录页面提取到原始链接: {} -> {}", url, originalUrl);
                    url = originalUrl;
                    isLoginPage = false;
                } else {
                    log.warn("无法从登录页面提取原始链接，将尝试访问登录页面内容");
                }
            }

        try {
            log.info("开始获取小红书内容，原始输入: {}, 最终URL: {}, 是否登录页: {}", 
                    input.substring(0, Math.min(100, input.length())), url, isLoginPage);

            // 设置请求头，模拟浏览器访问
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.put("Referer", "https://www.xiaohongshu.com/");

            // 尝试获取页面内容
            String htmlContent = com.lvxin.base.util.HttpUtil.get(url, headers, 15);

            if (htmlContent == null || htmlContent.isEmpty()) {
                log.warn("获取失败：无法获取页面内容，URL: {}", url);
                return CrawlResult.failure("无法获取页面内容，可能是链接无效或需要登录");
            }

            // 提取文本内容
            String extractedText = extractTextFromHtml(htmlContent);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                if (isLoginPage) {
                    log.warn("获取失败：链接重定向到登录页面，无法获取内容，URL: {}", url);
                    return CrawlResult.failure("链接需要登录才能访问，请确保链接是公开可访问的小红书旅游攻略");
                } else {
                    log.warn("获取失败：无法提取文本内容，URL: {}", url);
                    return CrawlResult.failure("无法提取文本内容，页面可能需要JavaScript渲染或需要登录");
                }
            }
            
            // 如果提取的内容看起来像是登录页面，给出提示
            if (isLoginPage || extractedText.contains("登录") || extractedText.contains("请登录") || 
                extractedText.contains("Sign in") || extractedText.contains("Login")) {
                log.warn("提取的内容可能来自登录页面，内容长度: {}", extractedText.length());
                // 继续处理，但内容可能不完整
            }

            // 提取图片URL
            List<String> images = extractImagesFromHtml(htmlContent);
            log.info("获取成功，提取文本长度: {}, 图片数量: {}, URL: {}", 
                    extractedText.length(), images.size(), url);
            
            return CrawlResult.success(extractedText, images);

        } catch (Exception e) {
            log.error("获取小红书内容时发生异常，URL: {}", url, e);
            return CrawlResult.failure("获取失败：" + e.getMessage());
        }
    }

    /**
     * 从HTML中提取文本内容
     * 注意：小红书的内容通常在JavaScript中，简单的HTML解析可能无法获取完整内容
     * 这里提供一个基础实现，实际可能需要使用浏览器自动化工具（如Selenium/Playwright）
     *
     * @param html HTML内容
     * @return 提取的文本
     */
    private String extractTextFromHtml(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }

        StringBuilder textBuilder = new StringBuilder();

        // 尝试提取JSON数据（小红书通常将内容放在window.__INITIAL_STATE__或类似变量中）
        Pattern jsonPattern = Pattern.compile(
                "window\\.__INITIAL_STATE__\\s*=\\s*\\{([^}]+)\\}",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher jsonMatcher = jsonPattern.matcher(html);
        if (jsonMatcher.find()) {
            // 如果找到JSON数据，尝试提取其中的文本内容
            // 这里可以进一步解析JSON，提取标题、正文等
            // 目前先使用其他方法提取，后续可以优化JSON解析逻辑
        }

        // 尝试提取title标签
        Pattern titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher titleMatcher = titlePattern.matcher(html);
        if (titleMatcher.find()) {
            textBuilder.append(titleMatcher.group(1).trim()).append("\n\n");
        }

        // 尝试提取meta description
        Pattern descPattern = Pattern.compile(
                "<meta[^>]*name=[\"']description[\"'][^>]*content=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE
        );
        Matcher descMatcher = descPattern.matcher(html);
        if (descMatcher.find()) {
            textBuilder.append(descMatcher.group(1).trim()).append("\n\n");
        }

        // 尝试提取script标签中的文本（小红书内容可能在JS中）
        Pattern scriptPattern = Pattern.compile(
                "<script[^>]*type=[\"']application/json[\"'][^>]*>([^<]+)</script>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher scriptMatcher = scriptPattern.matcher(html);
        while (scriptMatcher.find()) {
            String scriptContent = scriptMatcher.group(1);
            // 尝试提取其中的文本字段
            Pattern textPattern = Pattern.compile("\"(?:title|content|desc|text|note)\"\\s*:\\s*\"([^\"]+)\"");
            Matcher textMatcher = textPattern.matcher(scriptContent);
            while (textMatcher.find()) {
                textBuilder.append(textMatcher.group(1)).append("\n");
            }
        }

        // 如果以上方法都没有提取到内容，尝试简单的HTML标签移除
        if (textBuilder.length() == 0) {
            // 移除script和style标签
            String cleaned = html.replaceAll("<script[^>]*>.*?</script>", "")
                    .replaceAll("<style[^>]*>.*?</style>", "")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            // 如果清理后的文本足够长，使用它
            if (cleaned.length() > 100) {
                textBuilder.append(cleaned);
            }
        }

        String result = textBuilder.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    /**
     * 从登录页面HTML中提取原始的小红书链接
     * 某些登录页面可能会在URL参数或HTML中保存原始链接
     *
     * @param loginPageUrl 登录页面URL
     * @return 原始链接，如果无法提取则返回null
     */
    private String extractOriginalUrlFromLoginPage(String loginPageUrl) {
        try {
            // 首先检查URL参数中是否有原始链接
            if (loginPageUrl.contains("redirect=") || loginPageUrl.contains("returnUrl=") || 
                loginPageUrl.contains("redirect_uri=")) {
                Pattern redirectPattern = Pattern.compile(
                        "(?:redirect|returnUrl|redirect_uri)=([^&]+)",
                        Pattern.CASE_INSENSITIVE
                );
                Matcher matcher = redirectPattern.matcher(loginPageUrl);
                if (matcher.find()) {
                    try {
                        String encodedUrl = matcher.group(1);
                        String decodedUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8");
                        if (decodedUrl.contains(XIAOHONGSHU_DOMAIN)) {
                            return decodedUrl;
                        }
                    } catch (Exception e) {
                        log.debug("URL解码失败", e);
                    }
                }
            }
            
            // 尝试从HTML中提取
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            
            String htmlContent = com.lvxin.base.util.HttpUtil.get(loginPageUrl, headers, 10);
            if (htmlContent != null && htmlContent.length() > 0) {
                // 尝试从meta标签、script标签或隐藏字段中提取
                Pattern[] patterns = new Pattern[]{
                    Pattern.compile("(?:redirect|returnUrl|redirect_uri)[\"']?\\s*[:=]\\s*[\"']([^\"']*xiaohongshu\\.com[^\"']*)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("<input[^>]*name=[\"'](?:redirect|returnUrl)[\"'][^>]*value=[\"']([^\"']*xiaohongshu\\.com[^\"']*)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("window\\.location\\s*=\\s*[\"']([^\"']*xiaohongshu\\.com[^\"']*)", Pattern.CASE_INSENSITIVE)
                };
                
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(htmlContent);
                    if (matcher.find()) {
                        String foundUrl = matcher.group(1);
                        log.info("从登录页面HTML中提取到链接: {}", foundUrl);
                        return foundUrl;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("从登录页面提取原始链接失败", e);
            return null;
        }
    }

    /**
     * 从HTML中提取图片URL列表，并上传到自己的云存储
     *
     * @param html HTML内容
     * @return 上传后的图片URL列表（自己的云存储链接）
     */
    private List<String> extractImagesFromHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return Collections.emptyList();
        }

        try {
            // 使用ImageUrlExtractor提取图片
            List<String> images = imageUrlExtractor.extractImageUrlsFromHtml(html);
            
            // 过滤和排序图片
            images = imageUrlExtractor.filterAndSortImageUrls(images);
            
            // 限制图片数量（最多20张）
            if (images.size() > 20) {
                images = images.subList(0, 20);
            }
            
            log.debug("从小红书HTML中提取到 {} 张图片，开始上传到云存储", images.size());
            
            // 下载并上传图片到自己的云存储
            List<String> uploadedImages = new ArrayList<>();
            for (String imageUrl : images) {
                try {
                    String uploadedUrl = mediaService.downloadAndUploadImage(imageUrl, "xiaohongshu");
                    if (uploadedUrl != null && !uploadedUrl.isEmpty()) {
                        uploadedImages.add(uploadedUrl);
                        log.debug("图片上传成功: {} -> {}", imageUrl, uploadedUrl);
                    } else {
                        log.warn("图片上传失败，跳过: {}", imageUrl);
                    }
                } catch (Exception e) {
                    log.warn("上传图片时发生异常，跳过: {}", imageUrl, e);
                    // 继续处理下一张图片
                }
            }
            
            log.info("从小红书HTML中提取到 {} 张图片，成功上传 {} 张", images.size(), uploadedImages.size());
            return uploadedImages;
        } catch (Exception e) {
            log.warn("提取图片URL时发生异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取结果
     */
    public static class CrawlResult {
        private final boolean success;
        private final String content;
        private final List<String> images;
        private final String errorMessage;

        private CrawlResult(boolean success, String content, List<String> images, String errorMessage) {
            this.success = success;
            this.content = content;
            this.images = images != null ? images : Collections.emptyList();
            this.errorMessage = errorMessage;
        }

        public static CrawlResult success(String content) {
            return new CrawlResult(true, content, Collections.emptyList(), null);
        }

        public static CrawlResult success(String content, List<String> images) {
            return new CrawlResult(true, content, images, null);
        }

        public static CrawlResult failure(String errorMessage) {
            return new CrawlResult(false, null, Collections.emptyList(), errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContent() {
            return content;
        }

        public List<String> getImages() {
            return images;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
