package com.lvxin.base.constant;

/**
 * 计划表状态枚举
 * 用于标记计划表的生成状态
 *
 * @author lvxin
 */
public enum ItineraryStatus {
    /**
     * 正常状态：计划已生成完成，可以正常使用
     */
    NORMAL("正常"),

    /**
     * 生成中：计划正在后台生成，用户可以看到但无法编辑
     */
    GENERATING("生成中"),

    /**
     * 生成失败：计划生成失败，需要用户重新生成或查看错误信息
     */
    FAILED("生成失败");

    private final String description;

    ItineraryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据字符串获取状态枚举
     *
     * @param status 状态字符串
     * @return 状态枚举，如果不存在则返回NORMAL
     */
    public static ItineraryStatus fromString(String status) {
        if (status == null || status.isEmpty()) {
            return NORMAL;
        }
        try {
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
