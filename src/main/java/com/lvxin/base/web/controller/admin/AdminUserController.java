package com.lvxin.base.web.controller.admin;

import com.lvxin.base.common.Result;
import com.lvxin.base.common.PageResult;
import com.lvxin.base.service.UserService;
import com.lvxin.base.service.bo.UserOnlineInfoBO;
import com.lvxin.base.service.bo.UserBO;
import com.lvxin.base.web.req.UserRegisterReq;
import com.lvxin.base.web.req.QueryUserReq;
import com.lvxin.base.web.req.CreateUserReq;
import com.lvxin.base.web.req.ResetUserPwdReq;
import com.lvxin.base.web.req.ChangeUserStatusReq;
import com.lvxin.base.web.req.UpdateUserReq;
import com.lvxin.base.web.req.DeleteUserReq;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.resp.UserListResp;
import com.lvxin.base.util.CurrentUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 - Web层
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
@Tag(name = "管理端-用户管理", description = "用户注册、登录、登出等接口")
public class AdminUserController {

    @Resource
    private UserService userService;

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户", description = "创建后台管理用户")
    @PostMapping("/create")
    @AuthCheck("user")
    public Result<Long> createUser(@Valid @RequestBody CreateUserReq req) {
        Long userId = userService.registerUser(req.getPhone(), req.getUsername(), req.getPassword(), req.getEmail());
        // 分配角色（如果有）
        if (req.getRoles() != null && !req.getRoles().isEmpty()) {
            userService.assignRolesToUser(userId, req.getRoles());
        }
        return Result.success(userId);
    }

    /**
     * 用户详情
     */
    @Operation(summary = "用户详情", description = "根据ID获取用户详情")
    @GetMapping("/detail")
    @AuthCheck("user")
    public Result<UserListResp> getUserDetail(@RequestParam Long id) {
        UserBO user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        UserListResp resp = convertToUserListResp(user);
        return Result.success(resp);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "重置指定用户的登录密码")
    @PostMapping("/resetPwd")
    @AuthCheck("user")
    public Result<Void> resetPassword(@Valid @RequestBody ResetUserPwdReq req) {
        userService.resetPassword(req.getId(), req.getPassword());
        return Result.success();
    }

    /**
     * 修改用户状态
     */
    @Operation(summary = "修改用户状态", description = "启用或禁用用户")
    @PostMapping("/changeStatus")
    @AuthCheck("user")
    public Result<Void> changeStatus(@Valid @RequestBody ChangeUserStatusReq req) {
        if (req.getStatus() != null && req.getStatus() == 1) {
            userService.enableUser(req.getId());
        } else {
            userService.disableUser(req.getId());
        }
        return Result.success();
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新用户基本信息，包括用户名、昵称、邮箱、头像等")
    @PostMapping("/update")
    @AuthCheck("user")
    public Result<Void> updateUser(@Valid @RequestBody UpdateUserReq req) {
        userService.updateUser(
                req.getId(),
                req.getUsername(),
                req.getNickname(),
                req.getEmail(),
                req.getAvatarUrl()
        );
        
        // 如果提供了角色列表，则更新角色
        if (req.getRoles() != null) {
            userService.assignRolesToUser(req.getId(), req.getRoles());
        }
        
        return Result.success();
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "使用手机号、短信验证码、用户名、密码、邮箱进行用户注册")
    @PostMapping("/register")
    @AuthCheck("user")
    public Result<Long> register(@Valid @RequestBody UserRegisterReq req) {

        // 验证短信验证码
        userService.verifySmsCode(req.getPhone(), req.getSmsCode());

        // 执行用户注册
        Long userId = userService.registerUser(
                req.getPhone(),
                req.getUsername(),
                req.getPassword(),
                req.getEmail()
        );

        return Result.success(userId);
    }

    /**
     * 查询用户列表
     */
    @Operation(summary = "查询用户列表", description = "分页查询用户列表，支持按用户名、手机号、邮箱、状态筛选")
    @PostMapping("/list")
    @AuthCheck("user")
    public Result<PageResult<UserListResp>> getUserList(@Valid @RequestBody QueryUserReq req) {
        PageResult<UserBO> pageResult = userService.getUserList(req);

        // 转换为响应对象
        List<UserListResp> userList = pageResult.getList().stream()
                .map(this::convertToUserListResp)
                .collect(java.util.stream.Collectors.toList());

        PageResult<UserListResp> resp = new PageResult<>();
        resp.setList(userList);
        resp.setTotal(pageResult.getTotal());
        resp.setPage(pageResult.getPage());
        resp.setPageSize(pageResult.getPageSize());

        return Result.success(resp);
    }

    /**
     * 将UserBO转换为UserListResp
     */
    private UserListResp convertToUserListResp(UserBO userBO) {
        if (userBO == null) {
            return null;
        }

        UserListResp resp = new UserListResp();
        resp.setId(userBO.getId());
        resp.setUsername(userBO.getUsername());
        resp.setNickname(userBO.getNickname());
        resp.setPhone(userBO.getPhone());
        resp.setEmail(userBO.getEmail());
        resp.setAvatarUrl(userBO.getAvatarUrl());
        resp.setRoles(userBO.getRoles());
        resp.setStatus(userBO.getStatus() != null ? userBO.getStatus().getCode() : null);
        resp.setCreatedAt(userBO.getCreatedAt() != null ? com.lvxin.base.util.DateUtil.format(userBO.getCreatedAt()) : null);
        resp.setUpdatedAt(userBO.getUpdatedAt() != null ? com.lvxin.base.util.DateUtil.format(userBO.getUpdatedAt()) : null);

        return resp;
    }

    /**
     * 删除用户及其所有数据
     * 警告：此操作不可逆，会删除用户的所有数据，包括订单、钱包、地址等
     * 安全要求：只有SUPER_ADMIN可以调用此接口
     */
    @Operation(summary = "删除用户及其所有数据", 
               description = "删除指定用户及其在系统中的所有相关数据，包括订单、钱包、地址、邀请关系等。此操作不可逆，请谨慎使用！只有超级管理员可以执行此操作。")
    @PostMapping("/delete")
    @AuthCheck("user")
    public Result<Void> deleteUser(@Valid @RequestBody DeleteUserReq req) {
        try {
            // 获取当前操作人ID
            UserOnlineInfoBO currentUser = CurrentUserUtil.getCurrentUser();
            if (currentUser == null) {
                log.error("删除用户失败：未获取到当前登录用户信息");
                return Result.error("未授权，请先登录");
            }
            
            Long operatorId = currentUser.getId();
            
            // 参数验证
            if (req.getUserId() == null) {
                return Result.error("用户ID不能为空");
            }

            log.warn("管理员删除用户请求，目标用户ID: {}, 操作人ID: {}, 操作人手机号: {}", 
                req.getUserId(), operatorId, currentUser.getPhone());

            // 执行删除
            userService.deleteUserCompletely(req.getUserId(), operatorId);

            log.warn("管理员删除用户成功，目标用户ID: {}, 操作人ID: {}, 操作人手机号: {}", 
                req.getUserId(), operatorId, currentUser.getPhone());
            return Result.success();

        } catch (com.lvxin.base.exception.BusinessException e) {
            log.warn("删除用户业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除用户系统异常，用户ID: {}", req.getUserId(), e);
            return Result.error("删除用户失败: " + e.getMessage());
        }
    }

}
