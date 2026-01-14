package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 重置用户密码请求
 */
@Data
public class ResetUserPwdReq {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotBlank(message = "密码不能为空")
    private String password;
}
