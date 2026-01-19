package com.lvxin.base.ai.config;

import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.client.DashScopeClient;
import com.lvxin.base.ai.client.DeepSeekClient;
import com.lvxin.base.ai.client.OpenAIClient;
import com.lvxin.base.ai.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI客户端配置
 * 根据配置自动选择使用DeepSeek、OpenAI或DashScope
 * 优先级：DeepSeek > OpenAI > DashScope
 *
 * @author lvxin
 */
@Slf4j
@Configuration
public class AIClientConfig {
    
    /**
     * 如果配置了DeepSeek，优先使用DeepSeek（支持工具调用）
     */
    @Bean("aiClient")
    @Primary
    @ConditionalOnProperty(prefix = "ai.deepseek", name = "api-key")
    public AIClient deepSeekClient(DeepSeekConfig config, ToolManager toolManager) {
        log.info("使用DeepSeek-R1作为AI服务提供商，模型: {}，工具调用: {}", 
                config.getDefaultModel(), config.getEnableTools());
        return new DeepSeekClient(config, toolManager);
    }
    
    /**
     * 如果配置了OpenAI，使用OpenAI
     */
    @Bean("aiClient")
    @ConditionalOnProperty(prefix = "ai.openai", name = "api-key")
    @ConditionalOnMissingBean(name = "aiClient")
    public AIClient openAIClient(OpenAIConfig config) {
        log.info("使用OpenAI作为AI服务提供商，模型: {}", config.getDefaultModel());
        return new OpenAIClient(config);
    }
    
    /**
     * 如果未配置其他服务，使用DashScope作为默认
     */
    @Bean("aiClient")
    @ConditionalOnMissingBean(name = "aiClient")
    public AIClient dashScopeClient(DashScopeConfig config) {
        log.info("使用DashScope（通义千问）作为AI服务提供商，模型: {}", config.getDefaultModel());
        return new DashScopeClient(config);
    }
}
