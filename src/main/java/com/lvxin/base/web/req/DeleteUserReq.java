package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 删除用户请求
 */
@Data
public class DeleteUserReq {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
