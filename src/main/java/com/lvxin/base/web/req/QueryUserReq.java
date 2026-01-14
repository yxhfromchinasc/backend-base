package com.lvxin.base.web.req;

import com.lvxin.base.common.PageReq;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryUserReq extends PageReq {

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 手机号（模糊查询）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱（模糊查询）
     */
    private String email;

    /**
     * 用户状态
     */
    private Integer status;
}
