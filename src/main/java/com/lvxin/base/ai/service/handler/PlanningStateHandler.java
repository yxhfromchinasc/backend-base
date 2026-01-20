package com.lvxin.base.ai.service.handler;

import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ConversationStage;

/**
 * 规划状态处理器接口
 * 状态模式：定义每个对话阶段的行为
 *
 * @author lvxin
 */
public interface PlanningStateHandler {
    
    /**
     * 获取该处理器对应的阶段
     */
    ConversationStage getStage();
    
    /**
     * 处理对话逻辑
     * @param conversation 对话上下文
     * @return AI回复内容
     */
    String handle(AIConversation conversation);
    
    /**
     * 判断是否应该流转到下一个状态
     */
    boolean shouldTransition(AIConversation conversation);
    
    /**
     * 获取下一个状态
     */
    ConversationStage nextStage();
}
