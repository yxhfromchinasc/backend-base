package com.lvxin.base.ai.pipeline;

import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.prompt.PlanConverterPrompt;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import com.lvxin.base.util.JsonUtil;
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
        
        String extracted = JsonUtil.extractJsonFromMarkdown(aiResponse);
        if (extracted == null || extracted.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI生成结果不包含有效JSON");
        }
        return extracted;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
