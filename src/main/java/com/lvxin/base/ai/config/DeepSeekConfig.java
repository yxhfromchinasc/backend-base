package com.lvxin.base.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek配置
 *
 * @author lvxin
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekConfig {
    /**
     * API Key
     */
    private String apiKey;

    /**
     * API Base URL
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * 默认模型名称
     * 推荐：deepseek-reasoner（支持Function Calling）、deepseek-chat
     */
    private String defaultModel = "deepseek-reasoner";

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 360000; // 默认6分钟

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;
    
    /**
     * 是否启用工具调用
     */
    private Boolean enableTools = true;
}
