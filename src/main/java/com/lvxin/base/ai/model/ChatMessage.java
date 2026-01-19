package com.lvxin.base.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息模型
 * 表示对话中的一条消息
 *
 * @author lvxin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 角色：user（用户）或 assistant（AI助手）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 是否为计划内容（用于前端渲染）
     */
    private Boolean isPlan;

    /**
     * 创建用户消息
     */
    public static ChatMessage userMessage(String content) {
        return ChatMessage.builder()
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .isPlan(false)
                .build();
    }

    /**
     * 创建AI助手消息
     */
    public static ChatMessage assistantMessage(String content, boolean isPlan) {
        return ChatMessage.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .isPlan(isPlan)
                .build();
    }
}
