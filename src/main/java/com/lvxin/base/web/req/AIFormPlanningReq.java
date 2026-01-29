package com.lvxin.base.web.req;

import com.lvxin.base.ai.model.TravelRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 表单式AI规划请求
 */
@Data
@Schema(description = "表单式AI规划请求")
public class AIFormPlanningReq {

    @Schema(description = "对话ID（可选，如果传入则在现有对话中更新）")
    private String conversationId;

    @NotNull(message = "旅行需求不能为空")
    @Schema(description = "旅行需求表单数据")
    private TravelRequirement requirement;
}
