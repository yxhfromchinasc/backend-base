package com.lvxin.base.ai.agent;

import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ChatMessage;
import com.lvxin.base.ai.model.ConversationStage;
import com.lvxin.base.ai.model.TravelRequirement;
import com.lvxin.base.ai.prompt.PlanGeneratingPrompt;
import com.lvxin.base.ai.prompt.PlanOptimizingPrompt;
import com.lvxin.base.ai.prompt.RequirementCollectingPrompt;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 智能体1：旅行规划助手
 * 负责两阶段对话：需求收集和计划生成与调优
 *
 * @author lvxin
 */
@Slf4j
@Component
public class TravelPlanningAgent {

    private final AIClient aiClient;
    private final RequirementCollectingPrompt requirementPrompt;
    private final PlanGeneratingPrompt planGeneratingPrompt;
    private final PlanOptimizingPrompt planOptimizingPrompt;

    // 用户满意关键词（用于判断是否可以转换）
    private static final Pattern SATISFACTION_PATTERN = Pattern.compile(
            "(可以|满意|就这样|好的|行|没问题|确定|确认|ok|OK|好的|就这样吧)",
            Pattern.CASE_INSENSITIVE
    );

    public TravelPlanningAgent(
            AIClient aiClient,
            RequirementCollectingPrompt requirementPrompt,
            PlanGeneratingPrompt planGeneratingPrompt,
            PlanOptimizingPrompt planOptimizingPrompt) {
        this.aiClient = aiClient;
        this.requirementPrompt = requirementPrompt;
        this.planGeneratingPrompt = planGeneratingPrompt;
        this.planOptimizingPrompt = planOptimizingPrompt;
    }

    /**
     * 处理用户消息
     *
     * @param conversation 对话对象
     * @return AI回复
     */
    public String chat(AIConversation conversation) {
        String userMessage = conversation.getLastUserMessage();
        if (userMessage == null || userMessage.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户消息不能为空");
        }

        String aiReply;
        ConversationStage currentStage = conversation.getStage();

        try {
            if (currentStage == ConversationStage.REQUIREMENT_COLLECTING) {
                // 阶段1：需求收集
                aiReply = handleRequirementCollecting(conversation);
            } else {
                // 阶段2：计划生成与调优
                aiReply = handlePlanGenerating(conversation);
            }

            // 检查是否需要切换阶段
            checkStageTransition(conversation);

            return aiReply;
        } catch (Exception e) {
            log.error("AI规划助手处理失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI规划助手处理失败：" + e.getMessage());
        }
    }

    /**
     * 阶段1：需求收集
     */
    private String handleRequirementCollecting(AIConversation conversation) {
        log.debug("处理需求收集阶段，对话ID: {}", conversation.getConversationId());

        // 生成Prompt
        String systemPrompt = requirementPrompt.generateSystemPrompt(conversation.getRequirement());

        // 获取对话历史（转换为ChatMessage列表）
        List<ChatMessage> messages = conversation.getMessages();

        // 调用AI
        String reply = aiClient.chat(systemPrompt, messages);

        // 尝试从对话中提取需求信息
        extractRequirementAsync(conversation);

        // 如果AI回复看起来像是计划（包含计划关键词且长度足够），保存为计划
        if (conversation.getPlanMarkdown() == null && isPlanContent(reply)) {
            conversation.setPlanMarkdown(reply);
            log.debug("检测到计划内容，已保存，对话ID: {}", conversation.getConversationId());
        }

        return reply;
    }

    /**
     * 判断内容是否是计划
     */
    private boolean isPlanContent(String content) {
        if (content == null || content.length() < 500) {
            return false;
        }
        // 检查是否包含计划相关的关键词
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("day") || lowerContent.contains("第") || 
               lowerContent.contains("行程") || lowerContent.contains("计划") || 
               lowerContent.contains("安排") || lowerContent.contains("上午") ||
               lowerContent.contains("下午") || lowerContent.contains("晚上");
    }

    /**
     * 阶段2：计划生成与调优
     */
    private String handlePlanGenerating(AIConversation conversation) {
        log.debug("处理计划生成阶段，对话ID: {}", conversation.getConversationId());

        TravelRequirement requirement = conversation.getRequirement();
        if (requirement == null || !requirement.isComplete()) {
            // 如果需求不完整，返回需求收集阶段
            conversation.setStage(ConversationStage.REQUIREMENT_COLLECTING);
            return "抱歉，我需要先收集完整的旅行需求信息。让我重新了解一下您的需求...";
        }

        String systemPrompt;
        List<ChatMessage> messages = conversation.getMessages();

        if (conversation.getPlanMarkdown() == null) {
            // 首次生成计划
            log.debug("首次生成计划");
            systemPrompt = planGeneratingPrompt.generateSystemPrompt(requirement);
            String planMarkdown = aiClient.chat(systemPrompt, messages);
            conversation.setPlanMarkdown(planMarkdown);
            return planMarkdown;
        } else {
            // 根据用户反馈调优计划
            log.debug("根据用户反馈调优计划");
            String userFeedback = conversation.getLastUserMessage();
            PlanOptimizingPrompt.PlanOptimizingContext context =
                    new PlanOptimizingPrompt.PlanOptimizingContext(
                            conversation.getPlanMarkdown(),
                            userFeedback
                    );
            systemPrompt = planOptimizingPrompt.generateSystemPrompt(context);
            String optimizedPlan = aiClient.chat(systemPrompt, messages);
            conversation.setPlanMarkdown(optimizedPlan);
            return optimizedPlan;
        }
    }

    /**
     * 检查阶段切换
     */
    private void checkStageTransition(AIConversation conversation) {
        ConversationStage currentStage = conversation.getStage();

        if (currentStage == ConversationStage.REQUIREMENT_COLLECTING) {
            // 检查需求是否收集完整，或者AI已经生成了计划
            TravelRequirement requirement = conversation.getRequirement();
            boolean requirementComplete = requirement != null && requirement.isComplete();
            
            // 如果AI回复看起来像是计划（包含"Day"、"第1天"、"行程"等关键词），也切换到计划生成阶段
            String lastReply = conversation.getMessages().stream()
                    .filter(msg -> "assistant".equals(msg.getRole()))
                    .reduce((first, second) -> second)
                    .map(ChatMessage::getContent)
                    .orElse("");
            boolean looksLikePlan = lastReply.contains("Day") || lastReply.contains("第") || 
                    lastReply.contains("行程") || lastReply.contains("计划") || lastReply.contains("安排");
            
            if (requirementComplete || (looksLikePlan && lastReply.length() > 1000)) {
                log.info("需求收集完成或已生成计划，切换到计划生成阶段，对话ID: {}", conversation.getConversationId());
                conversation.setStage(ConversationStage.PLAN_GENERATING);
                // 如果还没有保存计划，保存当前回复作为计划
                if (conversation.getPlanMarkdown() == null && looksLikePlan) {
                    conversation.setPlanMarkdown(lastReply);
                }
            }
        } else if (currentStage == ConversationStage.PLAN_GENERATING) {
            // 检查用户是否满意计划
            String lastUserMessage = conversation.getLastUserMessage();
            if (isUserSatisfied(lastUserMessage) && conversation.getPlanMarkdown() != null) {
                log.info("用户满意计划，切换到计划就绪阶段，对话ID: {}", conversation.getConversationId());
                conversation.setStage(ConversationStage.PLAN_READY);
            } else if (conversation.getPlanMarkdown() != null) {
                // 如果计划已生成但用户还没确认，也更新计划内容
                String lastReply = conversation.getMessages().stream()
                        .filter(msg -> "assistant".equals(msg.getRole()))
                        .reduce((first, second) -> second)
                        .map(ChatMessage::getContent)
                        .orElse("");
                if (lastReply.length() > 500) {
                    conversation.setPlanMarkdown(lastReply);
                }
            }
        }
    }

    /**
     * 判断用户是否满意计划
     */
    private boolean isUserSatisfied(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return false;
        }
        return SATISFACTION_PATTERN.matcher(userMessage).find();
    }

    /**
     * 从对话历史中提取需求信息
     * 使用简单的文本解析提取关键信息
     */
    private void extractRequirementAsync(AIConversation conversation) {
        TravelRequirement requirement = conversation.getRequirement();
        if (requirement == null) {
            requirement = TravelRequirement.builder().build();
            conversation.setRequirement(requirement);
        }

        // 从对话历史中提取信息
        List<ChatMessage> messages = conversation.getMessages();
        String allText = messages.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .map(ChatMessage::getContent)
                .reduce("", (a, b) -> a + " " + b);

        // 提取出发地（优先提取，因为很重要）
        extractDepartureCity(requirement, allText);
        
        // 提取目的地（包含"去"、"到"等关键词）
        extractDestinations(requirement, allText);

        // 提取日期和天数
        extractDatesAndDays(requirement, allText);

        // 提取预算
        extractBudget(requirement, allText);

        // 提取偏好
        extractPreferences(requirement, allText);

        // 提取人数
        extractTravelers(requirement, allText);
    }

    private void extractDepartureCity(TravelRequirement requirement, String text) {
        if (requirement.getDepartureCity() != null && !requirement.getDepartureCity().isEmpty()) {
            return; // 已提取过
        }

        // 提取出发地关键词：从、出发、起点等
        String[] keywords = {"从", "出发", "起点", "自", "由"};
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index >= 0) {
                String after = text.substring(index + keyword.length()).trim();
                // 提取后面的城市名（1-4个字符，通常是城市名）
                String[] parts = after.split("[，,。.\\s、]");
                if (parts.length > 0) {
                    String city = parts[0].trim();
                    // 城市名通常是2-4个字符（如：北京、上海、深圳、杭州）
                    if (city.length() >= 2 && city.length() <= 4) {
                        requirement.setDepartureCity(city);
                        return;
                    }
                }
            }
        }
        
        // 如果没有找到，尝试匹配常见城市名（简化处理）
        String[] commonCities = {"北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "西安", "南京", "武汉"};
        for (String city : commonCities) {
            if (text.contains(city) && (text.contains("从") || text.contains("出发"))) {
                requirement.setDepartureCity(city);
                return;
            }
        }
    }

    private void extractDestinations(TravelRequirement requirement, String text) {
        if (!requirement.getDestinations().isEmpty()) {
            return; // 已提取过
        }

        // 简单匹配：包含"去"、"到"等关键词后的地名
        String[] keywords = {"去", "到", "想去", "计划去", "打算去"};
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index >= 0) {
                String after = text.substring(index + keyword.length()).trim();
                // 提取后面的1-3个字符作为地名（简化处理）
                String[] parts = after.split("[，,。.\\s]");
                if (parts.length > 0) {
                    String destination = parts[0].trim();
                    if (destination.length() > 0 && destination.length() < 20) {
                        requirement.getDestinations().add(destination);
                        return;
                    }
                }
            }
        }
    }

    private void extractDatesAndDays(TravelRequirement requirement, String text) {
        // 提取天数（包含"天"、"日"等）
        if (requirement.getDays() == null) {
            java.util.regex.Pattern dayPattern = java.util.regex.Pattern.compile("(\\d+)\\s*天");
            java.util.regex.Matcher matcher = dayPattern.matcher(text);
            if (matcher.find()) {
                try {
                    requirement.setDays(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }

        // 提取日期（简化处理，实际可以使用更复杂的日期解析）
        // TODO: 实现日期提取
    }

    private void extractBudget(TravelRequirement requirement, String text) {
        if (requirement.getBudget() != null) {
            return;
        }

        // 提取预算范围（如：5000-8000元）
        java.util.regex.Pattern budgetPattern = java.util.regex.Pattern.compile(
                "(\\d+)\\s*[-~到至]\\s*(\\d+)\\s*元?"
        );
        java.util.regex.Matcher matcher = budgetPattern.matcher(text);
        if (matcher.find()) {
            requirement.setBudget(matcher.group(1) + "-" + matcher.group(2) + "元");
        } else {
            // 提取单个预算数字
            java.util.regex.Pattern singleBudgetPattern = java.util.regex.Pattern.compile(
                    "预算\\s*(\\d+)\\s*元?"
            );
            matcher = singleBudgetPattern.matcher(text);
            if (matcher.find()) {
                requirement.setBudget(matcher.group(1) + "元");
            }
        }
    }

    private void extractPreferences(TravelRequirement requirement, String text) {
        if (!requirement.getPreferences().isEmpty()) {
            return;
        }

        // 提取偏好关键词
        String[] preferenceKeywords = {"历史文化", "美食", "自然风光", "购物", "休闲", "历史", "文化"};
        for (String keyword : preferenceKeywords) {
            if (text.contains(keyword)) {
                requirement.getPreferences().add(keyword);
            }
        }
    }

    private void extractTravelers(TravelRequirement requirement, String text) {
        if (requirement.getTravelers() != null) {
            return;
        }

        // 提取人数（包含"人"、"个"等）
        java.util.regex.Pattern travelerPattern = java.util.regex.Pattern.compile(
                "(\\d+)\\s*[人个]"
        );
        java.util.regex.Matcher matcher = travelerPattern.matcher(text);
        if (matcher.find()) {
            try {
                requirement.setTravelers(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException e) {
                // 忽略
            }
        } else {
            // 匹配"2个人"、"一个人"等
            if (text.contains("一个人") || text.contains("1人") || text.contains("独自")) {
                requirement.setTravelers(1);
            } else if (text.contains("两个人") || text.contains("2人") || text.contains("2个人")) {
                requirement.setTravelers(2);
            }
        }
    }
}
