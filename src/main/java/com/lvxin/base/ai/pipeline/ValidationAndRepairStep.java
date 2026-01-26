package com.lvxin.base.ai.pipeline;

import com.lvxin.base.ai.agent.JsonValidatorAgent;
import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.monitor.PerformanceMonitorContext;
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

        // JSON验证
        PerformanceMonitorContext.recordStage("JSON验证开始");
        long validateStartTime = System.currentTimeMillis();
        JsonValidatorAgent.ValidationResult result = validatorAgent.validate(currentJson);
        long validateDuration = System.currentTimeMillis() - validateStartTime;
        PerformanceMonitorContext.recordDuration("JSON验证", validateDuration);
        PerformanceMonitorContext.recordStage("JSON验证完成");
        
        if (result.isValid()) {
            log.info("JSON验证通过，无需修复");
            return currentJson;
        }

        log.warn("JSON验证失败: {}，尝试AI修复...", result.getErrorMessage());

        try {
            // 构建修复提示词
            long promptStartTime = System.currentTimeMillis();
            String prompt = String.format(REPAIR_PROMPT_TEMPLATE, currentJson, result.getErrorMessage());
            long promptDuration = System.currentTimeMillis() - promptStartTime;
            PerformanceMonitorContext.recordDuration("构建修复提示词", promptDuration);
            
            // AI修复调用
            PerformanceMonitorContext.recordStage("AI修复调用开始");
            long repairStartTime = System.currentTimeMillis();
            // 修复使用 fast-model，因为修复任务相对简单，不需要推理模型
            String repaired = aiClient.chat("fast-model", prompt, Collections.emptyList());
            long repairDuration = System.currentTimeMillis() - repairStartTime;
            PerformanceMonitorContext.recordDuration("AI修复调用", repairDuration);
            PerformanceMonitorContext.recordStage("AI修复调用完成");
            
            // 提取修复后的JSON
            long extractStartTime = System.currentTimeMillis();
            String extracted = extractJson(repaired);
            long extractDuration = System.currentTimeMillis() - extractStartTime;
            PerformanceMonitorContext.recordDuration("提取修复后的JSON", extractDuration);

            // 二次验证
            long revalidateStartTime = System.currentTimeMillis();
            boolean isValid = validatorAgent.validate(extracted).isValid();
            long revalidateDuration = System.currentTimeMillis() - revalidateStartTime;
            PerformanceMonitorContext.recordDuration("二次验证", revalidateDuration);
            
            if (isValid) {
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

    private String extractJson(String response) {
        // ... (复用提取逻辑，实际项目中应放入Util类)
        if (response == null) return null;
        Pattern p = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(response);
        if (m.find()) return m.group(1).trim();
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) return response.substring(start, end + 1).trim();
        return response;
    }
}
