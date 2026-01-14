package com.lvxin.base.web.req;

import com.lvxin.base.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询角色请求对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryRoleReq extends PageReq {
    private String roleName;
}
