package com.lvxin.base.ai.client;

import com.lvxin.base.ai.model.ChatMessage;

import java.util.List;

/**
 * AI客户端统一接口
 * 支持多种AI服务提供商（OpenAI、DashScope等）
 *
 * @author lvxin
 */
public interface AIClient {
    
    /**
     * 发送聊天消息
     *
     * @param systemPrompt 系统提示词
     * @param messages     对话历史
     * @return AI回复
     */
    String chat(String systemPrompt, List<ChatMessage> messages);
    
    /**
     * 发送聊天消息（指定模型）
     *
     * @param model        模型名称（支持别名，如 fast-model, smart-model）
     * @param systemPrompt 系统提示词
     * @param messages     对话历史
     * @return AI回复
     */
    default String chat(String model, String systemPrompt, List<ChatMessage> messages) {
        // 默认实现调用原方法，忽略模型参数
        return chat(systemPrompt, messages);
    }
    
    /**
     * 发送聊天消息（无系统提示词）
     *
     * @param messages 对话历史
     * @return AI回复
     */
    default String chat(List<ChatMessage> messages) {
        return chat(null, messages);
    }

    /**
     * 发送聊天消息并要求结构化输出
     *
     * @param systemPrompt 系统提示词
     * @param messages     对话历史
     * @param schemaName   Schema名称
     * @param schemaDefinition JSON Schema定义（JSONObject）
     * @return AI回复（JSON字符串）
     */
    default String chatWithStructure(String systemPrompt, List<ChatMessage> messages, String schemaName, Object schemaDefinition) {
        // 默认实现降级为普通对话
        return chat(systemPrompt, messages);
    }
}
