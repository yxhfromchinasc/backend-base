package com.lvxin.base.constant;

import lombok.Getter;

/**
 * 用户状态枚举（BO 层使用枚举，其它层使用 code）
 */
@Getter
public enum UserStatus {
    NORMAL(1, "正常"),
    DISABLED(0, "禁用"),
    LOCKED(-1, "锁定");

    private final int code;
    private final String desc;

    UserStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
