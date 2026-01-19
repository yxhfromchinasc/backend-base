package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI对话请求
 *
 * @author lvxin
 */
@Data
public class AIChatReq {
    /**
     * 对话ID（可选，首次对话时为空）
     */
    private String conversationId;

    /**
     * 用户消息（必填）
     */
    @NotBlank(message = "用户消息不能为空")
    private String message;
}
