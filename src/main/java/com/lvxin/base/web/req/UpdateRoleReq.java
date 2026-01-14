package com.lvxin.base.web.req;

import lombok.Data;

import java.util.List;

/**
 * 更新角色请求对象
 */
@Data
public class UpdateRoleReq {
    private Long roleId;

    private String roleName;

    private String roleCode;

    private String description;

    private List<String> permissions;

    private Integer status;

    private Integer sortOrder;

}
