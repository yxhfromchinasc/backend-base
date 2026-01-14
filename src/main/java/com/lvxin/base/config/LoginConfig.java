package com.lvxin.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 登录配置类
 * 管理登录相关的配置信息
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "login")
public class LoginConfig {

    /**
     * 手机号验证码配置
     */
    private SmsConfig sms = new SmsConfig();

    /**
     * 通用配置
     */
    private CommonConfig common = new CommonConfig();

    /**
     * 手机号验证码配置
     */
    @Data
    public static class SmsConfig {
        private Integer codeLength = 6;
        private Integer expireMinutes = 5;
        private Integer sendInterval = 60;
        private Integer dailyLimit = 10;
        private String template = "您的验证码是{code}，{expire}分钟内有效，请勿泄露给他人。";
    }

    /**
     * 通用配置
     */
    @Data
    public static class CommonConfig {
        private Integer maxFailAttempts = 5;
        private Integer lockDurationMinutes = 30;
        private Integer sessionExpireHours = 24;
        private Boolean enableLoginLog = true;
        private Boolean enableIpWhitelist = false;
        private List<String> ipWhitelist;
    }
}
