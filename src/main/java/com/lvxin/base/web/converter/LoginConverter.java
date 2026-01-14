package com.lvxin.base.web.converter;

import com.lvxin.base.service.bo.LoginRequestBO;
import com.lvxin.base.service.bo.LoginResponseBO;
import com.lvxin.base.web.req.LoginReq;
import com.lvxin.base.web.resp.LoginResp;

/**
 * 登录转换器（全静态，无Bean注入）
 */
public final class LoginConverter {

    private LoginConverter() {
    }

    /**
     * Req转BO
     */
    public static LoginRequestBO reqToBo(LoginReq req) {
        if (req == null) {
            return null;
        }

        LoginRequestBO bo = new LoginRequestBO();
        bo.setLoginMethod(req.getLoginMethod() != null ? Integer.valueOf(req.getLoginMethod()) : null);
        bo.setPhone(req.getPhone());
        bo.setPassword(req.getPassword());
        bo.setVerificationCode(req.getVerificationCode());
        bo.setIpAddress(req.getIpAddress());
        bo.setUserAgent(req.getUserAgent());
        bo.setDeviceInfo(req.getDeviceInfo());

        return bo;
    }

    /**
     * BO转Resp
     */
    public static LoginResp boToResp(LoginResponseBO bo) {
        if (bo == null) {
            return null;
        }

        LoginResp resp = new LoginResp();
        resp.setSuccess(bo.getSuccess());
        resp.setUserId(bo.getUserId());
        resp.setUsername(bo.getUsername());
        resp.setPhone(bo.getPhone());
        resp.setAccessToken(bo.getAccessToken());
        resp.setLoginStatus(String.valueOf(bo.getLoginStatus()));
        resp.setNeedBindPhone(bo.getNeedBindPhone());
        resp.setErrorMessage(bo.getErrorMessage());

        // 设置用户信息
        if (bo.getUserInfo() != null) {
            resp.setNickname(bo.getUserInfo().getNickname());
            resp.setEmail(bo.getUserInfo().getEmail());
        }

        // 设置时间信息
        if (bo.getTokenExpireTime() != null) {
            resp.setExpireTime(String.valueOf(bo.getTokenExpireTime()));
        }

        return resp;
    }
}
