package com.lvxin.base.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
     * 发送GET请求
     *
     * @param url 请求URL
     * @return 响应内容
     */
    public String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                log.error("HTTP GET请求失败，状态码: {}, URL: {}", response.statusCode(), url);
                throw new RuntimeException("HTTP请求失败，状态码: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("HTTP GET请求异常，URL: {}", url, e);
            throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头（可选）
     * @return 响应内容
     */
    public String post(String url, String body, java.util.Map<String, String> headers) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                log.error("HTTP POST请求失败，状态码: {}, URL: {}", response.statusCode(), url);
                throw new RuntimeException("HTTP请求失败，状态码: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("HTTP POST请求异常，URL: {}", url, e);
            throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
        }
    }
}
