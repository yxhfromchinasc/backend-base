package com.lvxin.base.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Grok (xAI) 配置类
 *
 * @author lvxin
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.grok")
public class GrokConfig {
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * API Base URL
     */
    private String baseUrl = "https://api.x.ai";
    
    /**
     * 默认模型
     */
    private String defaultModel = "grok-4-1-fast-reasoning";
    
    /**
     * 超时时间（毫秒）
     */
    private Long timeout = 360000L;
    
    /**
     * 是否启用工具调用
     */
    private Boolean enableTools = false;
}
