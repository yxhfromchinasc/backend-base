package com.lvxin.base.ai.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lvxin.base.ai.config.DeepSeekConfig;
import com.lvxin.base.ai.model.ChatMessage;
import com.lvxin.base.ai.tool.ToolManager;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek客户端封装
 * 支持DeepSeek-R1模型，支持Function Calling和工具调用
 *
 * @author lvxin
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.deepseek", name = "api-key")
public class DeepSeekClient implements AIClient {
    
    private final DeepSeekConfig config;
    private final ToolManager toolManager;
    private final HttpClient httpClient;
    
    public DeepSeekClient(DeepSeekConfig config, ToolManager toolManager) {
        this.config = config;
        this.toolManager = toolManager;
        
        // 创建HTTP客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeout() != null ? config.getTimeout() : 360000))
                .build();
        
        log.info("DeepSeek客户端初始化成功，模型: {}, BaseURL: {}", config.getDefaultModel(), config.getBaseUrl());
    }
    
    @Override
    public String chat(String systemPrompt, List<ChatMessage> messages) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", config.getDefaultModel());
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4000);
            
            // 构建消息列表
            JSONArray messagesArray = new JSONArray();
            
            // 添加系统消息
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
                messagesArray.add(systemMsg);
            }
            
            // 添加对话历史
            for (ChatMessage msg : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.getRole());
                
                // 尝试解析content，如果是JSON字符串，提取相应字段
                String content = msg.getContent();
                if (content != null && content.trim().startsWith("{")) {
                    try {
                        JSONObject contentObj = JSON.parseObject(content);
                        // 如果是assistant消息且包含tool_calls
                        if ("assistant".equals(msg.getRole()) && contentObj.containsKey("tool_calls")) {
                            msgObj.put("content", contentObj.getString("content")); // 可能为null
                            msgObj.put("tool_calls", contentObj.getJSONArray("tool_calls"));
                        }
                        // 如果是tool消息且包含tool_call_id
                        else if ("tool".equals(msg.getRole()) && contentObj.containsKey("tool_call_id")) {
                            msgObj.put("content", contentObj.getString("content"));
                            msgObj.put("tool_call_id", contentObj.getString("tool_call_id"));
                        }
                        // 否则直接使用content字符串
                        else {
                            msgObj.put("content", content);
                        }
                    } catch (Exception e) {
                        // 解析失败，直接使用原始content
                        msgObj.put("content", content);
                    }
                } else {
                    msgObj.put("content", content);
                }
                
                messagesArray.add(msgObj);
            }
            
            requestBody.put("messages", messagesArray);
            
            // 如果启用工具调用，添加工具定义
            if (config.getEnableTools() != null && config.getEnableTools()) {
                JSONArray toolsArray = buildToolsDefinition();
                if (!toolsArray.isEmpty()) {
                    requestBody.put("tools", toolsArray);
                    requestBody.put("tool_choice", "auto"); // 让模型自动决定是否调用工具
                }
            }
            
            // 发送请求
            String requestBodyStr = JSON.toJSONString(requestBody);
            log.debug("DeepSeek请求: {}", requestBodyStr);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .timeout(Duration.ofMillis(config.getTimeout() != null ? config.getTimeout() : 360000))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("DeepSeek API调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, 
                        "DeepSeek服务调用失败，状态码: " + response.statusCode());
            }
            
            // 解析响应
            JSONObject responseJson = JSON.parseObject(response.body());
            JSONArray choices = responseJson.getJSONArray("choices");
            
            if (choices == null || choices.isEmpty()) {
                log.warn("DeepSeek返回结果为空");
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI返回结果为空");
            }
            
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            
            // 检查是否有工具调用
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                // 处理工具调用
                return handleToolCalls(message, systemPrompt, messages);
            }
            
            // 直接返回内容
            String content = message.getString("content");
            log.debug("DeepSeek回复成功，长度: {}", content != null ? content.length() : 0);
            return content != null ? content : "";
            
        } catch (IOException | InterruptedException e) {
            log.error("DeepSeek调用异常", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek服务异常：" + e.getMessage());
        }
    }
    
    /**
     * 构建工具定义（用于Function Calling）
     */
    private JSONArray buildToolsDefinition() {
        JSONArray toolsArray = new JSONArray();
        Map<String, String> toolDescriptions = toolManager.getAllToolDescriptions();
        
        for (Map.Entry<String, String> entry : toolDescriptions.entrySet()) {
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            
            JSONObject function = new JSONObject();
            function.put("name", entry.getKey());
            function.put("description", entry.getValue());
            
            // 定义参数schema（简化版，实际应该更详细）
            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");
            parameters.put("properties", new JSONObject());
            parameters.put("required", new JSONArray());
            
            function.put("parameters", parameters);
            tool.put("function", function);
            
            toolsArray.add(tool);
        }
        
        return toolsArray;
    }
    
    /**
     * 处理工具调用
     */
    private String handleToolCalls(JSONObject message, String systemPrompt, List<ChatMessage> messages) {
        try {
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                return message.getString("content");
            }
            
            // 执行所有工具调用
            List<JSONObject> toolResults = new ArrayList<>();
            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject toolCall = toolCalls.getJSONObject(i);
                JSONObject function = toolCall.getJSONObject("function");
                String toolName = function.getString("name");
                String arguments = function.getString("arguments");
                
                // 执行工具
                String toolResult = toolManager.executeTool(toolName, arguments);
                
                // 构建工具调用结果
                JSONObject toolResultObj = new JSONObject();
                toolResultObj.put("role", "tool");
                toolResultObj.put("name", toolName);
                toolResultObj.put("content", toolResult);
                toolResultObj.put("tool_call_id", toolCall.getString("id"));
                toolResults.add(toolResultObj);
            }
            
            // 将工具调用结果添加到消息历史，再次调用AI生成最终回复
            List<ChatMessage> extendedMessages = new ArrayList<>(messages);
            
            // 添加AI的工具调用消息（需要包含tool_calls）
            JSONObject assistantMsg = new JSONObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", message.getString("content")); // 可能为null
            assistantMsg.put("tool_calls", message.getJSONArray("tool_calls"));
            extendedMessages.add(ChatMessage.builder()
                    .role("assistant")
                    .content(assistantMsg.toJSONString())
                    .build());
            
            // 添加工具结果消息（格式：role=tool, content=工具结果, tool_call_id=工具调用ID）
            for (JSONObject toolResult : toolResults) {
                // 工具结果消息的content应该是工具执行结果的字符串，而不是整个对象
                String toolContent = toolResult.getString("content");
                String toolCallId = toolResult.getString("tool_call_id");
                
                // 构建符合DeepSeek API格式的工具结果消息
                JSONObject toolMsgObj = new JSONObject();
                toolMsgObj.put("role", "tool");
                toolMsgObj.put("content", toolContent);
                toolMsgObj.put("tool_call_id", toolCallId);
                
                extendedMessages.add(ChatMessage.builder()
                        .role("tool")
                        .content(toolMsgObj.toJSONString())
                        .build());
            }
            
            // 再次调用AI，生成最终回复
            return chat(systemPrompt, extendedMessages);
            
        } catch (Exception e) {
            log.error("处理工具调用失败", e);
            return message.getString("content") + "\n\n[工具调用失败: " + e.getMessage() + "]";
        }
    }
}
