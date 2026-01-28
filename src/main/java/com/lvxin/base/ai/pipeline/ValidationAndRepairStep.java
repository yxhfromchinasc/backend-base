package com.lvxin.base.ai.pipeline;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lvxin.base.ai.agent.JsonValidatorAgent;
import com.lvxin.base.ai.client.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 步骤2：验证与修复
 * 如果验证失败，优先尝试代码自动修复，最后尝试调用AI进行修复
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
            
            **修复规则：**
            1. SectionBlock 必须包含 'blocks' 数组，如果误写为 'items' 请修正。
            2. 确保所有 data 字段都存在。
            3. 严格遵循之前定义的 Schema。
            
            只输出修复后的JSON，不要解释。
            """;

    public ValidationAndRepairStep(JsonValidatorAgent validatorAgent, AIClient aiClient) {
        this.validatorAgent = validatorAgent;
        this.aiClient = aiClient;
    }

    @Override
    public String process(String currentJson, String originalMarkdown) {
        log.info("[Pipeline] Step 2: Validating and Repairing...");

        // 1. 初次验证
        JsonValidatorAgent.ValidationResult result = validatorAgent.validate(currentJson);
        if (result.isValid()) {
            log.info("✅ JSON验证通过，无需修复");
            return currentJson;
        }

        log.warn("⚠️ JSON验证失败: {}，尝试自动规则修复...", result.getErrorMessage());

        // 2. 尝试代码规则修复（Rule-Based Repair）
        String ruleRepairedJson = tryRuleBasedRepair(currentJson);
        if (ruleRepairedJson != null) {
            JsonValidatorAgent.ValidationResult ruleResult = validatorAgent.validate(ruleRepairedJson);
            if (ruleResult.isValid()) {
                log.info("✅ 代码规则修复成功！");
                return ruleRepairedJson;
            } else {
                log.warn("⚠️ 代码规则修复后仍有错误: {}", ruleResult.getErrorMessage());
                // 更新 currentJson 为修复后的版本，以便 AI 基于更好的版本继续修复
                currentJson = ruleRepairedJson;
                // 更新 result 为新的错误信息
                result = ruleResult;
            }
        } else {
            log.info("ℹ️ 代码规则修复未触发或未修改任何内容");
        }

        // 3. 尝试 AI 修复
        log.warn("🤖 尝试AI修复...");
        try {
            String prompt = String.format(REPAIR_PROMPT_TEMPLATE, currentJson, result.getErrorMessage());
            String repaired = aiClient.chat(prompt, Collections.emptyList());
            String extracted = extractJson(repaired);

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

    /**
     * 基于已知问题的规则修复
     */
    private String tryRuleBasedRepair(String jsonString) {
        try {
            JSONObject root = JSON.parseObject(jsonString);
            boolean modified = false;

            if (root.containsKey("days")) {
                JSONArray days = root.getJSONArray("days");
                for (int i = 0; i < days.size(); i++) {
                    JSONObject day = days.getJSONObject(i);
                    if (day.containsKey("blocks")) {
                        JSONArray blocks = day.getJSONArray("blocks");
                        for (int j = 0; j < blocks.size(); j++) {
                            JSONObject block = blocks.getJSONObject(j);
                            modified |= fixSectionBlock(block);
                        }
                    }
                }
            }
            
            return modified ? root.toJSONString() : null;
        } catch (Exception e) {
            log.warn("规则修复过程异常", e);
            return null;
        }
    }

    private boolean fixSectionBlock(JSONObject block) {
        boolean modified = false;
        String type = block.getString("type");
        
        if ("SectionBlock".equals(type)) {
            // 修复 1: items -> blocks
            if (!block.containsKey("blocks") && block.containsKey("items")) {
                block.put("blocks", block.get("items"));
                block.remove("items");
                modified = true;
            }
            
            // 修复 2: 缺少 blocks 但必须有
            if (!block.containsKey("blocks")) {
                block.put("blocks", new JSONArray());
                modified = true;
            }
            
            // 递归检查子元素
            JSONArray subBlocks = block.getJSONArray("blocks");
            if (subBlocks != null) {
                for (int k = 0; k < subBlocks.size(); k++) {
                    // 通常 ItemBlock 不需要修复，但以防万一
                }
            }
        }
        return modified;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    private String extractJson(String response) {
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
