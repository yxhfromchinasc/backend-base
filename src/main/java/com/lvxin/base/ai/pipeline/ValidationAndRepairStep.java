package com.lvxin.base.ai.pipeline;

import com.lvxin.base.ai.agent.JsonValidatorAgent;
import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 步骤2：验证与修复
 * 如果验证失败，尝试调用AI进行修复
 */
@Slf4j
@Component
@Order(2)
public class ValidationAndRepairStep implements ConversionPipelineStep {

    private final JsonValidatorAgent validatorAgent;
    private final AIClient aiClient;

    private static final String REPAIR_PROMPT_TEMPLATE = """
            你是一个JSON修复专家。以下JSON存在格式错误，请修复它。
            
            **错误JSON：**
            %s
            
            **错误信息：**
            %s
            
            只输出修复后的JSON，不要解释。
            """;

    public ValidationAndRepairStep(JsonValidatorAgent validatorAgent, AIClient aiClient) {
        this.validatorAgent = validatorAgent;
        this.aiClient = aiClient;
    }

    @Override
    public String process(String currentJson, String originalMarkdown) {
        log.info("[Pipeline] Step 2: Validating and Repairing...");

        JsonValidatorAgent.ValidationResult result = validatorAgent.validate(currentJson);
        if (result.isValid()) {
            log.info("JSON验证通过，无需修复");
            return currentJson;
        }

        log.warn("JSON验证失败: {}，尝试AI修复...", result.getErrorMessage());

        try {
            String prompt = String.format(REPAIR_PROMPT_TEMPLATE, currentJson, result.getErrorMessage());
            String repaired = aiClient.chat(prompt, Collections.emptyList());
            String extracted = JsonUtil.extractJsonFromMarkdown(repaired);

            // 二次验证
            if (validatorAgent.validate(extracted).isValid()) {
                log.info("AI修复成功");
                return extracted;
            } else {
                throw new RuntimeException("修复后仍然无效");
            }
        } catch (Exception e) {
            log.error("AI修复失败", e);
            throw new RuntimeException("JSON验证失败且修复失败: " + result.getErrorMessage());
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
