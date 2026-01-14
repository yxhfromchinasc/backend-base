package com.lvxin.base.web.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户在线状态响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOnlineStatusResp {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否在线
     */
    private Boolean isOnline;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

}
