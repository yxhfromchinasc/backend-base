package com.lvxin.base.ai.service;

import com.lvxin.base.ai.model.AIConversation;
import com.lvxin.base.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * AI对话服务
 * 负责对话的存储和管理（使用Redis）
 *
 * @author lvxin
 */
@Slf4j
@Service
public class AIConversationService {

    private static final String CONVERSATION_KEY_PREFIX = "ai:conversation:";
    private static final Duration CONVERSATION_TTL = Duration.ofHours(24); // 24小时过期

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public AIConversationService(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取或创建对话
     *
     * @param conversationId 对话ID（如果为null，则创建新对话）
     * @param userId         用户ID
     * @return 对话对象
     */
    public AIConversation getOrCreate(String conversationId, Long userId) {
        if (conversationId != null && !conversationId.isEmpty()) {
            AIConversation conversation = get(conversationId);
            if (conversation != null) {
                // 验证用户权限
                if (!conversation.getUserId().equals(userId)) {
                    throw new com.lvxin.base.exception.BusinessException(
                            com.lvxin.base.exception.ErrorCode.FORBIDDEN,
                            "无权访问此对话"
                    );
                }
                return conversation;
            }
        }

        // 创建新对话
        AIConversation conversation = AIConversation.create(userId);
        save(conversation);
        return conversation;
    }

    /**
     * 获取对话
     *
     * @param conversationId 对话ID
     * @return 对话对象，如果不存在返回null
     */
    public AIConversation get(String conversationId) {
        try {
            String key = getKey(conversationId);
            Object value = redisService.getRaw(key);
            if (value == null) {
                return null;
            }
            String json = value instanceof String ? (String) value : objectMapper.writeValueAsString(value);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, AIConversation.class);
        } catch (Exception e) {
            log.error("获取对话失败，conversationId: {}", conversationId, e);
            return null;
        }
    }

    /**
     * 保存对话
     *
     * @param conversation 对话对象
     */
    public void save(AIConversation conversation) {
        try {
            String key = getKey(conversation.getConversationId());
            String json = objectMapper.writeValueAsString(conversation);
            redisService.set(key, json, CONVERSATION_TTL);
            log.debug("保存对话成功，conversationId: {}", conversation.getConversationId());
        } catch (Exception e) {
            log.error("保存对话失败，conversationId: {}", conversation.getConversationId(), e);
            throw new com.lvxin.base.exception.BusinessException(
                    com.lvxin.base.exception.ErrorCode.INTERNAL_ERROR,
                    "保存对话失败"
            );
        }
    }

    /**
     * 删除对话
     *
     * @param conversationId 对话ID
     */
    public void delete(String conversationId) {
        try {
            String key = getKey(conversationId);
            redisService.delete(key);
            log.debug("删除对话成功，conversationId: {}", conversationId);
        } catch (Exception e) {
            log.error("删除对话失败，conversationId: {}", conversationId, e);
        }
    }

    /**
     * 获取Redis key
     */
    private String getKey(String conversationId) {
        return CONVERSATION_KEY_PREFIX + conversationId;
    }
}
