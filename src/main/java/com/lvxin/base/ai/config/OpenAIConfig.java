package com.lvxin.base.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI配置
 *
 * @author lvxin
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.openai")
public class OpenAIConfig {
    /**
     * API Key
     */
    private String apiKey;

    /**
     * 默认模型名称
     * 推荐：gpt-4o（最新，支持工具调用）、gpt-4-turbo、gpt-3.5-turbo
     */
    private String defaultModel = "gpt-4o";

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 360000; // 默认6分钟

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;
    
    /**
     * 是否启用Web Search工具
     */
    private Boolean enableWebSearch = true;
    
    /**
     * Base URL（可选，用于代理或自定义端点）
     */
    private String baseUrl;
}
