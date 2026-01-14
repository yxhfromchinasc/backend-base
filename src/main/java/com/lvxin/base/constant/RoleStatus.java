package com.lvxin.base.constant;

import lombok.Getter;

/**
 * 角色状态枚举（BO 层使用枚举，其它层使用 code）
 */
@Getter
public enum RoleStatus {
    ENABLED(1, "启用"),
    DISABLED(0, "禁用");

    private final int code;
    private final String desc;

    RoleStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RoleStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RoleStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
