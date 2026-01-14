package com.lvxin.base.service;

import com.lvxin.base.common.PageResult;
import com.lvxin.base.service.bo.RoleBO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 创建角色
     */
    RoleBO createRole(RoleBO roleBO);

    /**
     * 更新角色
     */
    RoleBO updateRole(Long roleId, RoleBO roleBO);

    /**
     * 删除角色
     */
    void deleteRole(String roleCode);

    /**
     * 根据ID获取角色
     */
    RoleBO getRoleById(Long roleId);

    /**
     * 根据编码获取角色
     */
    RoleBO getRoleByCode(String roleCode);

    /**
     * 分页查询角色
     */
    PageResult<RoleBO> getRoleList(int page, int size, String keyword);

    /**
     * 获取所有角色
     */
    List<RoleBO> getAllRoles();

    /**
     * 分配权限给角色
     */
    void assignPermissionsToRole(String roleCode, List<String> permissions);

    /**
     * 获取角色的权限列表
     */
    List<String> getRolePermissions(String roleCode);

    /**
     * 启用角色
     */
    void enableRole(String roleCode);

    /**
     * 禁用角色
     */
    void disableRole(String roleCode);

}
