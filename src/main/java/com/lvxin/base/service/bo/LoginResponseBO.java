package com.lvxin.base.service.bo;

import lombok.Data;

/**
 * 登录响应业务对象
 * 统一处理各种登录方式的响应结果
 */
@Data
public class LoginResponseBO {

    /**
     * 登录是否成功
     */
    private Boolean success;

    /**
     * 登录状态
     */
    private Integer loginStatus;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 访问令牌
     */
    private String accessToken;


    /**
     * 令牌过期时间
     */
    private Long tokenExpireTime;

    /**
     * 是否新用户
     */
    private Boolean isNewUser;

    /**
     * 是否需要绑定手机号
     */
    private Boolean needBindPhone;

    /**
     * 是否需要完善信息
     */
    private Boolean needCompleteInfo;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo {
        private String nickname;
        private String avatarUrl;
        private String email;
        private Integer gender;
        private String country;
        private String province;
        private String city;
    }
}
