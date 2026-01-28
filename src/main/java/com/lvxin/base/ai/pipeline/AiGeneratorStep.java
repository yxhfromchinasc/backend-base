package com.lvxin.base.ai.pipeline;

import com.alibaba.fastjson2.JSONObject;
import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.prompt.PlanConverterPrompt;
import com.lvxin.base.ai.util.ItinerarySchemaFactory;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 步骤1：AI生成JSON
 * 使用 Grok 的结构化输出功能，确保生成严格符合 Schema 的 JSON
 */
@Slf4j
@Component
@Order(1)
public class AiGeneratorStep implements ConversionPipelineStep {

    private final AIClient aiClient;
    private final PlanConverterPrompt converterPrompt;
    
    public AiGeneratorStep(AIClient aiClient, PlanConverterPrompt converterPrompt) {
        this.aiClient = aiClient;
        this.converterPrompt = converterPrompt;
    }

    @Override
    public String process(String currentJson, String originalMarkdown) {
        log.info("[Pipeline] Step 1: Generating JSON from Markdown using Structured Output...");
        
        String systemPrompt = converterPrompt.generateSystemPrompt(originalMarkdown);
        JSONObject schema = ItinerarySchemaFactory.getItinerarySchema();
        
        // 调用 AI 接口，传入 Schema 要求结构化输出
        String aiResponse = aiClient.chatWithStructure(
                systemPrompt, 
                Collections.emptyList(), 
                "itinerary_plan", 
                schema
        );
        
        log.info("[Pipeline] Step 1: AI Response received. Raw Length: {}", aiResponse != null ? aiResponse.length() : 0);
        
        // 虽然结构化输出通常返回纯 JSON，但为了兼容性（如 DeepSeek 回退或偶尔的 Markdown 包裹），
        // 仍然尝试提取 JSON
        String extracted = extractJson(aiResponse);
        
        if (extracted == null || extracted.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI生成结果为空或无效");
        }
        
        return extracted;
    }

    @Override
    public int getOrder() {
        return 1;
    }

    private String extractJson(String response) {
        if (response == null) return null;
        // 尝试匹配 markdown code block
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "```(?:json)?\\s*([\\s\\S]*?)```",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.MULTILINE
        ).matcher(response);
        
        if (matcher.find()) return matcher.group(1).trim();
        
        // 尝试寻找首尾的大括号
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) return response.substring(start, end + 1).trim();
        
        // 假设就是纯 JSON
        return response.trim();
    }
}
