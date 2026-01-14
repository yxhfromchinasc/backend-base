package com.lvxin.base.web.resp;

import lombok.Data;

/**
 * 绑定手机号响应对象
 */
@Data
public class BindPhoneResp {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 绑定的手机号
     */
    private String phone;

    /**
     * 绑定时间
     */
    private String bindTime;
}
