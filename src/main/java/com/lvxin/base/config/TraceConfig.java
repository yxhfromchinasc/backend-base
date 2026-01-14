package com.lvxin.base.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
public class TraceConfig {

    @Bean
    @ConfigurationProperties(prefix = "tracing")
    public TraceProperties traceProperties() {
        return new TraceProperties();
    }

    @Bean
    public Filter traceFilter(TraceProperties props) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain filterChain) throws ServletException, IOException {
                // 使用缓存包装器，以便多次读取请求体和响应体
                ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(req);
                ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(resp);
                
                String traceIdHeader = props.getHeaderName();
                String incoming = req.getHeader(traceIdHeader);
                String traceId = (incoming == null || incoming.isEmpty()) ? UUID.randomUUID().toString() : incoming;
                MDC.put(props.getMdcKey(), traceId);
                // 在包装响应和原始响应上都设置 traceId，确保最终响应包含该 header
                wrappedResponse.setHeader(traceIdHeader, traceId);
                resp.setHeader(traceIdHeader, traceId);
                
                String method = req.getMethod();
                String uri = req.getRequestURI();
                String qs = req.getQueryString();
                String url = qs == null ? uri : uri + "?" + qs;
                long startNs = System.nanoTime();
                org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("HTTP_TRACE");
                
                Map<String, Object> requestInfo = null;
                if (logger.isInfoEnabled()) {
                    requestInfo = new HashMap<>();
                    requestInfo.put("type", "REQ");
                    requestInfo.put("method", method);
                    requestInfo.put("url", url);
                    
                    // 添加请求的IP、设备等信息
                    String clientIp = getClientIp(req);
                    if (clientIp != null) {
                        requestInfo.put("clientIp", clientIp);
                    }
                    String userAgent = req.getHeader("User-Agent");
                    if (userAgent != null && !userAgent.isEmpty()) {
                        requestInfo.put("userAgent", userAgent);
                    }
                }
                
                try {
                    // 执行过滤器链
                    filterChain.doFilter(wrappedRequest, wrappedResponse);
                    
                } finally {
                    long costMs = (System.nanoTime() - startNs) / 1_000_000;
                    
                    // 先输出请求入口日志（此时请求体已被缓存，可读取）
                    if (logger.isInfoEnabled() && requestInfo != null) {
                        String requestBodyForReq = getRequestBody(wrappedRequest);
                        if (requestBodyForReq != null && !requestBodyForReq.isEmpty()) {
                            try {
                                Object jsonBody = objectMapper.readValue(requestBodyForReq, Object.class);
                                requestInfo.put("requestBody", jsonBody);
                            } catch (Exception e) {
                                if (requestBodyForReq.length() > 500) {
                                    requestInfo.put("requestBody", requestBodyForReq.substring(0, 500) + "...");
                                } else {
                                    requestInfo.put("requestBody", requestBodyForReq);
                                }
                            }
                        }
                        
                        try {
                            logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestInfo));
                        } catch (Exception e) {
                            logger.info("REQ method={} url={} clientIp={} userAgent={} body={}",
                                    requestInfo.get("method"),
                                    requestInfo.get("url"),
                                    requestInfo.get("clientIp"),
                                    requestInfo.get("userAgent"),
                                    requestInfo.getOrDefault("requestBody", ""));
                        }
                    }
                    
                    // 记录响应信息（出口日志）
                    if (logger.isInfoEnabled()) {
                        Map<String, Object> responseInfo = new HashMap<>();
                        responseInfo.put("type", "RESP");
                        responseInfo.put("method", method);
                        responseInfo.put("url", url);
                        responseInfo.put("status", wrappedResponse.getStatus());
                        responseInfo.put("costMs", costMs);
                        
                        // 记录请求体（POST/PUT/PATCH 请求）
                        String requestBody = getRequestBody(wrappedRequest);
                        if (requestBody != null && !requestBody.isEmpty()) {
                            // 尝试解析 JSON，如果失败则原样记录
                            try {
                                Object jsonBody = objectMapper.readValue(requestBody, Object.class);
                                responseInfo.put("requestBody", jsonBody);
                            } catch (Exception e) {
                                // 不是 JSON，直接记录字符串（截断过长内容）
                                if (requestBody.length() > 500) {
                                    responseInfo.put("requestBody", requestBody.substring(0, 500) + "...");
                                } else {
                                    responseInfo.put("requestBody", requestBody);
                                }
                            }
                        }
                        
                        // 记录响应体（完整记录）
                        String responseBody = getResponseBody(wrappedResponse);
                        if (responseBody != null && !responseBody.isEmpty()) {
                            // 尝试解析 JSON，如果失败则原样记录
                            try {
                                Object jsonBody = objectMapper.readValue(responseBody, Object.class);
                                responseInfo.put("responseBody", jsonBody);
                            } catch (Exception e) {
                                // 不是 JSON，直接记录字符串（完整记录，不截断）
                                responseInfo.put("responseBody", responseBody);
                            }
                        }
                        
                        // 格式化输出 JSON（带换行和缩进，便于阅读）
                        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseInfo));
                    }
                    
                    // 将缓存的响应体写回原始响应
                    wrappedResponse.copyBodyToResponse();
                    
                    // 确保 traceId 在最终响应中（copyBodyToResponse 后再次设置，确保 header 存在）
                    resp.setHeader(traceIdHeader, traceId);
                    
                    MDC.remove(props.getMdcKey());
                }
            }
            
            /**
             * 获取客户端IP（从 HttpServletRequest）
             * 优先解析反向代理/负载均衡常见头：X-Forwarded-For、X-Real-IP 等
             */
            private String getClientIp(HttpServletRequest request) {
                if (request == null) {
                    return null;
                }

                // 常见代理头按优先级依次解析
                String[] headerNames = new String[]{
                        "X-Forwarded-For",
                        "X-Real-IP",
                        "Proxy-Client-IP",
                        "WL-Proxy-Client-IP",
                        "HTTP_CLIENT_IP",
                        "HTTP_X_FORWARDED_FOR"
                };

                for (String header : headerNames) {
                    String ip = request.getHeader(header);
                    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                        // X-Forwarded-For 可能包含多个IP，取第一个非unknown
                        if (ip.contains(",")) {
                            String[] parts = ip.split(",");
                            for (String part : parts) {
                                String candidate = part.trim();
                                if (candidate != null && !candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                                    return candidate;
                                }
                            }
                        }
                        return ip.trim();
                    }
                }

                // 兜底：直接使用远端地址
                String remoteAddr = request.getRemoteAddr();
                return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : null;
            }
            
            private String getRequestBody(ContentCachingRequestWrapper request) {
                byte[] content = request.getContentAsByteArray();
                if (content.length == 0) {
                    return null;
                }
                return new String(content, StandardCharsets.UTF_8);
            }
            
            private String getResponseBody(ContentCachingResponseWrapper response) {
                byte[] content = response.getContentAsByteArray();
                if (content.length == 0) {
                    return null;
                }
                return new String(content, StandardCharsets.UTF_8);
            }
        };
    }

    @Setter
    @Getter
    public static class TraceProperties {
        private String headerName = "X-Trace-Id";
        private String mdcKey = "traceId";

    }
}



