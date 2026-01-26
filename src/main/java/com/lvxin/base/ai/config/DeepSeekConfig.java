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
    
    /**
     * Temperature参数（0.0-2.0），控制输出的随机性
     * 较低的值使输出更确定，较高值使输出更随机
     * 默认0.7，可以降低到0.3-0.5以加快速度
     */
    private Double temperature = 0.7;
    
    /**
     * 最大输出token数
     * 降低此值可以减少生成时间，但可能截断输出
     * 默认8000，可以根据实际需求调整（如4000-6000）
     */
    private Integer maxTokens = 8000;
    
    /**
     * 工具调用策略
     * "auto": 让模型自动决定是否调用工具（默认）
     * "none": 禁用工具调用，加快速度但失去工具功能
     * "required": 强制调用工具（如果配置了工具）
     */
    private String toolChoice = "auto";
}
