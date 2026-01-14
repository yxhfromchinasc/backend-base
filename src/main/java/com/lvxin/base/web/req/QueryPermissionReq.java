package com.lvxin.base.web.req;

import lombok.Data;

/**
 * 查询权限请求
 */
@Data
public class QueryPermissionReq {

    /**
     * 权限名称（模糊查询）
     */
    private String permissionName;

    /**
     * 权限编码（模糊查询）
     */
    private String permissionCode;
}
