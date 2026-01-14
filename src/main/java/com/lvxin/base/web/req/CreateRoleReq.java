package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建角色请求对象
 */
@Data
public class CreateRoleReq {

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    private String description;

    private List<String> permissions;

    @NotNull(message = "状态不能为空")
    private Integer status;

}
