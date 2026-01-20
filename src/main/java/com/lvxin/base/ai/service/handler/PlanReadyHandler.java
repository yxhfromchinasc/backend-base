package com.lvxin.base.ai.service.handler;

import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ConversationStage;
import org.springframework.stereotype.Component;

/**
 * 计划就绪阶段处理器
 * 此时计划已定稿，用户主要是在询问关于计划的细节，或者发起转换请求
 *
 * @author lvxin
 */
@Component
public class PlanReadyHandler implements PlanningStateHandler {

    @Override
    public ConversationStage getStage() {
        return ConversationStage.PLAN_READY;
    }

    @Override
    public String handle(AIConversation conversation) {
        // 这一阶段通常只是简单的问答，或者提示用户点击生成按钮
        return "计划已准备就绪！您可以点击“生成行程表”按钮将其转换为详细的可编辑行程。或者如果您还有其他问题，我们可以继续聊聊。";
    }

    @Override
    public boolean shouldTransition(AIConversation conversation) {
        // 如果用户想修改，可能回退到 GENERATING，目前暂不支持自动回退
        return false;
    }

    @Override
    public ConversationStage nextStage() {
        return ConversationStage.PLAN_READY;
    }
}
