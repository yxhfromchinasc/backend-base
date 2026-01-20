package com.lvxin.base.ai.service;

import com.lvxin.base.ai.agent.TravelPlanningAgent;
import com.lvxin.base.ai.event.ItineraryGenerationEvent;
import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ConversationStage;
import com.lvxin.base.service.ItineraryService;
import com.lvxin.base.service.bo.ItineraryBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI规划服务
 * 协调智能体的工作，重构后只负责核心调度和事件发布
 *
 * @author lvxin
 */
@Slf4j
@Service
public class AIPlanningService {

    private final AIConversationService conversationService;
    private final TravelPlanningAgent planningAgent;
    private final ItineraryService itineraryService;
    private final ApplicationEventPublisher eventPublisher;

    public AIPlanningService(
            AIConversationService conversationService,
            TravelPlanningAgent planningAgent,
            ItineraryService itineraryService,
            ApplicationEventPublisher eventPublisher) {
        this.conversationService = conversationService;
        this.planningAgent = planningAgent;
        this.itineraryService = itineraryService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 处理用户消息（智能体1）
     */
    public ChatResponse chat(String conversationId, String userMessage, Long userId) {
        // 获取或创建对话
        AIConversation conversation = conversationService.getOrCreate(conversationId, userId);
        conversation.addUserMessage(userMessage);

        // 调用智能体1处理 (状态模式分发)
        String aiReply = planningAgent.chat(conversation);

        // 判断是否为计划内容 (简单判断)
        boolean isPlan = conversation.getStage() == ConversationStage.PLAN_GENERATING
                && conversation.getPlanMarkdown() != null
                && aiReply.equals(conversation.getPlanMarkdown());

        conversation.addAssistantMessage(aiReply, isPlan);
        conversationService.save(conversation);

        return ChatResponse.builder()
                .conversationId(conversation.getConversationId())
                .reply(aiReply)
                .stage(conversation.getStage())
                .planMarkdown(conversation.getPlanMarkdown())
                .canConvert(conversation.canConvert())
                .build();
    }

    /**
     * 转换计划为计划表
     * 创建记录后发布Spring事件，由监听器异步处理
     */
    @Transactional(rollbackFor = Exception.class)
    public ItineraryBO convertToItinerary(String conversationId, Long userId) {
        AIConversation conversation = conversationService.get(conversationId);
        validateConversation(conversation, userId);
        
        // 自动提取计划内容兜底
        ensurePlanMarkdown(conversation, conversationId);

        // 创建"生成中"的计划
        String title = extractTitle(conversation.getPlanMarkdown());
        ItineraryBO itineraryBO = itineraryService.createGeneratingItinerary(
                userId, title, conversation.getPlanMarkdown(), null
        );

        log.info("计划记录已创建，发布异步生成事件: {}", itineraryBO.getItineraryKey());
        
        // 发布事件，解耦异步逻辑
        eventPublisher.publishEvent(new ItineraryGenerationEvent(
                this, 
                itineraryBO.getItineraryKey(), 
                conversation.getPlanMarkdown(), 
                userId
        ));

        return itineraryBO;
    }

    /**
     * 文本转计划表
     */
    @Transactional(rollbackFor = Exception.class)
    public ItineraryBO convertTextToItinerary(String text, Long userId) {
        if (text == null || text.trim().isEmpty()) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.BAD_REQUEST, "计划文本不能为空"
            );
        }

        String title = extractTitle(text);
        ItineraryBO itineraryBO = itineraryService.createGeneratingItinerary(
                userId, title, text, null
        );
        
        log.info("文本计划记录已创建，发布异步生成事件: {}", itineraryBO.getItineraryKey());

        // 发布事件
        eventPublisher.publishEvent(new ItineraryGenerationEvent(
                this, 
                itineraryBO.getItineraryKey(), 
                text, 
                userId
        ));

        return itineraryBO;
    }

    // --- 私有辅助方法 ---

    private void validateConversation(AIConversation conversation, Long userId) {
        if (conversation == null) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.NOT_FOUND, "对话不存在"
            );
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.FORBIDDEN, "无权访问此对话"
            );
        }
        if (!conversation.canConvert() && (conversation.getPlanMarkdown() == null || conversation.getPlanMarkdown().isEmpty())) {
             throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.BAD_REQUEST, "计划尚未就绪，无法转换"
            );
        }
    }
    
    private void ensurePlanMarkdown(AIConversation conversation, String cid) {
        if (conversation.getPlanMarkdown() == null || conversation.getPlanMarkdown().isEmpty()) {
            // 尝试从最后一条消息提取
             String lastPlan = conversation.getMessages().stream()
                    .filter(msg -> "assistant".equals(msg.getRole()) && msg.getContent().length() > 200)
                    .reduce((first, second) -> second)
                    .map(com.lvxin.base.ai.model.ChatMessage::getContent)
                    .orElse(null);
             if (lastPlan != null) {
                 conversation.setPlanMarkdown(lastPlan);
                 conversationService.save(conversation);
             }
        }
    }

    private String extractTitle(String markdown) {
        String title = "AI生成的旅行计划";
        if (markdown != null) {
            for (String line : markdown.split("\n")) {
                if (line.contains("标题") || line.contains("title") || line.startsWith("#")) {
                    title = line.replace("#", "").replace("标题", "").replace("title", "").trim();
                    if (title.length() > 50) title = title.substring(0, 50);
                    break;
                }
            }
        }
        return title;
    }

    @lombok.Data
    @lombok.Builder
    public static class ChatResponse {
        private String conversationId;
        private String reply;
        private ConversationStage stage;
        private String planMarkdown;
        private Boolean canConvert;
    }
}