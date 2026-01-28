package com.lvxin.base.ai.config;

import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.client.DeepSeekClient;
import com.lvxin.base.ai.client.GrokClient;
import com.lvxin.base.ai.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI客户端配置
 * 根据配置自动选择使用DeepSeek或Grok
 *
 * @author lvxin
 */
@Slf4j
@Configuration
public class AIClientConfig {
    
    /**
     * 如果配置了DeepSeek，使用DeepSeek（支持工具调用）
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
     * 如果配置了Grok，使用Grok
     */
    @Bean("aiClient")
    @Primary
    @ConditionalOnProperty(prefix = "ai.grok", name = "api-key")
    public AIClient grokClient(GrokConfig config, ToolManager toolManager) {
        log.info("使用Grok作为AI服务提供商，模型: {}", config.getDefaultModel());
        return new GrokClient(config, toolManager);
    }
}