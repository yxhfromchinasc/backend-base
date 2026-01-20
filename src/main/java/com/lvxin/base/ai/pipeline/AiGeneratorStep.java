package com.lvxin.base.ai.pipeline;

import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.prompt.PlanConverterPrompt;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 步骤1：AI生成JSON
 */
@Slf4j
@Component
@Order(1)
public class AiGeneratorStep implements ConversionPipelineStep {

    private final AIClient aiClient;
    private final PlanConverterPrompt converterPrompt;
    
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    public AiGeneratorStep(AIClient aiClient, PlanConverterPrompt converterPrompt) {
        this.aiClient = aiClient;
        this.converterPrompt = converterPrompt;
    }

    @Override
    public String process(String currentJson, String originalMarkdown) {
        log.info("[Pipeline] Step 1: Generating JSON from Markdown...");
        
        String systemPrompt = converterPrompt.generateSystemPrompt(originalMarkdown);
        String aiResponse = aiClient.chat(systemPrompt, Collections.emptyList());
        
        String extracted = extractJson(aiResponse);
        if (extracted == null || extracted.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI生成结果不包含有效JSON");
        }
        return extracted;
    }

    @Override
    public int getOrder() {
        return 1;
    }
    
    private String extractJson(String response) {
        if (response == null) return null;
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) return matcher.group(1).trim();
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) return response.substring(start, end + 1).trim();
        return response;
    }
}
