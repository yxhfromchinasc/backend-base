package com.lvxin.base.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI对话模型
 * 管理整个AI规划对话的状态和历史
 *
 * @author lvxin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIConversation {
    /**
     * 对话ID（唯一标识）
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 当前对话阶段
     */
    private ConversationStage stage;

    /**
     * 对话消息历史
     */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 收集的旅行需求
     */
    private TravelRequirement requirement;

    /**
     * Markdown格式的计划（阶段2生成）
     */
    private String planMarkdown;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建新对话
     */
    public static AIConversation create(Long userId) {
        return AIConversation.builder()
                .conversationId(UUID.randomUUID().toString())
                .userId(userId)
                .stage(ConversationStage.REQUIREMENT_COLLECTING)
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        messages.add(ChatMessage.userMessage(content));
        updatedAt = LocalDateTime.now();
    }

    /**
     * 添加AI助手消息
     */
    public void addAssistantMessage(String content, boolean isPlan) {
        messages.add(ChatMessage.assistantMessage(content, isPlan));
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取最后一条用户消息
     */
    public String getLastUserMessage() {
        return messages.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .reduce((first, second) -> second)
                .map(ChatMessage::getContent)
                .orElse("");
    }

    /**
     * 是否可以转换为计划表
     */
    public boolean canConvert() {
        return stage == ConversationStage.PLAN_READY && planMarkdown != null && !planMarkdown.isEmpty();
    }
}
