package com.lvxin.base.web.converter;

import com.lvxin.base.repository.entity.PermissionDO;
import com.lvxin.base.service.bo.PermissionBO;

/**
 * 权限转换器（全静态，无Bean注入）
 */
public final class PermissionConverter {

    private PermissionConverter() {
    }

    /**
     * DO转BO
     */
    public static PermissionBO doToBo(PermissionDO permissionDO) {
        if (permissionDO == null) {
            return null;
        }

        PermissionBO permissionBO = new PermissionBO();
        permissionBO.setId(permissionDO.getId());
        permissionBO.setPermissionName(permissionDO.getPermissionName());
        permissionBO.setPermissionCode(permissionDO.getPermissionCode());
        permissionBO.setDescription(permissionDO.getDescription());
        permissionBO.setCreatedAt(permissionDO.getCreatedAt());
        permissionBO.setUpdatedAt(permissionDO.getUpdatedAt());

        return permissionBO;
    }

    /**
     * BO转DO
     */
    public static PermissionDO boToDo(PermissionBO permissionBO) {
        if (permissionBO == null) {
            return null;
        }

        PermissionDO permissionDO = new PermissionDO();
        permissionDO.setId(permissionBO.getId());
        permissionDO.setPermissionName(permissionBO.getPermissionName());
        permissionDO.setPermissionCode(permissionBO.getPermissionCode());
        permissionDO.setDescription(permissionBO.getDescription());
        permissionDO.setCreatedAt(permissionBO.getCreatedAt());
        permissionDO.setUpdatedAt(permissionBO.getUpdatedAt());

        return permissionDO;
    }
}
