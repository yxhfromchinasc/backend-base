package com.lvxin.base.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录状态枚举
 * 定义登录过程中的各种状态
 */
@Getter
@AllArgsConstructor
public enum LoginStatus {

    /**
     * 登录成功
     */
    SUCCESS(1, "登录成功"),

    /**
     * 登录失败
     */
    FAILED(2, "登录失败"),

    /**
     * 需要验证码
     */
    NEED_VERIFICATION(3, "需要验证码"),

    /**
     * 需要绑定手机号
     */
    NEED_BIND_PHONE(4, "需要绑定手机号"),

    /**
     * 需要完善信息
     */
    NEED_COMPLETE_INFO(5, "需要完善信息"),

    /**
     * 账户被锁定
     */
    ACCOUNT_LOCKED(6, "账户被锁定"),

    /**
     * 验证码错误
     */
    VERIFICATION_ERROR(7, "验证码错误"),

    /**
     * 密码错误
     */
    PASSWORD_ERROR(8, "密码错误"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(9, "用户不存在"),

    /**
     * 登录过期
     */
    LOGIN_EXPIRED(10, "登录过期");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 登录状态枚举
     */
    public static LoginStatus getByCode(Integer code) {
        for (LoginStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的登录状态码: " + code);
    }
}
