package com.lvxin.base.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.lvxin.base.ai.client.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON修复智能体
 * 负责修复不完整或格式错误的JSON
 *
 * @author lvxin
 */
@Slf4j
@Component
public class JsonRepairAgent {

    private final AIClient aiClient;

    private static final String REPAIR_PROMPT_TEMPLATE = """
            你是一个JSON修复专家。请修复以下不完整的JSON，使其成为完整、有效的JSON格式。
            
            **原始JSON（可能不完整）：**
            %s
            
            **修复要求：**
            1. 如果JSON被截断，请补全缺失的部分
            2. 修复所有语法错误（缺少引号、逗号、括号等）
            3. 确保所有字符串都有正确的引号
            4. 确保所有数组和对象都正确闭合
            5. 保持原有的数据结构不变
            6. 如果缺少字段，根据上下文推断合理的默认值
            
            **输出要求：**
            - 只输出修复后的完整JSON，不要其他内容
            - JSON必须是有效的、可解析的
            - 保持原有的数据结构和内容
            
            请开始修复：
            """;

    public JsonRepairAgent(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * 修复JSON字符串
     *
     * @param brokenJson 不完整的JSON字符串
     * @param originalPlan 原始计划内容（用于上下文推断）
     * @return 修复后的JSON字符串
     */
    public String repairJson(String brokenJson, String originalPlan) {
        if (brokenJson == null || brokenJson.isEmpty()) {
            throw new IllegalArgumentException("JSON内容不能为空");
        }

        try {
            log.debug("开始修复JSON，原始长度: {}", brokenJson.length());

            // 先尝试简单的自动修复
            String autoFixed = autoRepair(brokenJson);
            if (isValidJson(autoFixed)) {
                log.debug("自动修复成功");
                return autoFixed;
            }

            // 如果自动修复失败，使用AI修复
            log.debug("自动修复失败，使用AI修复");
            String prompt = String.format(REPAIR_PROMPT_TEMPLATE, brokenJson);
            String repairedJson = aiClient.chat(prompt, Collections.emptyList());

            // 提取JSON
            String extractedJson = extractJsonFromResponse(repairedJson);

            // 验证修复后的JSON
            if (isValidJson(extractedJson)) {
                log.debug("AI修复成功，修复后长度: {}", extractedJson.length());
                return extractedJson;
            } else {
                log.warn("AI修复后仍然无效");
                throw new RuntimeException("无法修复JSON");
            }

        } catch (Exception e) {
            log.error("JSON修复失败", e);
            throw new RuntimeException("JSON修复失败：" + e.getMessage(), e);
        }
    }

    /**
     * 自动修复常见的JSON错误
     */
    private String autoRepair(String json) {
        String repaired = json.trim();

        // 1. 如果以{开头但没有闭合，尝试补全
        if (repaired.startsWith("{") && !repaired.endsWith("}")) {
            int openBraces = countChar(repaired, '{');
            int closeBraces = countChar(repaired, '}');
            int missing = openBraces - closeBraces;
            if (missing > 0) {
                // 检查最后一个字符
                char lastChar = repaired.charAt(repaired.length() - 1);
                if (lastChar == ',' || lastChar == '"' || lastChar == ']' || lastChar == '}') {
                    // 可能需要补全
                } else {
                    // 补全缺失的闭合括号
                    repaired += "\n" + "}".repeat(missing);
                }
            }
        }

        // 2. 修复常见的引号问题
        repaired = repaired.replaceAll("([{,:])\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:", "$1\"$2\":");

        // 3. 修复缺失的逗号（简单情况）
        repaired = repaired.replaceAll("}\\s*{", "},{");
        repaired = repaired.replaceAll("]\\s*\\[", "],[");

        return repaired;
    }

    /**
     * 统计字符出现次数
     */
    private int countChar(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) count++;
        }
        return count;
    }

    /**
     * 验证JSON是否有效
     */
    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            JSON.parse(json);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 从AI响应中提取JSON
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }

        // 尝试匹配JSON代码块
        Pattern jsonBlockPattern = Pattern.compile(
                "```(?:json)?\\s*([\\s\\S]*?)```",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher matcher = jsonBlockPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果没有代码块，尝试直接提取JSON（查找第一个{到最后一个}）
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1).trim();
        }

        return response.trim();
    }
}
