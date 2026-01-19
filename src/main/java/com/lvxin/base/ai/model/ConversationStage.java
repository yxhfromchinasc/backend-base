package com.lvxin.base.ai.model;

/**
 * 对话阶段枚举
 * 定义AI规划对话的不同阶段
 *
 * @author lvxin
 */
public enum ConversationStage {
    /**
     * 需求收集阶段：收集用户的旅行需求（时间、地点、偏好等）
     */
    REQUIREMENT_COLLECTING("需求收集中"),

    /**
     * 计划生成阶段：根据需求生成和调优旅行计划
     */
    PLAN_GENERATING("计划生成中"),

    /**
     * 计划就绪：计划已生成，等待用户确认转换为计划表
     */
    PLAN_READY("计划已就绪");

    private final String description;

    ConversationStage(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
