package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI计划转换请求
 *
 * @author lvxin
 */
@Data
public class AIConvertReq {
    /**
     * 对话ID（必填）
     */
    @NotBlank(message = "对话ID不能为空")
    private String conversationId;
}
