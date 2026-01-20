package com.lvxin.base.ai.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 计划表生成事件
 * 用于解耦计划生成请求和异步处理逻辑
 *
 * @author lvxin
 */
@Getter
public class ItineraryGenerationEvent extends ApplicationEvent {
    
    private final String itineraryKey;
    private final String planMarkdown;
    private final Long userId;
    
    /**
     * @param source 事件源
     * @param itineraryKey 计划表Key
     * @param planMarkdown 计划Markdown内容
     * @param userId 用户ID
     */
    public ItineraryGenerationEvent(Object source, String itineraryKey, String planMarkdown, Long userId) {
        super(source);
        this.itineraryKey = itineraryKey;
        this.planMarkdown = planMarkdown;
        this.userId = userId;
    }
}
