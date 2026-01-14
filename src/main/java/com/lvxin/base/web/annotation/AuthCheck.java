package com.lvxin.base.web.annotation;

import java.lang.annotation.*;

/**
 * 接口权限控制注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthCheck {

    /**
     * 权限点列表
     */
    String[] value() default {};

    /**
     * 匹配模式：ALL 表示全部命中；ANY 表示命中任一
     */
    MatchMode matchMode() default MatchMode.ALL;

    enum MatchMode {
        ALL,
        ANY
    }
}
