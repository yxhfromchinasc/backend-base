package com.lvxin.base.ai.service.handler;

import com.alibaba.fastjson2.JSON;
import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ChatMessage;
import com.lvxin.base.ai.model.ConversationStage;
import com.lvxin.base.ai.model.TravelRequirement;
import com.lvxin.base.ai.prompt.RequirementCollectingPrompt;
import com.lvxin.base.ai.service.AIConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 需求收集阶段处理器
 *
 * @author lvxin
 */
@Slf4j
@Component
public class RequirementCollectionHandler implements PlanningStateHandler {

    private final AIClient aiClient;
    private final RequirementCollectingPrompt requirementPrompt;
    private final AIConversationService conversationService;
    private final Executor executor;
    
    // JSON代码块匹配模式
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    public RequirementCollectionHandler(AIClient aiClient, 
                                        RequirementCollectingPrompt requirementPrompt,
                                        AIConversationService conversationService,
                                        @Qualifier("defaultExecutor") Executor executor) {
        this.aiClient = aiClient;
        this.requirementPrompt = requirementPrompt;
        this.conversationService = conversationService;
        this.executor = executor;
    }

    @Override
    public ConversationStage getStage() {
        return ConversationStage.REQUIREMENT_COLLECTING;
    }

    @Override
    public String handle(AIConversation conversation) {
        log.debug("处理需求收集阶段，对话ID: {}", conversation.getConversationId());
        
        // 1. 生成Prompt
        String systemPrompt = requirementPrompt.generateSystemPrompt(conversation.getRequirement());
        List<ChatMessage> messages = conversation.getMessages();

        // 2. 调用AI (使用快速模型) - 这是必须同步等待的，为了回复用户
        long start = System.currentTimeMillis();
        String reply = aiClient.chat("fast-model", systemPrompt, messages);
        log.info("AI回复耗时: {}ms", System.currentTimeMillis() - start);

        // 3. 异步提取结构化需求 (Side Effect)
        // 这里的操作比较耗时且不需要阻塞用户回复，放入线程池执行
        executor.execute(() -> extractRequirementByAI(conversation));

        // 4. 检查是否意外生成了计划 (Side Effect)
        // 简单的关键词匹配很快，可以同步执行
        if (conversation.getPlanMarkdown() == null && isPlanContent(reply)) {
            conversation.setPlanMarkdown(reply);
            log.debug("检测到计划内容，已保存");
        }

        return reply;
    }

    @Override
    public boolean shouldTransition(AIConversation conversation) {
        // 条件1：需求收集完整
        TravelRequirement req = conversation.getRequirement();
        boolean complete = req != null && req.isComplete();
        
        // 条件2：或者AI已经直接生成了计划文本
        boolean hasPlan = conversation.getPlanMarkdown() != null && !conversation.getPlanMarkdown().isEmpty();
        
        return complete || hasPlan;
    }

    @Override
    public ConversationStage nextStage() {
        return ConversationStage.PLAN_GENERATING;
    }
    
    // --- 私有辅助方法 ---

    private boolean isPlanContent(String content) {
        if (content == null || content.length() < 500) return false;
        String lower = content.toLowerCase();
        return lower.contains("day") || lower.contains("行程") || lower.contains("计划");
    }

    private void extractRequirementByAI(AIConversation conversation) {
        long start = System.currentTimeMillis();
        log.debug("开始异步提取需求...");
        
        TravelRequirement requirement = conversation.getRequirement();
        if (requirement == null) {
            requirement = TravelRequirement.builder().build();
            conversation.setRequirement(requirement);
        }

        String extractionPrompt = """
            请分析上述对话，提取用户的旅行需求，并以严格的JSON格式返回。
            提取字段：departureCity, destinations, days, budget, travelers, preferences。
            如果未提及设为null。只返回JSON。
            """;
            
        try {
            // 使用快速模型进行信息提取
            String jsonResponse = aiClient.chat("fast-model", extractionPrompt, conversation.getMessages());
            String json = extractJson(jsonResponse);
            if (json != null) {
                TravelRequirement extracted = JSON.parseObject(json, TravelRequirement.class);
                mergeRequirements(requirement, extracted);
                
                // 关键：异步更新后，必须手动保存到数据库
                conversationService.save(conversation);
                
                log.info("异步需求提取成功，耗时: {}ms, 结果: {}", System.currentTimeMillis() - start, JSON.toJSONString(requirement));
            }
        } catch (Exception e) {
            log.warn("需求提取失败: {}", e.getMessage());
        }
    }
    
    private String extractJson(String response) {
        if (response == null) return null;
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) return matcher.group(1).trim();
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) return response.substring(start, end + 1).trim();
        return null;
    }
    
    private void mergeRequirements(TravelRequirement target, TravelRequirement source) {
        if (source.getDepartureCity() != null) target.setDepartureCity(source.getDepartureCity());
        if (source.getDestinations() != null && !source.getDestinations().isEmpty()) {
             if (target.getDestinations().isEmpty()) target.setDestinations(source.getDestinations());
             else for (String d : source.getDestinations()) if (!target.getDestinations().contains(d)) target.getDestinations().add(d);
        }
        if (source.getDays() != null && source.getDays() > 0) target.setDays(source.getDays());
        if (source.getBudget() != null) target.setBudget(source.getBudget());
        if (source.getTravelers() != null && source.getTravelers() > 0) target.setTravelers(source.getTravelers());
        if (source.getPreferences() != null && !source.getPreferences().isEmpty()) {
            if (target.getPreferences().isEmpty()) target.setPreferences(source.getPreferences());
            else for (String p : source.getPreferences()) if (!target.getPreferences().contains(p)) target.getPreferences().add(p);
        }
    }
}
