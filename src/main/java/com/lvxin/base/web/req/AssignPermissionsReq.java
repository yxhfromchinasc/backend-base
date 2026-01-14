package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配权限请求对象
 */
@Data
public class AssignPermissionsReq {

    @NotNull(message = "角色编码不能为空")
    private String roleCode;

    @NotEmpty(message = "权限列表不能为空")
    private List<String> permissions;

}
