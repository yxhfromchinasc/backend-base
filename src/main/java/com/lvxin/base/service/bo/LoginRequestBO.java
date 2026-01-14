package com.lvxin.base.service.bo;

import lombok.Data;

/**
 * 登录请求业务对象
 * 统一处理各种登录方式的请求参数
 */
@Data
public class LoginRequestBO {

    /**
     * 登录方式
     */
    private Integer loginMethod;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 验证码
     */
    private String verificationCode;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

}
