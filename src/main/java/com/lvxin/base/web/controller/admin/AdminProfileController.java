package com.lvxin.base.web.controller.admin;

import com.lvxin.base.common.Result;
import com.lvxin.base.service.UserService;
import com.lvxin.base.service.bo.UserOnlineInfoBO;
import com.lvxin.base.util.CurrentUserUtil;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.resp.UserInfoResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 - Web层
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
@Tag(name = "管理端-个人信息管理", description = "个人信息")
public class AdminProfileController {

    @Resource
    private UserService userService;

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据JWT Token获取当前登录用户的详细信息")
    @GetMapping("/info")
    @AuthCheck
    public Result<UserInfoResp> getUserInfo() {
        UserOnlineInfoBO userInfo = CurrentUserUtil.getCurrentUser();

        UserInfoResp resp = new UserInfoResp();
        resp.setId(userInfo.getId());
        resp.setUsername(userInfo.getUsername());
        resp.setPhone(userInfo.getPhone());
        resp.setEmail(userInfo.getEmail());
        resp.setNickname(userInfo.getNickname());
        resp.setAvatarUrl(userInfo.getAvatarUrl());
        resp.setRoles(userInfo.getRoles());
        resp.setPermissions(userInfo.getPermissionCodeSet() != null ?
                userInfo.getPermissionCodeSet().stream().toList() : List.of());
        resp.setStatus(userInfo.getStatus() != null ? userInfo.getStatus().getCode() : null);
        resp.setCreatedAt(userInfo.getCreatedAt() != null ? com.lvxin.base.util.DateUtil.format(userInfo.getCreatedAt()) : null);
        resp.setUpdatedAt(userInfo.getUpdatedAt() != null ? com.lvxin.base.util.DateUtil.format(userInfo.getUpdatedAt()) : null);

        return Result.success(resp);
    }

    /**
     * 获取个人资料
     */
    @Operation(summary = "获取个人资料")
    @GetMapping("/profile")
    @AuthCheck
    public Result<UserInfoResp> getProfile() {
        return getUserInfo();
    }

    /**
     * 更新个人资料（不允许修改手机号）
     */
    @Operation(summary = "更新个人资料")
    @PostMapping("/profile/update")
    @AuthCheck
    public Result<Void> updateProfile(@RequestBody java.util.Map<String, String> body) {
        UserOnlineInfoBO current = CurrentUserUtil.getCurrentUser();
        String nickname = body.get("nickName");
        String email = body.get("email");
        String gender = body.get("sex");
        userService.updateUserProfile(current.getId(), nickname, email, gender);
        return Result.success();
    }

    /**
     * 修改个人密码（需要旧密码）
     */
    @Operation(summary = "修改个人密码")
    @PostMapping("/profile/updatePwd")
    @AuthCheck
    public Result<Void> updatePwd(@RequestBody java.util.Map<String, String> body) {
        UserOnlineInfoBO current = CurrentUserUtil.getCurrentUser();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        userService.updatePasswordWithOld(current.getId(), oldPassword, newPassword);
        return Result.success();
    }

    /**
     * 更新头像（接收已上传后的URL；如需直传可改为Multipart）
     */
    @Operation(summary = "更新头像")
    @PostMapping("/profile/avatar")
    @AuthCheck
    public Result<Void> updateAvatar(@RequestBody java.util.Map<String, String> body) {
        UserOnlineInfoBO current = CurrentUserUtil.getCurrentUser();
        String avatarUrl = body.get("avatarUrl");
        userService.updateAvatar(current.getId(), avatarUrl);
        return Result.success();
    }
}
