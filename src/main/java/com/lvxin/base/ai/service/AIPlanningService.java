package com.lvxin.base.ai.service;

import com.lvxin.base.ai.agent.PlanConverterAgent;
import com.lvxin.base.ai.agent.TravelPlanningAgent;
import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ConversationStage;
import com.lvxin.base.constant.ItineraryStatus;
import com.lvxin.base.service.ItineraryService;
import com.lvxin.base.service.bo.ItineraryBO;
import com.lvxin.base.util.ThreadPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

/**
 * AI规划服务
 * 协调两个智能体的工作
 *
 * @author lvxin
 */
@Slf4j
@Service
public class AIPlanningService {

    private final AIConversationService conversationService;
    private final TravelPlanningAgent planningAgent;
    private final PlanConverterAgent converterAgent;
    private final ItineraryService itineraryService;
    
    @Resource
    private ThreadPoolManager threadPoolManager;

    public AIPlanningService(
            AIConversationService conversationService,
            TravelPlanningAgent planningAgent,
            PlanConverterAgent converterAgent,
            ItineraryService itineraryService) {
        this.conversationService = conversationService;
        this.planningAgent = planningAgent;
        this.converterAgent = converterAgent;
        this.itineraryService = itineraryService;
    }

    /**
     * 处理用户消息（智能体1）
     *
     * @param conversationId 对话ID（可选，首次为空）
     * @param userMessage    用户消息
     * @param userId         用户ID
     * @return 对话响应
     */
    public ChatResponse chat(String conversationId, String userMessage, Long userId) {
        // 获取或创建对话
        AIConversation conversation = conversationService.getOrCreate(conversationId, userId);

        // 添加用户消息
        conversation.addUserMessage(userMessage);

        // 调用智能体1处理
        String aiReply = planningAgent.chat(conversation);

        // 判断回复是否为计划内容
        boolean isPlan = conversation.getStage() == ConversationStage.PLAN_GENERATING
                && conversation.getPlanMarkdown() != null
                && aiReply.equals(conversation.getPlanMarkdown());

        // 添加AI回复
        conversation.addAssistantMessage(aiReply, isPlan);

        // 保存对话
        conversationService.save(conversation);

        // 构建响应
        return ChatResponse.builder()
                .conversationId(conversation.getConversationId())
                .reply(aiReply)
                .stage(conversation.getStage())
                .planMarkdown(conversation.getPlanMarkdown())
                .canConvert(conversation.canConvert())
                .build();
    }

    /**
     * 转换计划为计划表（异步方式）
     * 立即创建计划（状态为GENERATING），然后异步执行生成
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @return 计划表详情（状态为GENERATING）
     */
    @Transactional(rollbackFor = Exception.class)
    public ItineraryBO convertToItinerary(String conversationId, Long userId) {
        // 获取对话
        AIConversation conversation = conversationService.get(conversationId);
        if (conversation == null) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.NOT_FOUND,
                    "对话不存在"
            );
        }

        // 验证用户权限
        if (!conversation.getUserId().equals(userId)) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.FORBIDDEN,
                    "无权访问此对话"
            );
        }

        // 验证是否可以转换
        // 如果planMarkdown为空，尝试从最后一条AI消息中提取计划内容
        boolean planMarkdownUpdated = false;
        if (conversation.getPlanMarkdown() == null || conversation.getPlanMarkdown().isEmpty()) {
            String lastPlanMessage = conversation.getMessages().stream()
                    .filter(msg -> "assistant".equals(msg.getRole()) 
                            && (Boolean.TRUE.equals(msg.getIsPlan()) || isPlanContent(msg.getContent())))
                    .reduce((first, second) -> second)
                    .map(com.lvxin.base.ai.model.ChatMessage::getContent)
                    .orElse(null);
            
            if (lastPlanMessage != null && !lastPlanMessage.isEmpty() && lastPlanMessage.length() > 500) {
                log.info("从最后一条AI消息中提取计划内容，对话ID: {}", conversationId);
                conversation.setPlanMarkdown(lastPlanMessage);
                planMarkdownUpdated = true;
            }
        }
        
        // 如果计划已生成但阶段不是PLAN_READY，自动切换到PLAN_READY
        // 无论当前是什么阶段，只要有计划内容，就应该允许转换
        if (conversation.getPlanMarkdown() != null && !conversation.getPlanMarkdown().isEmpty()) {
            if (conversation.getStage() != ConversationStage.PLAN_READY) {
                log.info("计划已生成，自动切换到计划就绪阶段，对话ID: {}, 原阶段: {}", 
                        conversationId, conversation.getStage());
                conversation.setStage(ConversationStage.PLAN_READY);
                planMarkdownUpdated = true;
            }
        }
        
        // 如果更新了计划内容或阶段，保存对话
        if (planMarkdownUpdated) {
            conversationService.save(conversation);
        }
        
        // 再次检查是否可以转换
        if (!conversation.canConvert()) {
            // 如果仍然不能转换，提供更详细的错误信息
            String errorMsg = "计划尚未就绪，无法转换";
            if (conversation.getPlanMarkdown() == null || conversation.getPlanMarkdown().isEmpty()) {
                errorMsg = "计划内容为空，无法转换";
            } else if (conversation.getStage() != ConversationStage.PLAN_READY) {
                errorMsg = String.format("计划阶段不正确（当前：%s），无法转换", conversation.getStage());
            }
            log.warn("转换计划失败，对话ID: {}, 阶段: {}, planMarkdown为空: {}", 
                    conversationId, conversation.getStage(), 
                    conversation.getPlanMarkdown() == null || conversation.getPlanMarkdown().isEmpty());
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.BAD_REQUEST,
                    errorMsg
            );
        }

        // 解析标题（从planMarkdown中提取或使用默认值）
        String title = "AI生成的旅行计划";
        if (conversation.getPlanMarkdown() != null && conversation.getPlanMarkdown().length() > 0) {
            // 尝试从markdown中提取标题（第一行或包含"标题"的行）
            String[] lines = conversation.getPlanMarkdown().split("\n");
            for (String line : lines) {
                if (line.contains("标题") || line.contains("title") || line.startsWith("#")) {
                    title = line.replace("#", "").replace("标题", "").replace("title", "").trim();
                    if (title.length() > 50) {
                        title = title.substring(0, 50);
                    }
                    break;
                }
            }
        }

        // 立即创建计划（状态为GENERATING）
        ItineraryBO itineraryBO = itineraryService.createGeneratingItinerary(
                userId,
                title,
                conversation.getPlanMarkdown(), // 原始计划内容
                null // 封面图
        );

        log.info("创建生成中的计划，conversationId: {}, itineraryKey: {}", conversationId, itineraryBO.getItineraryKey());

        // 异步执行生成任务
        String finalItineraryKey = itineraryBO.getItineraryKey();
        String finalPlanMarkdown = conversation.getPlanMarkdown();
        threadPoolManager.defaultExecutor().execute(() -> {
            convertToItineraryAsync(finalItineraryKey, finalPlanMarkdown, userId);
        }, (Exception e) -> {
            log.error("异步转换计划失败，itineraryKey: {}", finalItineraryKey, e);
            // 更新状态为失败
            itineraryService.updateItineraryStatus(
                    finalItineraryKey,
                    ItineraryStatus.FAILED.name(),
                    null,
                    "生成失败：" + e.getMessage()
            );
        });

        return itineraryBO;
    }

    /**
     * 异步执行计划转换（内部方法）
     */
    private void convertToItineraryAsync(String itineraryKey, String planMarkdown, Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("========== 异步计划转换开始 ==========");
        log.info("[{}] 开始异步转换计划", itineraryKey);
        log.info("[{}] 用户ID: {}, 计划内容长度: {} 字符", itineraryKey, userId, planMarkdown != null ? planMarkdown.length() : 0);
        
        try {
            // 步骤1：调用智能体2转换
            log.info("[{}] [步骤1/3] 开始调用AI转换器生成JSON...", itineraryKey);
            long step1Start = System.currentTimeMillis();
            String jsonString = converterAgent.convertToJson(planMarkdown);
            long step1Duration = System.currentTimeMillis() - step1Start;
            log.info("[{}] [步骤1/3] AI转换完成，耗时: {}ms, JSON长度: {} 字符", 
                    itineraryKey, step1Duration, jsonString != null ? jsonString.length() : 0);

            // 步骤2：补全和验证JSON
            log.info("[{}] [步骤2/3] 开始补全和验证JSON...", itineraryKey);
            long step2Start = System.currentTimeMillis();
            jsonString = converterAgent.completeAndValidateItineraryJson(jsonString);
            long step2Duration = System.currentTimeMillis() - step2Start;
            log.info("[{}] [步骤2/3] JSON补全完成，耗时: {}ms, 最终JSON长度: {} 字符", 
                    itineraryKey, step2Duration, jsonString != null ? jsonString.length() : 0);

            // 步骤3：更新计划状态为正常，并保存JSON数据
            log.info("[{}] [步骤3/3] 开始更新计划状态为NORMAL并保存JSON...", itineraryKey);
            long step3Start = System.currentTimeMillis();
            itineraryService.updateItineraryStatus(
                    itineraryKey,
                    ItineraryStatus.NORMAL.name(),
                    jsonString,
                    null
            );
            long step3Duration = System.currentTimeMillis() - step3Start;
            long totalDuration = System.currentTimeMillis() - startTime;
            
            log.info("[{}] [步骤3/3] 计划状态更新完成，耗时: {}ms", itineraryKey, step3Duration);
            log.info("[{}] ✅ 异步转换计划成功！总耗时: {}ms (步骤1: {}ms, 步骤2: {}ms, 步骤3: {}ms)", 
                    itineraryKey, totalDuration, step1Duration, step2Duration, step3Duration);
            log.info("========== 异步计划转换完成 ==========");

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[{}] ❌ 异步转换计划失败，总耗时: {}ms", itineraryKey, totalDuration, e);
            log.error("[{}] 错误类型: {}, 错误消息: {}", 
                    itineraryKey, e.getClass().getSimpleName(), e.getMessage());
            
            // 更新状态为失败
            String errorMessage = "生成失败：" + e.getMessage();
            if (errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 497) + "...";
            }
            
            try {
                itineraryService.updateItineraryStatus(
                        itineraryKey,
                        ItineraryStatus.FAILED.name(),
                        null,
                        errorMessage
                );
                log.info("[{}] 计划状态已更新为FAILED", itineraryKey);
            } catch (Exception updateException) {
                log.error("[{}] 更新计划状态为FAILED时发生异常", itineraryKey, updateException);
            }
            
            log.info("========== 异步计划转换失败 ==========");
        }
    }

    /**
     * 文本转计划表（异步方式）
     * 立即创建计划（状态为GENERATING），然后异步执行生成
     *
     * @param text   文本格式的旅行计划
     * @param userId 用户ID
     * @return 计划表详情（状态为GENERATING）
     */
    @Transactional(rollbackFor = Exception.class)
    public ItineraryBO convertTextToItinerary(String text, Long userId) {
        if (text == null || text.trim().isEmpty()) {
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.BAD_REQUEST,
                    "计划文本不能为空"
            );
        }

        log.info("开始转换文本计划（异步），userId: {}, textLength: {}", userId, text.length());

        // 解析标题（从文本中提取或使用默认值）
        String title = "文本转换的旅行计划";
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.contains("标题") || line.contains("title") || line.startsWith("#")) {
                title = line.replace("#", "").replace("标题", "").replace("title", "").trim();
                if (title.length() > 50) {
                    title = title.substring(0, 50);
                }
                break;
            }
        }

        // 立即创建计划（状态为GENERATING）
        ItineraryBO itineraryBO = itineraryService.createGeneratingItinerary(
                userId,
                title,
                text, // 原始计划内容
                null // 封面图
        );

        log.info("创建生成中的计划，itineraryKey: {}", itineraryBO.getItineraryKey());

        // 异步执行生成任务
        String finalItineraryKey = itineraryBO.getItineraryKey();
        threadPoolManager.defaultExecutor().execute(() -> {
            convertTextToItineraryAsync(finalItineraryKey, text, userId);
        }, (Exception e) -> {
            log.error("异步转换文本计划失败，itineraryKey: {}", finalItineraryKey, e);
            // 更新状态为失败
            itineraryService.updateItineraryStatus(
                    finalItineraryKey,
                    ItineraryStatus.FAILED.name(),
                    null,
                    "生成失败：" + e.getMessage()
            );
        });

        return itineraryBO;
    }

    /**
     * 异步执行文本转计划表（内部方法）
     */
    private void convertTextToItineraryAsync(String itineraryKey, String text, Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("========== 异步文本转计划表开始 ==========");
        log.info("[{}] 开始异步转换文本计划", itineraryKey);
        log.info("[{}] 用户ID: {}, 文本长度: {} 字符", itineraryKey, userId, text != null ? text.length() : 0);
        
        try {
            // 步骤1：调用智能体2转换
            log.info("[{}] [步骤1/3] 开始调用AI转换器生成JSON...", itineraryKey);
            long step1Start = System.currentTimeMillis();
            String jsonString = converterAgent.convertToJson(text);
            long step1Duration = System.currentTimeMillis() - step1Start;
            log.info("[{}] [步骤1/3] AI转换完成，耗时: {}ms, JSON长度: {} 字符", 
                    itineraryKey, step1Duration, jsonString != null ? jsonString.length() : 0);

            // 步骤2：补全和验证JSON
            log.info("[{}] [步骤2/3] 开始补全和验证JSON...", itineraryKey);
            long step2Start = System.currentTimeMillis();
            jsonString = converterAgent.completeAndValidateItineraryJson(jsonString);
            long step2Duration = System.currentTimeMillis() - step2Start;
            log.info("[{}] [步骤2/3] JSON补全完成，耗时: {}ms, 最终JSON长度: {} 字符", 
                    itineraryKey, step2Duration, jsonString != null ? jsonString.length() : 0);

            // 步骤3：更新计划状态为正常，并保存JSON数据
            log.info("[{}] [步骤3/3] 开始更新计划状态为NORMAL并保存JSON...", itineraryKey);
            long step3Start = System.currentTimeMillis();
            itineraryService.updateItineraryStatus(
                    itineraryKey,
                    ItineraryStatus.NORMAL.name(),
                    jsonString,
                    null
            );
            long step3Duration = System.currentTimeMillis() - step3Start;
            long totalDuration = System.currentTimeMillis() - startTime;
            
            log.info("[{}] [步骤3/3] 计划状态更新完成，耗时: {}ms", itineraryKey, step3Duration);
            log.info("[{}] ✅ 异步转换文本计划成功！总耗时: {}ms (步骤1: {}ms, 步骤2: {}ms, 步骤3: {}ms)", 
                    itineraryKey, totalDuration, step1Duration, step2Duration, step3Duration);
            log.info("========== 异步文本转计划表完成 ==========");

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[{}] ❌ 异步转换文本计划失败，总耗时: {}ms", itineraryKey, totalDuration, e);
            log.error("[{}] 错误类型: {}, 错误消息: {}", 
                    itineraryKey, e.getClass().getSimpleName(), e.getMessage());
            
            // 更新状态为失败
            String errorMessage = "生成失败：" + e.getMessage();
            if (errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 497) + "...";
            }
            
            try {
                itineraryService.updateItineraryStatus(
                        itineraryKey,
                        ItineraryStatus.FAILED.name(),
                        null,
                        errorMessage
                );
                log.info("[{}] 计划状态已更新为FAILED", itineraryKey);
            } catch (Exception updateException) {
                log.error("[{}] 更新计划状态为FAILED时发生异常", itineraryKey, updateException);
            }
            
            log.info("========== 异步文本转计划表失败 ==========");
        }
    }

    /**
     * 判断内容是否是计划内容
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
     * 对话响应
     */
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
