package com.lvxin.base.web.resp;

import lombok.Data;

/**
 * 登录响应对象
 * Controller层响应参数
 */
@Data
public class LoginResp {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 访问令牌
     */
    private String accessToken;


    /**
     * 登录方式
     */
    private String loginMethod;

    /**
     * 登录状态
     */
    private String loginStatus;

    /**
     * 是否需要绑定手机号
     */
    private Boolean needBindPhone;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 登录时间
     */
    private String loginTime;

    /**
     * 过期时间
     */
    private String expireTime;
}
