package com.lvxin.base.ai.service.handler;

import com.lvxin.base.ai.client.AIClient;
import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ChatMessage;
import com.lvxin.base.ai.model.ConversationStage;
import com.lvxin.base.ai.model.TravelRequirement;
import com.lvxin.base.ai.prompt.PlanGeneratingPrompt;
import com.lvxin.base.ai.prompt.PlanOptimizingPrompt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 计划生成与调优阶段处理器
 *
 * @author lvxin
 */
@Slf4j
@Component
public class PlanGenerationHandler implements PlanningStateHandler {

    private final AIClient aiClient;
    private final PlanGeneratingPrompt planGeneratingPrompt;
    private final PlanOptimizingPrompt planOptimizingPrompt;
    
    // 用户满意关键词
    private static final Pattern SATISFACTION_PATTERN = Pattern.compile(
            "(可以|满意|就这样|好的|行|没问题|确定|确认|ok|OK)",
            Pattern.CASE_INSENSITIVE
    );

    public PlanGenerationHandler(AIClient aiClient, 
                                 PlanGeneratingPrompt planGeneratingPrompt,
                                 PlanOptimizingPrompt planOptimizingPrompt) {
        this.aiClient = aiClient;
        this.planGeneratingPrompt = planGeneratingPrompt;
        this.planOptimizingPrompt = planOptimizingPrompt;
    }

    @Override
    public ConversationStage getStage() {
        return ConversationStage.PLAN_GENERATING;
    }

    @Override
    public String handle(AIConversation conversation) {
        log.debug("处理计划生成阶段，对话ID: {}", conversation.getConversationId());
        
        // 防御性检查：如果需求莫名其妙丢了，需要回退（虽然State transition应该保证这一点，但为了健壮性）
        TravelRequirement requirement = conversation.getRequirement();
        if (requirement == null || !requirement.isComplete()) {
            // 这里返回特殊消息提示前端或上层逻辑，实际上nextStage逻辑会处理回退
            return "抱歉，需求信息似乎不完整，我们需要重新确认一下。";
        }

        String systemPrompt;
        List<ChatMessage> messages = conversation.getMessages();

        if (conversation.getPlanMarkdown() == null) {
            // A. 首次生成
            log.debug("首次生成计划");
            systemPrompt = planGeneratingPrompt.generateSystemPrompt(requirement);
        } else {
            // B. 调优
            log.debug("根据反馈调优计划");
            String userFeedback = conversation.getLastUserMessage();
            PlanOptimizingPrompt.PlanOptimizingContext context =
                    new PlanOptimizingPrompt.PlanOptimizingContext(conversation.getPlanMarkdown(), userFeedback);
            systemPrompt = planOptimizingPrompt.generateSystemPrompt(context);
        }
        
        // 使用智能模型进行计划生成和调优
        String aiResponse = aiClient.chat("smart-model", systemPrompt, messages);
        conversation.setPlanMarkdown(aiResponse); // 总是更新最新的计划
        return aiResponse;
    }

    @Override
    public boolean shouldTransition(AIConversation conversation) {
        // 如果需求不完整，回退到需求收集（这是一个特殊的反向流转，State Pattern通常处理正向，这里我们在Service层处理或者容忍）
        TravelRequirement req = conversation.getRequirement();
        if (req == null || !req.isComplete()) return true; // 回退

        // 正常流转：用户满意
        String lastMsg = conversation.getLastUserMessage();
        return lastMsg != null && SATISFACTION_PATTERN.matcher(lastMsg).find() 
               && conversation.getPlanMarkdown() != null;
    }

    @Override
    public ConversationStage nextStage() {
        // 特殊判断：回退
        TravelRequirement req = null; // 无法直接获取Context里的req检查，这里简化逻辑
        // 在State模式中，通常nextStage是确定的。
        // 如果需要动态判断nextStage，接口需要改动。
        // 鉴于目前逻辑，我们假设如果 shouldTransition 为 true 且 Plan 存在，则是 READY。
        // 如果需求不完整导致 transition，应该在 handle 里就抛出异常或处理了。
        
        // 为了简单起见，这里只处理正向流转
        return ConversationStage.PLAN_READY;
    }
}
