package com.lvxin.base.ai.agent;

import com.alibaba.fastjson2.JSON;
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
    private final JsonValidatorAgent validatorAgent;
    private final JsonRepairAgent repairAgent;

    // 最大重试次数
    private static final int MAX_RETRIES = 3;

    public PlanConverterAgent(AIClient aiClient, PlanConverterPrompt converterPrompt,
                              JsonValidatorAgent validatorAgent, JsonRepairAgent repairAgent) {
        this.aiClient = aiClient;
        this.converterPrompt = converterPrompt;
        this.validatorAgent = validatorAgent;
        this.repairAgent = repairAgent;
    }

    // JSON代码块匹配模式
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );


    /**
     * 转换Markdown计划为JSON字符串（带重试和多智能体协作）
     *
     * @param markdownPlan Markdown格式的计划
     * @return JSON格式的计划表字符串
     */
    public String convertToJson(String markdownPlan) {
        if (markdownPlan == null || markdownPlan.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划内容不能为空");
        }

        Exception lastException = null;
        
        // 重试机制
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            long attemptStartTime = System.currentTimeMillis();
            try {
                log.info("  → [转换尝试 {}/{}] 开始转换计划，计划长度: {} 字符", 
                        attempt, MAX_RETRIES, markdownPlan.length());

                // 步骤1：生成JSON（生成器智能体）
                log.info("  → [转换尝试 {}/{}] [步骤1] 调用AI生成JSON...", attempt, MAX_RETRIES);
                long generateStart = System.currentTimeMillis();
                String jsonString = generateJson(markdownPlan, attempt);
                long generateDuration = System.currentTimeMillis() - generateStart;
                log.info("  → [转换尝试 {}/{}] [步骤1] AI生成完成，耗时: {}ms, JSON长度: {} 字符", 
                        attempt, MAX_RETRIES, generateDuration, jsonString != null ? jsonString.length() : 0);

                // 步骤2：验证JSON（验证器智能体）
                log.info("  → [转换尝试 {}/{}] [步骤2] 验证JSON格式和结构...", attempt, MAX_RETRIES);
                long validateStart = System.currentTimeMillis();
                JsonValidatorAgent.ValidationResult validationResult = validatorAgent.validate(jsonString);
                long validateDuration = System.currentTimeMillis() - validateStart;
                
                if (validationResult.isValid()) {
                    log.info("  → [转换尝试 {}/{}] [步骤2] JSON验证通过，耗时: {}ms", 
                            attempt, MAX_RETRIES, validateDuration);
                    
                    // 验证通过，补全缺失字段
                    log.info("  → [转换尝试 {}/{}] [步骤3] 补全JSON缺失字段...", attempt, MAX_RETRIES);
                    long completeStart = System.currentTimeMillis();
                    jsonString = completeAndValidateItineraryJson(jsonString);
                    long completeDuration = System.currentTimeMillis() - completeStart;
                    long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                    
                    log.info("  → [转换尝试 {}/{}] [步骤3] JSON补全完成，耗时: {}ms", 
                            attempt, MAX_RETRIES, completeDuration);
                    log.info("  → [转换尝试 {}/{}] ✅ 计划转换成功！总耗时: {}ms, 最终JSON长度: {} 字符", 
                            attempt, MAX_RETRIES, attemptDuration, jsonString.length());
                    return jsonString;
                } else {
                    log.warn("  → [转换尝试 {}/{}] [步骤2] JSON验证失败，耗时: {}ms", 
                            attempt, MAX_RETRIES, validateDuration);
                    log.warn("  → [转换尝试 {}/{}] 验证错误: {}", attempt, MAX_RETRIES, validationResult.getErrorMessage());
                    if (!validationResult.getWarnings().isEmpty()) {
                        log.warn("  → [转换尝试 {}/{}] 验证警告: {}", attempt, MAX_RETRIES, 
                                String.join("; ", validationResult.getWarnings()));
                    }
                    
                    // 验证失败，尝试修复
                    log.info("  → [转换尝试 {}/{}] [步骤3] 尝试修复JSON...", attempt, MAX_RETRIES);
                    try {
                        long repairStart = System.currentTimeMillis();
                        jsonString = repairAgent.repairJson(jsonString, markdownPlan);
                        long repairDuration = System.currentTimeMillis() - repairStart;
                        log.info("  → [转换尝试 {}/{}] [步骤3] JSON修复完成，耗时: {}ms, 修复后长度: {} 字符", 
                                attempt, MAX_RETRIES, repairDuration, jsonString != null ? jsonString.length() : 0);
                        
                        // 再次验证
                        log.info("  → [转换尝试 {}/{}] [步骤4] 重新验证修复后的JSON...", attempt, MAX_RETRIES);
                        long revalidateStart = System.currentTimeMillis();
                        validationResult = validatorAgent.validate(jsonString);
                        long revalidateDuration = System.currentTimeMillis() - revalidateStart;
                        
                        if (validationResult.isValid()) {
                            log.info("  → [转换尝试 {}/{}] [步骤4] 修复后验证通过，耗时: {}ms", 
                                    attempt, MAX_RETRIES, revalidateDuration);
                            
                            log.info("  → [转换尝试 {}/{}] [步骤5] 补全修复后的JSON...", attempt, MAX_RETRIES);
                            long completeStart = System.currentTimeMillis();
                            jsonString = completeAndValidateItineraryJson(jsonString);
                            long completeDuration = System.currentTimeMillis() - completeStart;
                            long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                            
                            log.info("  → [转换尝试 {}/{}] [步骤5] JSON补全完成，耗时: {}ms", 
                                    attempt, MAX_RETRIES, completeDuration);
                            log.info("  → [转换尝试 {}/{}] ✅ JSON修复成功！总耗时: {}ms, 最终JSON长度: {} 字符", 
                                    attempt, MAX_RETRIES, attemptDuration, jsonString.length());
                            return jsonString;
                        } else {
                            log.warn("  → [转换尝试 {}/{}] [步骤4] 修复后验证仍然失败，耗时: {}ms", 
                                    attempt, MAX_RETRIES, revalidateDuration);
                            log.warn("  → [转换尝试 {}/{}] 修复后验证错误: {}", attempt, MAX_RETRIES, 
                                    validationResult.getErrorMessage());
                        }
                    } catch (Exception repairException) {
                        log.error("  → [转换尝试 {}/{}] [步骤3] JSON修复失败: {}", 
                                attempt, MAX_RETRIES, repairException.getMessage(), repairException);
                    }
                }

                // 如果最后一次尝试，抛出异常
                if (attempt == MAX_RETRIES) {
                    long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                    log.error("  → [转换尝试 {}/{}] ❌ 计划转换最终失败，总耗时: {}ms", 
                            attempt, MAX_RETRIES, attemptDuration);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, 
                            "计划转换失败，已重试" + MAX_RETRIES + "次：" + validationResult.getErrorMessage());
                }

                // 等待后重试
                int delaySeconds = attempt;
                log.info("  → [转换尝试 {}/{}] 等待 {} 秒后重试...", attempt, MAX_RETRIES, delaySeconds);
                Thread.sleep(1000 * delaySeconds); // 递增延迟：1s, 2s, 3s

            } catch (BusinessException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "转换过程被中断");
            } catch (Exception e) {
                lastException = e;
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                log.warn("  → [转换尝试 {}/{}] ❌ 计划转换失败，耗时: {}ms, 错误: {}", 
                        attempt, MAX_RETRIES, attemptDuration, e.getMessage());
                
                if (attempt == MAX_RETRIES) {
                    log.error("  → [转换尝试 {}/{}] ❌ 计划转换最终失败，已重试{}次", 
                            attempt, MAX_RETRIES, MAX_RETRIES, e);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, 
                            "计划转换失败：" + e.getMessage());
                }
            }
        }

        // 理论上不会到达这里
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, 
                "计划转换失败：" + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    /**
     * 生成JSON（生成器智能体）
     */
    private String generateJson(String markdownPlan, int attempt) {
        // 生成Prompt
        String systemPrompt = converterPrompt.generateSystemPrompt(markdownPlan);
        
        // 如果是重试，在prompt中添加提示
        if (attempt > 1) {
            systemPrompt += "\n\n**重要提示（第" + attempt + "次尝试）：**\n" +
                    "- 请确保输出完整的JSON，不要截断\n" +
                    "- 确保所有括号、引号都正确闭合\n" +
                    "- 确保JSON格式完全正确，可以被解析\n";
        }

        // 调用AI转换
        String aiResponse = aiClient.chat(systemPrompt, Collections.emptyList());

        // 提取JSON（可能包含在代码块中）
        String jsonString = extractJsonFromResponse(aiResponse);
        
        return jsonString;
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
