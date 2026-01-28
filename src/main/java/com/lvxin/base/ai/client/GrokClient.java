package com.lvxin.base.ai.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lvxin.base.ai.config.GrokConfig;
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
 * Grok (xAI) 客户端封装
 * 支持工具调用 (Function Calling) 和 结构化输出
 *
 * @author lvxin
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.grok", name = "api-key")
public class GrokClient implements AIClient {

    private final GrokConfig config;
    private final HttpClient httpClient;
    private final ToolManager toolManager;

    public GrokClient(GrokConfig config, ToolManager toolManager) {
        this.config = config;
        this.toolManager = toolManager;
        
        // 创建HTTP客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeout() != null ? config.getTimeout() : 360000))
                .build();
        
        log.info("Grok客户端初始化成功，模型: {}, BaseURL: {}", config.getDefaultModel(), config.getBaseUrl());
    }

    @Override
    public String chat(String systemPrompt, List<ChatMessage> messages) {
        return chat(null, systemPrompt, messages);
    }

    @Override
    public String chat(String model, String systemPrompt, List<ChatMessage> messages) {
        String targetModel = (model != null && !model.isEmpty()) ? model : config.getDefaultModel();
        
        // 映射模型别名
        if ("fast-model".equals(targetModel) || "smart-model".equals(targetModel)) {
            targetModel = config.getDefaultModel();
        }

        return doChat(targetModel, systemPrompt, messages);
    }

    @Override
    public String chatWithStructure(String systemPrompt, List<ChatMessage> messages, String schemaName, Object schemaDefinition) {
        // Grok 支持结构化输出，使用 grok-beta 或更高版本
        return doChat(config.getDefaultModel(), systemPrompt, messages, schemaName, schemaDefinition);
    }

    private String doChat(String modelName, String systemPrompt, List<ChatMessage> messages) {
        return doChat(modelName, systemPrompt, messages, null, null);
    }

    private String doChat(String modelName, String systemPrompt, List<ChatMessage> messages, String schemaName, Object schemaDefinition) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", modelName);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);
            // 设置最大Token数，防止长文本截断 (增加到16000以支持完整5天行程)
            requestBody.put("max_tokens", 16000);
            
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
                
                // 处理可能包含 tool_calls 的消息内容
                // 这里简化处理：如果是普通的文本消息，直接使用content
                // 如果是复杂的tool消息，需要正确构建（这里主要处理简单的文本历史）
                // 更严谨的实现应该检查 ChatMessage 是否包含 toolCalls 字段
                // 目前 ChatMessage 结构较简单，假设主要是文本
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
                        msgObj.put("content", content);
                    }
                } else {
                    msgObj.put("content", content);
                }
                
                messagesArray.add(msgObj);
            }
            
            requestBody.put("messages", messagesArray);

            // 添加结构化输出参数
            if (schemaName != null && schemaDefinition != null) {
                JSONObject responseFormat = new JSONObject();
                responseFormat.put("type", "json_schema");
                
                JSONObject jsonSchema = new JSONObject();
                jsonSchema.put("name", schemaName);
                jsonSchema.put("strict", true);
                jsonSchema.put("schema", schemaDefinition);
                
                responseFormat.put("json_schema", jsonSchema);
                requestBody.put("response_format", responseFormat);
            }
            
            // 添加工具定义 (如果启用)
            if (config.getEnableTools() != null && config.getEnableTools()) {
                JSONArray toolsArray = buildToolsDefinition();
                if (!toolsArray.isEmpty()) {
                    requestBody.put("tools", toolsArray);
                    requestBody.put("tool_choice", "auto");
                }
            }
            
            // 发送请求
            String requestBodyStr = JSON.toJSONString(requestBody);
            log.debug("Grok请求: {}", requestBodyStr);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .timeout(Duration.ofMillis(config.getTimeout() != null ? config.getTimeout() : 360000))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("Grok API调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, 
                        "Grok服务调用失败，状态码: " + response.statusCode());
            }
            
            // 解析响应
            JSONObject responseJson = JSON.parseObject(response.body());
            JSONArray choices = responseJson.getJSONArray("choices");
            
            if (choices == null || choices.isEmpty()) {
                log.warn("Grok返回结果为空");
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI返回结果为空");
            }
            
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            
            // 检查是否有工具调用
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                // 处理工具调用，并递归调用
                return handleToolCalls(message, systemPrompt, messages, modelName, schemaName, schemaDefinition);
            }
            
            String content = message.getString("content");
            log.debug("Grok回复成功，长度: {}", content != null ? content.length() : 0);
            return content != null ? content : "";
            
        } catch (IOException | InterruptedException e) {
            log.error("Grok调用异常", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Grok服务异常：" + e.getMessage());
        }
    }

    /**
     * 构建工具定义
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
            
            // 定义参数schema (简化版)
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
    private String handleToolCalls(JSONObject message, String systemPrompt, List<ChatMessage> messages, 
                                   String modelName, String schemaName, Object schemaDefinition) {
        try {
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                return message.getString("content");
            }
            
            List<JSONObject> toolResults = new ArrayList<>();
            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject toolCall = toolCalls.getJSONObject(i);
                JSONObject function = toolCall.getJSONObject("function");
                String toolName = function.getString("name");
                String arguments = function.getString("arguments");
                
                log.info("Grok 请求调用工具: {}", toolName);
                
                // 执行工具
                String toolResult = toolManager.executeTool(toolName, arguments);
                
                // 构建结果
                JSONObject toolResultObj = new JSONObject();
                toolResultObj.put("role", "tool");
                toolResultObj.put("name", toolName);
                toolResultObj.put("content", toolResult);
                toolResultObj.put("tool_call_id", toolCall.getString("id"));
                toolResults.add(toolResultObj);
            }
            
            // 扩展消息历史
            List<ChatMessage> extendedMessages = new ArrayList<>(messages);
            
            // 添加 Assistant 的 tool_calls 消息
            JSONObject assistantMsg = new JSONObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", message.getString("content"));
            assistantMsg.put("tool_calls", message.getJSONArray("tool_calls"));
            
            // 将 JSON 对象转为 String 存入 ChatMessage (为了兼容现有结构)
            extendedMessages.add(ChatMessage.builder()
                    .role("assistant")
                    .content(assistantMsg.toJSONString())
                    .build());
            
            // 添加 Tool 结果消息
            for (JSONObject toolResult : toolResults) {
                extendedMessages.add(ChatMessage.builder()
                        .role("tool")
                        .content(toolResult.toJSONString())
                        .build());
            }
            
            // 递归调用，带上 schema 信息，确保最终输出符合结构
            return doChat(modelName, systemPrompt, extendedMessages, schemaName, schemaDefinition);
            
        } catch (Exception e) {
            log.error("处理工具调用失败", e);
            return message.getString("content") + "\n\n[工具调用失败: " + e.getMessage() + "]";
        }
    }
}
