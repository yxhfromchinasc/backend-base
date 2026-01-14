package com.lvxin.base.service;

import com.lvxin.base.service.bo.PermissionBO;
import com.lvxin.base.web.req.QueryPermissionReq;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 创建权限
     */
    PermissionBO createPermission(PermissionBO permissionBO);

    /**
     * 更新权限
     */
    PermissionBO updatePermission(PermissionBO permissionBO);

    /**
     * 根据ID获取权限
     */
    PermissionBO getPermissionById(Long permissionId);

    /**
     * 根据编码获取权限
     */
    PermissionBO getPermissionByCode(String permissionCode);

    /**
     * 获取所有权限
     */
    List<PermissionBO> getAllPermissions();

    /**
     * 根据查询条件获取权限列表
     */
    List<PermissionBO> getAllPermissions(QueryPermissionReq queryReq);

    /**
     * 根据编码删除权限
     */
    void deleteByCode(String permissionCode);
}
