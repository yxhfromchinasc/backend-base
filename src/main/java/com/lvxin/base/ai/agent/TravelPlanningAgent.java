package com.lvxin.base.ai.agent;

import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.ai.model.ConversationStage;
import com.lvxin.base.ai.service.handler.PlanningStateHandler;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能体1：旅行规划助手 (重构版)
 * 职责：作为“状态机引擎”，根据当前对话状态分发给对应的 Handler
 *
 * @author lvxin
 */
@Slf4j
@Component
public class TravelPlanningAgent {

    private final Map<ConversationStage, PlanningStateHandler> stateHandlers;

    public TravelPlanningAgent(List<PlanningStateHandler> handlers) {
        // 将List转为Map，方便查找
        this.stateHandlers = handlers.stream()
                .collect(Collectors.toMap(PlanningStateHandler::getStage, h -> h));
    }

    /**
     * 处理用户消息
     */
    public String chat(AIConversation conversation) {
        String userMessage = conversation.getLastUserMessage();
        if (userMessage == null || userMessage.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户消息不能为空");
        }

        ConversationStage currentStage = conversation.getStage();
        PlanningStateHandler handler = stateHandlers.get(currentStage);

        if (handler == null) {
            log.error("未找到对应状态的处理器: {}", currentStage);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "系统状态异常");
        }

        // 1. 处理当前逻辑
        String aiReply = handler.handle(conversation);

        // 2. 判断是否流转
        if (handler.shouldTransition(conversation)) {
            ConversationStage next = handler.nextStage();
            log.info("对话状态流转: {} -> {}, 对话ID: {}", currentStage, next, conversation.getConversationId());
            conversation.setStage(next);
        }

        return aiReply;
    }
}