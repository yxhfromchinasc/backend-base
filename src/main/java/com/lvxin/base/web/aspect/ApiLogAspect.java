package com.lvxin.base.web.aspect;

import com.lvxin.base.web.annotation.ApiLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * API 日志切面
 * 自动记录 Controller 方法的入参和出参
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 拦截所有 Controller 包下的方法
     */
    @Around("execution(* com.lvxin.base.web.controller.*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        // 获取注解配置
        ApiLog apiLog = method.getAnnotation(ApiLog.class);
        boolean logRequest = apiLog == null || apiLog.logRequest();
        boolean logResponse = apiLog == null || apiLog.logResponse();

        // 构建日志上下文
        Map<String, Object> logContext = new HashMap<>();
        logContext.put("event", "API_LOG");
        logContext.put("className", className);
        logContext.put("methodName", methodName);
        logContext.put("description", apiLog != null ? apiLog.value() : "");

        if (request != null) {
            logContext.put("requestMethod", request.getMethod());
            logContext.put("requestURI", request.getRequestURI());
            logContext.put("remoteAddr", getClientIpAddress(request));
        }

        // 记录入参
        if (logRequest) {
            Object[] args = joinPoint.getArgs();
            Map<String, Object> requestParams = new HashMap<>();

            // 获取参数名和值
            String[] paramNames = signature.getParameterNames();
            for (int i = 0; i < args.length; i++) {
                if (i < paramNames.length) {
                    requestParams.put(paramNames[i], args[i]);
                } else {
                    requestParams.put("arg" + i, args[i]);
                }
            }

            logContext.put("requestParams", requestParams);
            logContext.put("logType", "REQUEST");

            log.info("API请求日志: {}", objectMapper.writeValueAsString(logContext));
        }

        // 执行方法
        Object result = null;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;

            // 记录出参
            if (logResponse) {
                Map<String, Object> responseLog = new HashMap<>(logContext);
                responseLog.put("logType", "RESPONSE");
                responseLog.put("costTime", costTime + "ms");
                responseLog.put("success", exception == null);

                if (exception != null) {
                    responseLog.put("errorMessage", exception.getMessage());
                    responseLog.put("errorType", exception.getClass().getSimpleName());
                } else {
                    responseLog.put("responseData", result);
                }

                log.info("API响应日志: {}", objectMapper.writeValueAsString(responseLog));
            }
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
