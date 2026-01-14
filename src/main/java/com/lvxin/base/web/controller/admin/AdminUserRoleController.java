package com.lvxin.base.web.controller.admin;

import com.lvxin.base.common.Result;
import com.lvxin.base.service.UserService;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.req.AssignUserRolesReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/user-role")
@Tag(name = "管理端-用户角色管理", description = "用户角色分配、查询等接口")
public class AdminUserRoleController {

    @Resource
    private UserService userService;

    /**
     * 为用户分配角色
     */
    @Operation(summary = "为用户分配角色", description = "为指定用户分配角色列表")
    @AuthCheck("user")
    @PostMapping("/roles4UserId")
    public Result<Void> assignRolesToUser(@RequestBody AssignUserRolesReq req) {

        userService.assignRolesToUser(Long.valueOf(req.getUserId()), req.getRoles());

        return Result.success();
    }

    /**
     * 获取用户角色
     */
    @Operation(summary = "获取用户角色", description = "获取指定用户的角色列表")
    @AuthCheck("user")
    @GetMapping("/rolesByUserId")
    public Result<List<String>> getUserRoles(@Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        List<String> roles = userService.getUserRoles(userId);
        return Result.success(roles);
    }

    /**
     * 获取用户权限
     */
    @Operation(summary = "获取用户权限", description = "获取指定用户的权限列表")
    @AuthCheck("user")
    @GetMapping("/permissionsByUserId")
    public Result<List<String>> getUserPermissions(@Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        List<String> permissions = userService.getUserPermissions(userId);
        return Result.success(permissions);
    }
}
