package com.lvxin.base.web.annotation;

import java.lang.annotation.*;

/**
 * API 日志注解
 * 用于标记需要记录入参/出参日志的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    /**
     * 接口描述
     */
    String value() default "";

    /**
     * 是否记录入参
     */
    boolean logRequest() default true;

    /**
     * 是否记录出参
     */
    boolean logResponse() default true;
}
