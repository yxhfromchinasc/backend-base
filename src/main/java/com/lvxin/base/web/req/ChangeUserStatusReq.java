package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户状态请求
 */
@Data
public class ChangeUserStatusReq {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 用户状态（1-正常，0-禁用，-1-锁定）
     */
    @NotNull(message = "用户状态不能为空")
    private Integer status;
}
