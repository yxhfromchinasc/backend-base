package com.lvxin.base.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云DashScope（通义千问）配置
 *
 * @author lvxin
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.dashscope")
public class DashScopeConfig {
    /**
     * API Key
     */
    private String apiKey;

    /**
     * 默认模型名称
     */
    private String defaultModel = "qwen-plus";

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 360000; // 默认6分钟（360秒），AI生成计划需要较长时间

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;
}
