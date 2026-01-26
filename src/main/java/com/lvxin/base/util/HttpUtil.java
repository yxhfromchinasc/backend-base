package com.lvxin.base.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求工具类
 * 提供常用的GET和POST请求方法
 */
@Slf4j
@Component
public class HttpUtil {

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 1000;

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @return 响应内容
     */
    public static String get(String url) {
        return get(url, null, null);
    }

    /**
     * 发送GET请求（带请求头）
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应内容
     */
    public static String get(String url, Map<String, String> headers) {
        return get(url, headers, null);
    }

    /**
     * 发送GET请求（带请求头和超时设置）
     *
     * @param url            请求URL
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public static String get(String url, Map<String, String> headers, Integer timeoutSeconds) {
        return sendRequest("GET", url, null, headers, timeoutSeconds, null);
    }

    /**
     * 发送POST请求
     *
     * @param url  请求URL
     * @param body 请求体
     * @return 响应内容
     */
    public static String post(String url, String body) {
        return post(url, body, null, null);
    }

    /**
     * 发送POST请求（带请求头）
     *
     * @param url     请求URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public static String post(String url, String body, Map<String, String> headers) {
        return post(url, body, headers, null);
    }

    /**
     * 发送POST请求（带请求头和超时设置）
     *
     * @param url            请求URL
     * @param body           请求体
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public static String post(String url, String body, Map<String, String> headers, Integer timeoutSeconds) {
        return sendRequest("POST", url, body, headers, timeoutSeconds, null);
    }

    /**
     * 发送POST请求（带重试机制）
     *
     * @param url            请求URL
     * @param body           请求体
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @param maxRetries     最大重试次数
     * @return 响应内容
     */
    public static String post(String url, String body, Map<String, String> headers, Integer timeoutSeconds, Integer maxRetries) {
        return sendRequest("POST", url, body, headers, timeoutSeconds, maxRetries);
    }

    /**
     * 发送GET请求（带重试机制）
     *
     * @param url            请求URL
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @param maxRetries     最大重试次数
     * @return 响应内容
     */
    public static String get(String url, Map<String, String> headers, Integer timeoutSeconds, Integer maxRetries) {
        return sendRequest("GET", url, null, headers, timeoutSeconds, maxRetries);
    }

    /**
     * 发送HTTP请求的核心方法
     *
     * @param method         请求方法
     * @param url            请求URL
     * @param body           请求体
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @param maxRetries     最大重试次数
     * @return 响应内容
     */
    private static String sendRequest(String method, String url, String body, Map<String, String> headers,
                                      Integer timeoutSeconds, Integer maxRetries) {
        int actualTimeout = timeoutSeconds != null ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
        int actualMaxRetries = maxRetries != null ? maxRetries : DEFAULT_MAX_RETRIES;

        for (int attempt = 1; attempt <= actualMaxRetries; attempt++) {
            try {
                log.debug("发送HTTP请求，方法: {}, URL: {}, 尝试次数: {}/{}", method, url, attempt, actualMaxRetries);

                // 创建HttpClient
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS))
                        .build();

                // 构建请求
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(actualTimeout))
                        .header("User-Agent", "HttpUtil/1.0");

                // 设置请求方法
                if ("GET".equals(method)) {
                    requestBuilder.GET();
                } else if ("POST".equals(method)) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                } else if ("PUT".equals(method)) {
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                } else if ("PATCH".equals(method)) {
                    requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                }

                // 添加自定义请求头
                if (headers != null && !headers.isEmpty()) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        requestBuilder.header(entry.getKey(), entry.getValue());
                    }
                }

                HttpRequest request = requestBuilder.build();

                // 发送请求
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // 检查响应状态码
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.debug("HTTP请求成功，状态码: {}, 响应长度: {}", response.statusCode(),
                            response.body() != null ? response.body().length() : 0);
                    return response.body();
                } else {
                    log.warn("HTTP请求失败，状态码: {}, 尝试次数: {}/{}", response.statusCode(), attempt, actualMaxRetries);

                    if (attempt == actualMaxRetries) {
                        log.error("HTTP请求最终失败，状态码: {}, URL: {}", response.statusCode(), url);
                        return null;
                    }

                    // 等待后重试
                    Thread.sleep(DEFAULT_RETRY_DELAY_MS * attempt);
                }

            } catch (java.net.ConnectException e) {
                log.warn("网络连接失败，尝试次数: {}/{}, 错误: {}", attempt, actualMaxRetries, e.getMessage());
                if (attempt == actualMaxRetries) {
                    log.error("网络连接最终失败，URL: {}", url, e);
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);

            } catch (java.net.http.HttpTimeoutException e) {
                log.warn("HTTP请求超时，尝试次数: {}/{}, 错误: {}", attempt, actualMaxRetries, e.getMessage());
                if (attempt == actualMaxRetries) {
                    log.error("HTTP请求最终超时，URL: {}", url, e);
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);

            } catch (IOException e) {
                // SSL握手异常通常是重试过程中的正常情况，使用DEBUG级别
                boolean isSSLException = e.getMessage() != null && 
                    (e.getMessage().contains("SSL") || 
                     e.getMessage().contains("handshake") ||
                     e.getMessage().contains("terminated"));
                
                if (isSSLException && attempt < actualMaxRetries) {
                    // SSL握手失败但还有重试机会，使用DEBUG级别
                    log.debug("SSL握手失败，尝试次数: {}/{}, 将重试: {}", attempt, actualMaxRetries, e.getMessage());
                } else {
                    // 其他IO异常或最后一次尝试，使用WARN级别
                    log.warn("IO异常，尝试次数: {}/{}, 错误: {}", attempt, actualMaxRetries, e.getMessage());
                }
                
                if (attempt == actualMaxRetries) {
                    if (isSSLException) {
                        log.warn("SSL握手最终失败，URL: {}, 错误: {}", url, e.getMessage());
                    } else {
                        log.error("IO异常最终失败，URL: {}", url, e);
                    }
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);

            } catch (Exception e) {
                log.error("发送HTTP请求失败，尝试次数: {}/{}, URL: {}", attempt, actualMaxRetries, url, e);
                if (attempt == actualMaxRetries) {
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);
            }
        }

        return null;
    }

    /**
     * 安全的睡眠方法，处理中断异常
     *
     * @param millis 睡眠时间（毫秒）
     */
    private static void sleepWithInterrupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("线程被中断，停止重试");
        }
    }

    /**
     * 发送JSON POST请求
     *
     * @param url      请求URL
     * @param jsonBody JSON请求体
     * @return 响应内容
     */
    public static String postJson(String url, String jsonBody) {
        return postJson(url, jsonBody, null, null);
    }

    /**
     * 发送JSON POST请求（带请求头）
     *
     * @param url      请求URL
     * @param jsonBody JSON请求体
     * @param headers  请求头
     * @return 响应内容
     */
    public static String postJson(String url, String jsonBody, Map<String, String> headers) {
        return postJson(url, jsonBody, headers, null);
    }

    /**
     * 发送JSON POST请求（带请求头和超时设置）
     *
     * @param url            请求URL
     * @param jsonBody       JSON请求体
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public static String postJson(String url, String jsonBody, Map<String, String> headers, Integer timeoutSeconds) {
        // 添加JSON请求头
        if (headers != null) {
            headers.put("Content-Type", "application/json");
        } else {
            headers = Map.of("Content-Type", "application/json");
        }

        return post(url, jsonBody, headers, timeoutSeconds);
    }

    /**
     * 发送JSON PUT请求
     *
     * @param url            请求URL
     * @param jsonBody       JSON请求体
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public static String putJson(String url, String jsonBody, Map<String, String> headers, Integer timeoutSeconds) {
        // 添加JSON请求头
        Map<String, String> actualHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        actualHeaders.put("Content-Type", "application/json");

        return sendRequest("PUT", url, jsonBody, actualHeaders, timeoutSeconds, null);
    }

    /**
     * 发送JSON PATCH请求
     *
     * @param url            请求URL
     * @param jsonBody       JSON请求体
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public static String patchJson(String url, String jsonBody, Map<String, String> headers, Integer timeoutSeconds) {
        // 添加JSON请求头
        Map<String, String> actualHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        actualHeaders.put("Content-Type", "application/json");

        return sendRequest("PATCH", url, jsonBody, actualHeaders, timeoutSeconds, null);
    }

    /**
     * 发送表单POST请求
     *
     * @param url      请求URL
     * @param formData 表单数据
     * @return 响应内容
     */
    public static String postForm(String url, String formData) {
        return postForm(url, formData, null, null);
    }

    /**
     * 发送表单POST请求（带请求头）
     *
     * @param url      请求URL
     * @param formData 表单数据
     * @param headers  请求头
     * @return 响应内容
     */
    public static String postForm(String url, String formData, Map<String, String> headers) {
        return postForm(url, formData, headers, null);
    }

    /**
     * 发送表单POST请求（带请求头和超时设置）
     *
     * @param url            请求URL
     * @param formData       表单数据
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public static String postForm(String url, String formData, Map<String, String> headers, Integer timeoutSeconds) {
        // 添加表单请求头
        if (headers != null) {
            headers.put("Content-Type", "application/x-www-form-urlencoded");
        } else {
            headers = Map.of("Content-Type", "application/x-www-form-urlencoded");
        }

        return post(url, formData, headers, timeoutSeconds);
    }

    /**
     * 发送XML POST请求
     *
     * @param url     请求URL
     * @param xmlBody XML请求体
     * @return 响应内容
     */
    public static String postXml(String url, String xmlBody) {
        return postXml(url, xmlBody, null, null);
    }

    /**
     * 发送XML POST请求（带请求头）
     *
     * @param url     请求URL
     * @param xmlBody XML请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public static String postXml(String url, String xmlBody, Map<String, String> headers) {
        return postXml(url, xmlBody, headers, null);
    }

    /**
     * 发送XML POST请求（带请求头和超时设置）
     *
     * @param url            请求URL
     * @param xmlBody        XML请求体
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public static String postXml(String url, String xmlBody, Map<String, String> headers, Integer timeoutSeconds) {
        // 添加XML请求头
        if (headers != null) {
            headers.put("Content-Type", "application/xml");
        } else {
            headers = Map.of("Content-Type", "application/xml");
        }

        return post(url, xmlBody, headers, timeoutSeconds);
    }

    /**
     * 下载文件（返回字节数组）
     *
     * @param url            文件URL
     * @param headers        请求头
     * @param timeoutSeconds 超时时间（秒）
     * @return 文件字节数组，失败返回null
     */
    public static byte[] downloadFile(String url, Map<String, String> headers, Integer timeoutSeconds) {
        int actualTimeout = timeoutSeconds != null ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
        int actualMaxRetries = DEFAULT_MAX_RETRIES;

        for (int attempt = 1; attempt <= actualMaxRetries; attempt++) {
            try {
                log.debug("下载文件，URL: {}, 尝试次数: {}/{}", url, attempt, actualMaxRetries);

                // 创建HttpClient
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS))
                        .build();

                // 构建请求
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(actualTimeout))
                        .GET();

                // 添加自定义请求头
                if (headers != null && !headers.isEmpty()) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        requestBuilder.header(entry.getKey(), entry.getValue());
                    }
                } else {
                    requestBuilder.header("User-Agent", "HttpUtil/1.0");
                }

                HttpRequest request = requestBuilder.build();

                // 发送请求
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                // 检查响应状态码
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    byte[] data = response.body();
                    log.debug("文件下载成功，URL: {}, 大小: {} bytes", url, data != null ? data.length : 0);
                    return data;
                } else {
                    log.warn("文件下载失败，状态码: {}, 尝试次数: {}/{}", response.statusCode(), attempt, actualMaxRetries);

                    if (attempt == actualMaxRetries) {
                        log.error("文件下载最终失败，状态码: {}, URL: {}", response.statusCode(), url);
                        return null;
                    }

                    // 等待后重试
                    sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);
                }

            } catch (java.net.ConnectException e) {
                log.warn("网络连接失败，尝试次数: {}/{}, 错误: {}", attempt, actualMaxRetries, e.getMessage());
                if (attempt == actualMaxRetries) {
                    log.error("网络连接最终失败，URL: {}", url, e);
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);

            } catch (java.net.http.HttpTimeoutException e) {
                log.warn("文件下载超时，尝试次数: {}/{}, 错误: {}", attempt, actualMaxRetries, e.getMessage());
                if (attempt == actualMaxRetries) {
                    log.error("文件下载最终超时，URL: {}", url, e);
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);

            } catch (IOException e) {
                boolean isSSLException = e.getMessage() != null &&
                    (e.getMessage().contains("SSL") ||
                     e.getMessage().contains("handshake") ||
                     e.getMessage().contains("terminated"));

                if (isSSLException && attempt < actualMaxRetries) {
                    log.debug("SSL握手失败，尝试次数: {}/{}, 将重试: {}", attempt, actualMaxRetries, e.getMessage());
                } else {
                    log.warn("IO异常，尝试次数: {}/{}, 错误: {}", attempt, actualMaxRetries, e.getMessage());
                }

                if (attempt == actualMaxRetries) {
                    if (isSSLException) {
                        log.warn("SSL握手最终失败，URL: {}, 错误: {}", url, e.getMessage());
                    } else {
                        log.error("IO异常最终失败，URL: {}", url, e);
                    }
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("文件下载被中断，URL: {}", url, e);
                return null;
            } catch (Exception e) {
                log.error("文件下载异常，URL: {}", url, e);
                if (attempt == actualMaxRetries) {
                    return null;
                }
                sleepWithInterrupt(DEFAULT_RETRY_DELAY_MS * attempt);
            }
        }

        return null;
    }
}
