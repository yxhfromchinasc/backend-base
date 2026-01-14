package com.lvxin.base.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 开发模式配置类
 * 用于控制开发环境下的各种模拟行为
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "dev")
public class DevConfig {

    /**
     * 是否启用开发模式
     */
    private Boolean enabled = false;

    /**
     * 模拟数据配置
     */
    private MockConfig mock = new MockConfig();

    @Data
    public static class MockConfig {
        /**
         * 短信相关模拟数据
         */
        private SmsMockConfig sms = new SmsMockConfig();

        /**
         * 邮件相关模拟数据
         */
        private EmailMockConfig email = new EmailMockConfig();
    }

    @Data
    public static class SmsMockConfig {
        /**
         * 模拟的短信验证码
         */
        private String code = "123456";
    }

    @Data
    public static class EmailMockConfig {
        /**
         * 模拟的邮件验证码
         */
        private String code = "123456";
    }

    /**
     * 检查是否启用开发模式
     *
     * @return 是否启用开发模式
     */
    public boolean isDevMode() {
        return enabled != null && enabled;
    }

    /**
     * 获取模拟的短信验证码
     *
     * @return 模拟的短信验证码
     */
    public String getMockSmsCode() {
        if (!isDevMode()) {
            log.warn("开发模式未启用，无法获取模拟数据");
            return null;
        }
        return mock.getSms().getCode();
    }

    /**
     * 获取模拟的邮件验证码
     *
     * @return 模拟的邮件验证码
     */
    public String getMockEmailCode() {
        if (!isDevMode()) {
            log.warn("开发模式未启用，无法获取模拟数据");
            return null;
        }
        return mock.getEmail().getCode();
    }

    /**
     * 记录开发模式日志
     *
     * @param message 日志消息
     */
    public void logDevMode(String message) {
        if (isDevMode()) {
            log.info("开发模式: {}", message);
        }
    }
}
