package com.lvxin.base.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.prompt.PlanConverterPrompt;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能体2：计划转换器
 * 负责将Markdown格式的计划转换为JSON格式的计划表
 *
 * @author lvxin
 */
@Slf4j
@Component
public class PlanConverterAgent {

    private final AIClient aiClient;
    private final PlanConverterPrompt converterPrompt;

    public PlanConverterAgent(AIClient aiClient, PlanConverterPrompt converterPrompt) {
        this.aiClient = aiClient;
        this.converterPrompt = converterPrompt;
    }

    // JSON代码块匹配模式
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );


    /**
     * 转换Markdown计划为JSON字符串
     *
     * @param markdownPlan Markdown格式的计划
     * @return JSON格式的计划表字符串
     */
    public String convertToJson(String markdownPlan) {
        if (markdownPlan == null || markdownPlan.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划内容不能为空");
        }

        try {
            log.debug("开始转换计划，计划长度: {}", markdownPlan.length());

            // 生成Prompt
            String systemPrompt = converterPrompt.generateSystemPrompt(markdownPlan);

            // 调用AI转换
            String aiResponse = aiClient.chat(systemPrompt, Collections.emptyList());

            // 提取JSON（可能包含在代码块中）
            String jsonString = extractJsonFromResponse(aiResponse);

            // 验证JSON格式
            validateJson(jsonString);

            log.debug("计划转换成功，JSON长度: {}", jsonString.length());
            return jsonString;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("计划转换失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "计划转换失败：" + e.getMessage());
        }
    }

    /**
     * 从AI响应中提取JSON
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI返回结果为空");
        }

        // 尝试匹配JSON代码块
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果没有代码块，尝试直接提取JSON（查找第一个{到最后一个}）
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1).trim();
        }

        // 如果都找不到，返回原始响应（可能是纯JSON）
        return response.trim();
    }

    /**
     * 验证JSON格式
     */
    private void validateJson(String jsonString) {
        try {
            // 尝试解析JSON
            Object parsed = JSON.parse(jsonString);
            if (parsed == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JSON解析结果为空");
            }
        } catch (JSONException e) {
            log.error("JSON格式验证失败: {}", jsonString.substring(0, Math.min(200, jsonString.length())));
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成的JSON格式不正确：" + e.getMessage());
        }
    }

    /**
     * 补全和验证计划表JSON
     * 确保包含必需的字段和格式
     *
     * @param jsonString 原始JSON字符串
     * @return 补全后的JSON字符串
     */
    public String completeAndValidateItineraryJson(String jsonString) {
        try {
            // 解析JSON
            com.alibaba.fastjson2.JSONObject jsonObject = com.alibaba.fastjson2.JSON.parseObject(jsonString);

            // 确保有title字段
            if (!jsonObject.containsKey("title") || jsonObject.getString("title") == null) {
                jsonObject.put("title", "AI生成的旅行计划");
            }

            // 确保有days字段
            if (!jsonObject.containsKey("days")) {
                jsonObject.put("days", Collections.emptyList());
            }

            // 确保有id、version、componentVersion字段（这些会在保存时由系统设置）
            // 这里不做处理，由Service层统一处理

            return JSON.toJSONString(jsonObject);

        } catch (Exception e) {
            log.error("补全计划表JSON失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "补全计划表JSON失败：" + e.getMessage());
        }
    }
}
