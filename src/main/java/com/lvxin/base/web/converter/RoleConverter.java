package com.lvxin.base.web.converter;

import com.lvxin.base.constant.RoleStatus;
import com.lvxin.base.repository.entity.RoleDO;
import com.lvxin.base.service.bo.RoleBO;
import com.lvxin.base.util.JsonUtil;
import com.lvxin.base.web.req.CreateRoleReq;
import com.lvxin.base.web.req.UpdateRoleReq;
import com.lvxin.base.web.vo.RoleVO;

import java.util.List;

/**
 * 角色转换器（全静态，无Bean注入）
 */
public final class RoleConverter {

    private RoleConverter() {
    }

    /**
     * BO转DO
     */
    public static RoleDO boToDo(RoleBO roleBO) {
        RoleDO roleDO = new RoleDO();
        roleDO.setRoleName(roleBO.getRoleName());
        roleDO.setRoleCode(roleBO.getRoleCode());
        roleDO.setDescription(roleBO.getDescription());
        roleDO.setStatus(roleBO.getStatus() == null ? null : roleBO.getStatus().getCode());

        if (roleBO.getPermissions() != null) {
            roleDO.setPermissions(JsonUtil.toJson(roleBO.getPermissions()));
        }
        return roleDO;
    }

    /**
     * DO转BO
     */
    public static RoleBO doToBo(RoleDO roleDO) {
        RoleBO roleBO = new RoleBO();
        roleBO.setId(roleDO.getId());
        roleBO.setRoleName(roleDO.getRoleName());
        roleBO.setRoleCode(roleDO.getRoleCode());
        roleBO.setDescription(roleDO.getDescription());
        roleBO.setStatus(RoleStatus.fromCode(roleDO.getStatus()));
        roleBO.setCreatedAt(roleDO.getCreatedAt());
        roleBO.setUpdatedAt(roleDO.getUpdatedAt());

        String permissionsJson = roleDO.getPermissions();
        List<String> permissions = permissionsJson == null ? List.of() : JsonUtil.toList(permissionsJson, String.class);
        roleBO.setPermissions(permissions);

        return roleBO;
    }

    /**
     * 从BO更新DO
     */
    public static void updateDoFromBo(RoleDO roleDO, RoleBO roleBO) {
        if (roleBO.getRoleName() != null) {
            roleDO.setRoleName(roleBO.getRoleName());
        }
        if (roleBO.getRoleCode() != null) {
            roleDO.setRoleCode(roleBO.getRoleCode());
        }
        if (roleBO.getDescription() != null) {
            roleDO.setDescription(roleBO.getDescription());
        }
        if (roleBO.getStatus() != null) {
            roleDO.setStatus(roleBO.getStatus().getCode());
        }

        if (roleBO.getPermissions() != null) {
            roleDO.setPermissions(JsonUtil.toJson(roleBO.getPermissions()));
        }
    }

    /**
     * 创建请求转BO
     */
    public static RoleBO reqToBo(CreateRoleReq req) {
        RoleBO roleBO = new RoleBO();
        roleBO.setRoleName(req.getRoleName());
        roleBO.setRoleCode(req.getRoleCode());
        roleBO.setDescription(req.getDescription());
        roleBO.setPermissions(req.getPermissions());
        roleBO.setStatus(req.getStatus() != null ? RoleStatus.fromCode(req.getStatus()) : null);
        return roleBO;
    }

    /**
     * 更新请求转BO
     */
    public static RoleBO reqToBo(UpdateRoleReq req) {
        RoleBO roleBO = new RoleBO();
        roleBO.setRoleName(req.getRoleName());
        roleBO.setRoleCode(req.getRoleCode());
        roleBO.setDescription(req.getDescription());
        roleBO.setPermissions(req.getPermissions());
        roleBO.setStatus(req.getStatus() != null ? RoleStatus.fromCode(req.getStatus()) : null);
        return roleBO;
    }

    /**
     * BO转VO
     */
    public static RoleVO boToVo(RoleBO roleBO) {
        if (roleBO == null) return null;

        RoleVO roleVO = new RoleVO();
        roleVO.setId(roleBO.getId());
        roleVO.setRoleName(roleBO.getRoleName());
        roleVO.setRoleCode(roleBO.getRoleCode());
        roleVO.setDescription(roleBO.getDescription());
        roleVO.setPermissions(roleBO.getPermissions());
        roleVO.setStatus(roleBO.getStatus() != null ? roleBO.getStatus().getCode() : null);
        roleVO.setCreatedAt(roleBO.getCreatedAt() != null ? com.lvxin.base.util.DateUtil.format(roleBO.getCreatedAt()) : null);
        roleVO.setUpdatedAt(roleBO.getUpdatedAt() != null ? com.lvxin.base.util.DateUtil.format(roleBO.getUpdatedAt()) : null);
        return roleVO;
    }
}
