package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 登录请求对象
 * Controller层请求参数
 */
@Data
public class LoginReq {

    /**
     * 登录方式
     */
    @NotNull(message = "登录方式不能为空")
    private String loginMethod;

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
     * 客户端IP
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * 邀请码（可选，格式：RICH + userid）
     * 用于建立推荐关系，仅在注册新用户时使用
     */
    private String referralCode;
}
