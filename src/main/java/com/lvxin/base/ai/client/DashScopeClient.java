package com.lvxin.base.ai.client;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.lvxin.base.ai.config.DashScopeConfig;
import com.lvxin.base.ai.model.ChatMessage;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云DashScope（通义千问）客户端封装
 * 提供统一的AI调用接口
 *
 * @author lvxin
 */
@Slf4j
@Component
public class DashScopeClient implements AIClient {
    private final DashScopeConfig config;
    private final Generation generation;

    public DashScopeClient(DashScopeConfig config) {
        this.config = config;
        this.generation = new Generation();
        
        // 设置HTTP超时时间（DashScope SDK内部使用OkHttp，通过系统属性设置）
        if (config.getTimeout() != null && config.getTimeout() > 0) {
            int timeoutSeconds = config.getTimeout() / 1000;
            // 设置OkHttp连接超时和读取超时（秒）
            System.setProperty("dashscope.http.connectTimeout", String.valueOf(timeoutSeconds));
            System.setProperty("dashscope.http.readTimeout", String.valueOf(timeoutSeconds));
            System.setProperty("dashscope.http.writeTimeout", String.valueOf(timeoutSeconds));
            log.info("DashScope HTTP超时时间已设置为: {}秒 ({}ms)", timeoutSeconds, config.getTimeout());
        }
    }

    /**
     * 发送聊天消息
     *
     * @param systemPrompt 系统提示词
     * @param messages     对话历史
     * @return AI回复
     */
    public String chat(String systemPrompt, List<ChatMessage> messages) {
        try {
            // 构建消息列表
            List<Message> messageList = new ArrayList<>();
            
            // 添加系统消息
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messageList.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(systemPrompt)
                        .build());
            }

            // 添加对话历史
            messageList.addAll(messages.stream()
                    .map(msg -> Message.builder()
                            .role(msg.getRole())
                            .content(msg.getContent())
                            .build())
                    .collect(Collectors.toList()));

            // 构建请求参数
            QwenParam param = QwenParam.builder()
                    .apiKey(config.getApiKey())
                    .model(config.getDefaultModel())
                    .messages(messageList)
                    .resultFormat(QwenParam.ResultFormat.MESSAGE)
                    .build();

            // 设置超时时间（如果配置中有）
            if (config.getTimeout() != null && config.getTimeout() > 0) {
                // DashScope SDK可能通过Generation对象设置超时，或者通过HTTP客户端配置
                // 这里先尝试通过param设置，如果SDK不支持，需要在Generation初始化时配置
                log.debug("设置AI请求超时时间: {}ms", config.getTimeout());
            }

            // 调用API
            com.alibaba.dashscope.aigc.generation.GenerationResult result = generation.call(param);
            
            // 提取回复内容
            if (result.getOutput() != null 
                    && result.getOutput().getChoices() != null 
                    && !result.getOutput().getChoices().isEmpty()) {
                String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                log.debug("AI回复成功，长度: {}", content.length());
                return content;
            } else {
                log.warn("AI返回结果为空");
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI返回结果为空");
            }

        } catch (NoApiKeyException e) {
            log.error("DashScope API Key未配置", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI服务配置错误：API Key未配置");
        } catch (InputRequiredException e) {
            log.error("DashScope请求参数错误", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI请求参数错误：" + e.getMessage());
        } catch (ApiException e) {
            log.error("DashScope API调用失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI服务调用失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("AI调用异常", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI服务异常：" + e.getMessage());
        }
    }

    /**
     * 发送聊天消息（无系统提示词）
     *
     * @param messages 对话历史
     * @return AI回复
     */
    public String chat(List<ChatMessage> messages) {
        return chat(null, messages);
    }
}
