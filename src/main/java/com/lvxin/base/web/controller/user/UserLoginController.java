package com.lvxin.base.web.controller.user;

import com.lvxin.base.common.Result;
import com.lvxin.base.constant.LoginMethod;
import com.lvxin.base.service.LoginService;
import com.lvxin.base.service.UserService;
import com.lvxin.base.service.bo.LoginRequestBO;
import com.lvxin.base.service.bo.LoginResponseBO;
import com.lvxin.base.service.bo.UserOnlineInfoBO;
import com.lvxin.base.util.CurrentUserUtil;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.converter.LoginConverter;
import com.lvxin.base.web.req.LoginReq;
import com.lvxin.base.web.resp.LoginResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端登录控制器
 * 处理用户端登录相关的API请求
 */
@Slf4j
@RestController
@RequestMapping("/user/login")
@Tag(name = "用户端-登录管理", description = "用户登录相关接口")
public class UserLoginController {
    @Resource
    private LoginService loginService;
    @Resource
    private UserService userService;

    @Operation(summary = "手机号密码登录")
    @PostMapping("/phone-password")
    public Result<LoginResp> phonePasswordLogin(@RequestBody LoginReq loginRequest) {
        LoginReq request = new LoginReq();
        request.setLoginMethod(String.valueOf(LoginMethod.PHONE_PASSWORD.getCode()));
        request.setPhone(loginRequest.getPhone());
        request.setPassword(loginRequest.getPassword());
        // 客户端信息
        request.setIpAddress(CurrentUserUtil.getClientIp());
        request.setUserAgent(CurrentUserUtil.getUserAgent());
        request.setDeviceInfo(CurrentUserUtil.getDeviceInfo());

        LoginRequestBO bo = LoginConverter.reqToBo(request);
        LoginResponseBO respBO = loginService.login(bo);
        LoginResp resp = LoginConverter.boToResp(respBO);
        if (resp.getSuccess()) {
            return Result.success(resp);
        }
        return Result.error(resp.getErrorMessage());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    @AuthCheck
    public Result<String> logout() {
        UserOnlineInfoBO currentUser = CurrentUserUtil.getCurrentUser();
        if (currentUser != null) {
            userService.forceUserOffline(currentUser.getId());
        }
        return Result.success("登出成功");
    }
}
