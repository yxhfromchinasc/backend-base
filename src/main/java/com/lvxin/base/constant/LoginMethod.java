package com.lvxin.base.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录方式枚举
 * 定义支持的登录方式
 */
@Getter
@AllArgsConstructor
public enum LoginMethod {

    /**
     * 手机号密码登录
     */
    PHONE_PASSWORD(1, "手机号密码登录"),

    /**
     * 手机号验证码登录
     */
    PHONE_SMS(2, "手机号验证码登录");

    /**
     * 登录方式码
     */
    private final Integer code;

    /**
     * 登录方式描述
     */
    private final String description;

    /**
     * 根据登录方式码获取枚举
     *
     * @param code 登录方式码
     * @return 登录方式枚举
     */
    public static LoginMethod getByCode(Integer code) {
        for (LoginMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("无效的登录方式码: " + code);
    }
}
