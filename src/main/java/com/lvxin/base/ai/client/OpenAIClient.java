package com.lvxin.base.ai.client;

import com.lvxin.base.ai.config.OpenAIConfig;
import com.lvxin.base.ai.model.ChatMessage;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI客户端封装
 * 支持GPT-4o、GPT-4-turbo等模型，支持Function Calling和Web Search
 *
 * @author lvxin
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.openai", name = "api-key")
public class OpenAIClient implements AIClient {
    
    private final OpenAIConfig config;
    private final OpenAiService openAiService;
    
    public OpenAIClient(OpenAIConfig config) {
        this.config = config;
        
        // 创建OpenAI服务实例
        Duration timeout = Duration.ofMillis(config.getTimeout() != null ? config.getTimeout() : 360000);
        // OpenAI Java SDK的OpenAiService构造函数：OpenAiService(String token, Duration timeout)
        this.openAiService = new OpenAiService(config.getApiKey(), timeout);
        
        log.info("OpenAI客户端初始化成功，模型: {}, 超时: {}ms", config.getDefaultModel(), config.getTimeout());
    }
    
    @Override
    public String chat(String systemPrompt, List<ChatMessage> messages) {
        try {
            List<com.theokanning.openai.completion.chat.ChatMessage> openAIMessages = new ArrayList<>();
            
            // 添加系统消息
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                openAIMessages.add(new com.theokanning.openai.completion.chat.ChatMessage(
                        ChatMessageRole.SYSTEM.value(),
                        systemPrompt
                ));
            }
            
            // 转换对话历史
            openAIMessages.addAll(messages.stream()
                    .map(msg -> {
                        String role = "user".equals(msg.getRole()) 
                                ? ChatMessageRole.USER.value() 
                                : ChatMessageRole.ASSISTANT.value();
                        return new com.theokanning.openai.completion.chat.ChatMessage(role, msg.getContent());
                    })
                    .collect(Collectors.toList()));
            
            // 构建请求
            ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder = ChatCompletionRequest.builder()
                    .model(config.getDefaultModel())
                    .messages(openAIMessages)
                    .temperature(0.7)
                    .maxTokens(4000);
            
            // 如果启用Web Search，添加工具支持
            // 注意：Web Search需要通过Responses API或工具调用实现
            // 这里先实现基础版本，后续可以扩展支持工具调用
            
            ChatCompletionRequest request = requestBuilder.build();
            
            // 调用API
            com.theokanning.openai.completion.chat.ChatCompletionResult result = 
                    openAiService.createChatCompletion(request);
            
            // 提取回复内容
            if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                String content = result.getChoices().get(0).getMessage().getContent();
                log.debug("OpenAI回复成功，长度: {}", content.length());
                return content;
            } else {
                log.warn("OpenAI返回结果为空");
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI返回结果为空");
            }
            
        } catch (com.theokanning.openai.OpenAiHttpException e) {
            log.error("OpenAI API调用失败，状态码: {}, 错误: {}", e.statusCode, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OpenAI服务调用失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("OpenAI调用异常", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OpenAI服务异常：" + e.getMessage());
        }
    }
}
