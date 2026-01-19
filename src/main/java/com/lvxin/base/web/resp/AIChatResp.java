package com.lvxin.base.web.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI对话响应
 *
 * @author lvxin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatResp {
    /**
     * 对话ID
     */
    private String conversationId;

    /**
     * AI回复
     */
    private String reply;

    /**
     * 当前阶段（REQUIREMENT_COLLECTING、PLAN_GENERATING、PLAN_READY）
     */
    private String stage;

    /**
     * Markdown格式的计划（阶段2才有）
     */
    private String planMarkdown;

    /**
     * 是否可以转换为计划表
     */
    private Boolean canConvert;
}
