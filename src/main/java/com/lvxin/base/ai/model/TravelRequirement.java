package com.lvxin.base.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 旅行需求模型
 * 存储用户收集的旅行需求信息
 *
 * @author lvxin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelRequirement {
    /**
     * 出发地（城市名称，如：北京、上海）
     */
    private String departureCity;
    
    /**
     * 目的地列表
     */
    @Builder.Default
    private List<String> destinations = new ArrayList<>();

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 旅行天数（如果只提供了天数，没有具体日期）
     */
    private Integer days;

    /**
     * 预算范围（如：5000-10000元）
     */
    private String budget;

    /**
     * 旅行偏好列表（如：美食、自然风光、历史文化、购物等）
     */
    @Builder.Default
    private List<String> preferences = new ArrayList<>();

    /**
     * 出行人数
     */
    private Integer travelers;

    /**
     * 特殊需求（如：带老人、带小孩、无障碍设施等）
     */
    private String specialRequirements;

    /**
     * 其他扩展信息
     */
    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();

    /**
     * 检查需求是否收集完整
     */
    public boolean isComplete() {
        return departureCity != null && !departureCity.isEmpty()
                && !destinations.isEmpty()
                && (startDate != null || days != null)
                && budget != null
                && !preferences.isEmpty()
                && travelers != null;
    }

    /**
     * 获取需求摘要（用于Prompt）
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (departureCity != null && !departureCity.isEmpty()) {
            sb.append("出发地：").append(departureCity).append("\n");
        }
        if (!destinations.isEmpty()) {
            sb.append("目的地：").append(String.join("、", destinations)).append("\n");
        }
        if (startDate != null && endDate != null) {
            sb.append("时间：").append(startDate).append(" 到 ").append(endDate);
            sb.append("（共").append(days != null ? days : calculateDays()).append("天）").append("\n");
        } else if (days != null) {
            sb.append("天数：").append(days).append("天\n");
        }
        if (budget != null && !budget.isEmpty()) {
            sb.append("预算：").append(budget).append("\n");
        }
        if (!preferences.isEmpty()) {
            sb.append("偏好：").append(String.join("、", preferences)).append("\n");
        }
        if (travelers != null) {
            sb.append("人数：").append(travelers).append("人\n");
        }
        if (specialRequirements != null && !specialRequirements.isEmpty()) {
            sb.append("特殊需求：").append(specialRequirements).append("\n");
        }
        return sb.toString();
    }

    private int calculateDays() {
        if (startDate != null && endDate != null) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return days != null ? days : 0;
    }
}
