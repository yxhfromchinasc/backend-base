package com.lvxin.base.ai.service;

import com.lvxin.base.ai.agent.TravelPlanningAgent;
import com.lvxin.base.ai.event.ItineraryGenerationEvent;
import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ConversationStage;
import com.lvxin.base.service.ItineraryService;
import com.lvxin.base.service.XiaohongshuCrawlerService;
import com.lvxin.base.service.bo.ItineraryBO;
import com.lvxin.base.util.ImageUrlExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final XiaohongshuCrawlerService xiaohongshuCrawlerService;
    private final ImageUrlExtractor imageUrlExtractor;

    public AIPlanningService(
            AIConversationService conversationService,
            TravelPlanningAgent planningAgent,
            ItineraryService itineraryService,
            ApplicationEventPublisher eventPublisher,
            XiaohongshuCrawlerService xiaohongshuCrawlerService,
            ImageUrlExtractor imageUrlExtractor) {
        this.conversationService = conversationService;
        this.planningAgent = planningAgent;
        this.itineraryService = itineraryService;
        this.eventPublisher = eventPublisher;
        this.xiaohongshuCrawlerService = xiaohongshuCrawlerService;
        this.imageUrlExtractor = imageUrlExtractor;
    }

    /**
     * 表单式AI规划：用户提交表单，跳过需求收集阶段
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse submitForm(String conversationId, com.lvxin.base.ai.model.TravelRequirement requirement, Long userId) {
        // 1. 获取或创建对话
        AIConversation conversation = conversationService.getOrCreate(conversationId, userId);
        
        // 2. 设置收集到的需求
        conversation.setRequirement(requirement);
        
        // 3. 添加一条模拟的用户消息，告知AI需求已通过表单提交
        String userMessage = "这是我的旅行需求：\n" + requirement.getSummary();
        conversation.addUserMessage(userMessage);
        
        // 4. 如果需求已完整，手动将阶段设为需求收集，让Agent在下一次chat时触发流转
        // 或者更直接点，如果已完整，直接调用Agent处理，Agent会自动发现shouldTransition并切到生成阶段
        conversation.setStage(ConversationStage.REQUIREMENT_COLLECTING);
        
        // 5. 调用智能体处理
        String aiReply = planningAgent.chat(conversation);
        
        // 6. 判断是否生成了计划
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

        // 从文本中提取图片URL
        List<String> imageUrls = imageUrlExtractor.extractImageUrlsFromText(text);
        log.info("从文本中提取到 {} 张图片", imageUrls.size());

        String title = extractTitle(text);
        ItineraryBO itineraryBO = itineraryService.createGeneratingItinerary(
                userId, title, text, imageUrls.isEmpty() ? null : imageUrls
        );
        
        log.info("文本计划记录已创建，发布异步生成事件: {}, 封面图片数量: {}", 
                itineraryBO.getItineraryKey(), imageUrls.size());

        // 发布事件
        eventPublisher.publishEvent(new ItineraryGenerationEvent(
                this, 
                itineraryBO.getItineraryKey(), 
                text, 
                userId
        ));

        return itineraryBO;
    }

    /**
     * 小红书链接转计划表
     * 先获取小红书内容，然后转换为计划表
     */
    @Transactional(rollbackFor = Exception.class)
    public ItineraryBO convertXiaohongshuToItinerary(String url, Long userId) {
        if (url == null || url.trim().isEmpty()) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.BAD_REQUEST, "小红书链接不能为空"
            );
        }

        // 获取内容（crawlContent方法内部会处理链接提取和验证）
        XiaohongshuCrawlerService.CrawlResult crawlResult = xiaohongshuCrawlerService.crawlContent(url);
        
        if (!crawlResult.isSuccess()) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.BAD_REQUEST, 
                    "获取失败：" + crawlResult.getErrorMessage()
            );
        }

        String crawledText = crawlResult.getContent();
        if (crawledText == null || crawledText.trim().isEmpty()) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.BAD_REQUEST, 
                    "未能从小红书链接中提取到有效内容"
            );
        }

        // 获取图片列表
        List<String> images = crawlResult.getImages();
        log.info("小红书内容获取成功，文本长度: {}, 图片数量: {}, 开始转换为计划表", 
                crawledText.length(), images.size());

        // 使用获取的文本转换为计划表（复用文本转计划表的逻辑）
        String title = extractTitle(crawledText);
        // 如果标题是默认的，尝试使用"小红书攻略"作为前缀
        if ("AI生成的旅行计划".equals(title)) {
            title = "小红书攻略";
        }
        
        ItineraryBO itineraryBO = itineraryService.createGeneratingItinerary(
                userId, title, crawledText, images.isEmpty() ? null : images
        );
        
        log.info("小红书计划记录已创建，发布异步生成事件: {}", itineraryBO.getItineraryKey());

        // 发布事件
        eventPublisher.publishEvent(new ItineraryGenerationEvent(
                this, 
                itineraryBO.getItineraryKey(), 
                crawledText, 
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