package com.lvxin.base.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SSE响应过滤器
 * 确保SSE流式响应不包含Content-Length头，使用Transfer-Encoding: chunked
 *
 * @author lvxin
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE) // 确保在所有其他Filter之后执行
public class SseResponseFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        
        // 只处理SSE流式端点
        if (uri != null && uri.contains("/chat/stream")) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 使用包装器拦截setHeader调用，防止Content-Length被设置
            HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(httpResponse) {
                @Override
                public void setContentLength(int len) {
                    // 忽略Content-Length设置
                    log.debug("阻止设置Content-Length: {}", len);
                }
                
                @Override
                public void setContentLengthLong(long len) {
                    // 忽略Content-Length设置
                    log.debug("阻止设置Content-Length: {}", len);
                }
                
                @Override
                public void setHeader(String name, String value) {
                    if ("Content-Length".equalsIgnoreCase(name)) {
                        // 忽略Content-Length头设置
                        log.debug("阻止设置Content-Length头: {}", value);
                        return;
                    }
                    super.setHeader(name, value);
                }
                
                @Override
                public void addHeader(String name, String value) {
                    if ("Content-Length".equalsIgnoreCase(name)) {
                        // 忽略Content-Length头添加
                        log.debug("阻止添加Content-Length头: {}", value);
                        return;
                    }
                    super.addHeader(name, value);
                }
            };
            
            chain.doFilter(request, wrappedResponse);
            
            // 确保最终响应中没有Content-Length头
            httpResponse.setHeader("Content-Length", null);
            log.debug("SSE响应处理完成，已确保移除Content-Length头");
        } else {
            chain.doFilter(request, response);
        }
    }
}
