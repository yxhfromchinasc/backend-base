package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配用户角色请求
 */
@Data
public class AssignUserRolesReq {

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 角色列表
     */
    @NotNull(message = "角色列表不能为空")
    private List<String> roles;
}
