package com.lvxin.base.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP客户端工具类
 * 用于调用外部API（高德地图、大众点评等）
 *
 * @author lvxin
 */
@Slf4j
@Component
public class HttpClientUtil {
    
    private final HttpClient httpClient;
    
    public HttpClientUtil() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * 发送GET请求（带重试机制）
     *
     * @param url 请求URL
     * @return 响应内容
     */
    public String get(String url) {
        return get(url, 3); // 默认重试3次
    }
    
    /**
     * 发送GET请求（带重试机制）
     *
     * @param url 请求URL
     * @param maxRetries 最大重试次数
     * @return 响应内容
     */
    public String get(String url, int maxRetries) {
        long totalStartTime = System.currentTimeMillis();
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            attempt++;
            long attemptStartTime = System.currentTimeMillis();
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                
                if (response.statusCode() == 200) {
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.info("HTTP GET请求成功: URL={}, 本次耗时={}ms, 总耗时={}ms, 尝试次数={}", 
                            url, attemptDuration, totalDuration, attempt);
                    return response.body();
                } else {
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.error("HTTP GET请求失败，状态码: {}, URL: {}, 本次耗时={}ms, 总耗时={}ms", 
                            response.statusCode(), url, attemptDuration, totalDuration);
                    throw new RuntimeException("HTTP请求失败，状态码: " + response.statusCode());
                }
            } catch (SSLHandshakeException | SocketException e) {
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                lastException = e;
                boolean isSSLException = e instanceof SSLHandshakeException || 
                        (e.getMessage() != null && (e.getMessage().contains("SSL") || 
                         e.getMessage().contains("handshake") || 
                         e.getMessage().contains("Connection reset")));
                
                if (isSSLException && attempt < maxRetries) {
                    log.debug("SSL握手失败，尝试次数: {}/{}, 本次耗时={}ms, 将重试: {}", 
                            attempt, maxRetries, attemptDuration, e.getMessage());
                    try {
                        Thread.sleep(1000 * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("请求被中断", ie);
                    }
                    continue;
                } else {
                    // SSL错误且达到最大重试次数，尝试使用HttpUtil作为备选方案
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.warn("HttpClient SSL握手最终失败，总耗时={}ms，尝试使用HttpUtil备选方案: {}", totalDuration, url);
                    return getWithHttpUtil(url);
                }
            } catch (IOException | InterruptedException e) {
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("HTTP GET请求异常，尝试次数: {}/{}, 本次耗时={}ms, 将重试: {}", 
                            attempt, maxRetries, attemptDuration, e.getMessage());
                    try {
                        Thread.sleep(1000 * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("请求被中断", ie);
                    }
                    continue;
                } else {
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.error("HTTP GET请求最终失败，URL: {}, 总耗时={}ms", url, totalDuration, e);
                    throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
                }
            }
        }
        
        // 如果所有重试都失败，尝试使用HttpUtil作为最后备选方案
        if (lastException != null) {
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.warn("所有重试都失败，总耗时={}ms，尝试使用HttpUtil备选方案: {}", totalDuration, url);
            return getWithHttpUtil(url);
        }
        
        throw new RuntimeException("HTTP请求失败，未知错误");
    }
    
    /**
     * 使用HttpUtil作为备选方案（有重试机制）
     */
    private String getWithHttpUtil(String url) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0");
            String response = com.lvxin.base.util.HttpUtil.get(url, headers, 30);
            long duration = System.currentTimeMillis() - startTime;
            if (response != null && !response.isEmpty()) {
                log.info("使用HttpUtil备选方案成功: URL={}, 耗时={}ms", url, duration);
                return response;
            } else {
                log.error("HttpUtil备选方案返回空响应: URL={}, 耗时={}ms", url, duration);
                throw new RuntimeException("HttpUtil备选方案返回空响应");
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("HttpUtil备选方案也失败: URL={}, 耗时={}ms", url, duration, e);
            throw new RuntimeException("HTTP请求失败，所有方案都失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送POST请求（带重试机制）
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头（可选）
     * @return 响应内容
     */
    public String post(String url, String body, java.util.Map<String, String> headers) {
        return post(url, body, headers, 3); // 默认重试3次
    }
    
    /**
     * 发送POST请求（带重试机制）
     *
     * @param url        请求URL
     * @param body       请求体
     * @param headers    请求头（可选）
     * @param maxRetries 最大重试次数
     * @return 响应内容
     */
    public String post(String url, String body, java.util.Map<String, String> headers, int maxRetries) {
        long totalStartTime = System.currentTimeMillis();
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            attempt++;
            long attemptStartTime = System.currentTimeMillis();
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(body));
                
                if (headers != null) {
                    headers.forEach(requestBuilder::header);
                } else {
                    requestBuilder.header("User-Agent", "Mozilla/5.0");
                }
                
                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                
                if (response.statusCode() == 200) {
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.info("HTTP POST请求成功: URL={}, 本次耗时={}ms, 总耗时={}ms, 尝试次数={}", 
                            url, attemptDuration, totalDuration, attempt);
                    return response.body();
                } else {
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.error("HTTP POST请求失败，状态码: {}, URL: {}, 本次耗时={}ms, 总耗时={}ms", 
                            response.statusCode(), url, attemptDuration, totalDuration);
                    throw new RuntimeException("HTTP请求失败，状态码: " + response.statusCode());
                }
            } catch (SSLHandshakeException | SocketException e) {
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                lastException = e;
                boolean isSSLException = e instanceof SSLHandshakeException || 
                        (e.getMessage() != null && (e.getMessage().contains("SSL") || 
                         e.getMessage().contains("handshake") || 
                         e.getMessage().contains("Connection reset")));
                
                if (isSSLException && attempt < maxRetries) {
                    log.debug("SSL握手失败，尝试次数: {}/{}, 本次耗时={}ms, 将重试: {}", 
                            attempt, maxRetries, attemptDuration, e.getMessage());
                    try {
                        Thread.sleep(1000 * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("请求被中断", ie);
                    }
                    continue;
                } else {
                    // SSL错误且达到最大重试次数，尝试使用HttpUtil作为备选方案
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.warn("HttpClient SSL握手最终失败，总耗时={}ms，尝试使用HttpUtil备选方案: {}", totalDuration, url);
                    return postWithHttpUtil(url, body, headers);
                }
            } catch (IOException | InterruptedException e) {
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("HTTP POST请求异常，尝试次数: {}/{}, 本次耗时={}ms, 将重试: {}", 
                            attempt, maxRetries, attemptDuration, e.getMessage());
                    try {
                        Thread.sleep(1000 * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("请求被中断", ie);
                    }
                    continue;
                } else {
                    long totalDuration = System.currentTimeMillis() - totalStartTime;
                    log.error("HTTP POST请求最终失败，URL: {}, 总耗时={}ms", url, totalDuration, e);
                    throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
                }
            }
        }
        
        // 如果所有重试都失败，尝试使用HttpUtil作为最后备选方案
        if (lastException != null) {
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.warn("所有重试都失败，总耗时={}ms，尝试使用HttpUtil备选方案: {}", totalDuration, url);
            return postWithHttpUtil(url, body, headers);
        }
        
        throw new RuntimeException("HTTP请求失败，未知错误");
    }
    
    /**
     * 使用HttpUtil作为POST备选方案（有重试机制）
     */
    private String postWithHttpUtil(String url, String body, java.util.Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, String> finalHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
            if (!finalHeaders.containsKey("User-Agent")) {
                finalHeaders.put("User-Agent", "Mozilla/5.0");
            }
            String response = com.lvxin.base.util.HttpUtil.post(url, body, finalHeaders, 30);
            long duration = System.currentTimeMillis() - startTime;
            if (response != null && !response.isEmpty()) {
                log.info("使用HttpUtil备选方案成功: URL={}, 耗时={}ms", url, duration);
                return response;
            } else {
                log.error("HttpUtil备选方案返回空响应: URL={}, 耗时={}ms", url, duration);
                throw new RuntimeException("HttpUtil备选方案返回空响应");
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("HttpUtil备选方案也失败: URL={}, 耗时={}ms", url, duration, e);
            throw new RuntimeException("HTTP请求失败，所有方案都失败: " + e.getMessage(), e);
        }
    }
}
