package com.lvxin.base.web.converter;

import com.lvxin.base.constant.UserStatus;
import com.lvxin.base.repository.entity.UserDO;
import com.lvxin.base.service.bo.UserBO;
import com.lvxin.base.util.JsonUtil;

import java.util.List;

/**
 * 用户转换器（全静态，无Bean注入）
 */
public final class UserConverter {

    private UserConverter() {
    }

    /**
     * DO转BO
     */
    public static UserBO doToBo(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        
        UserBO userBO = new UserBO();
        userBO.setId(userDO.getId());
        userBO.setUsername(userDO.getUsername());
        userBO.setPhone(userDO.getPhone());
        userBO.setEmail(userDO.getEmail());
        userBO.setNickname(userDO.getNickname());
        userBO.setAvatarUrl(userDO.getAvatarUrl());
        userBO.setStatus(UserStatus.fromCode(userDO.getStatus()));
        userBO.setCreatedAt(userDO.getCreatedAt());
        userBO.setUpdatedAt(userDO.getUpdatedAt());

        String rolesJson = userDO.getRoles();
        List<String> roles = rolesJson == null ? List.of() : JsonUtil.toList(rolesJson, String.class);
        userBO.setRoles(roles);

        return userBO;
    }
}
