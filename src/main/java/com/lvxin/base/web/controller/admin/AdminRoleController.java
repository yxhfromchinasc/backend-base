package com.lvxin.base.web.controller.admin;

import com.lvxin.base.common.PageResult;
import com.lvxin.base.common.Result;
import com.lvxin.base.service.RoleService;
import com.lvxin.base.service.bo.RoleBO;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.req.CreateRoleReq;
import com.lvxin.base.web.req.QueryRoleReq;
import com.lvxin.base.web.req.UpdateRoleReq;
import com.lvxin.base.web.req.AssignPermissionsReq;
import com.lvxin.base.web.converter.RoleConverter;
import com.lvxin.base.web.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/role")
@Tag(name = "管理端-角色管理", description = "角色创建、更新、删除、查询等接口")
public class AdminRoleController {

    @Resource
    private RoleService roleService;

    /**
     * 创建角色
     */
    @Operation(summary = "创建角色", description = "创建新的角色，包含角色名称、编码、描述等信息")
    @PostMapping("/create")
    @AuthCheck("role")
    public Result<RoleVO> createRole(@RequestBody CreateRoleReq req) {
        log.info("创建角色请求: {}", req);

        RoleBO roleBOParam = RoleConverter.reqToBo(req);
        RoleBO roleBO = roleService.createRole(roleBOParam);

        return Result.success(RoleConverter.boToVo(roleBO));
    }

    /**
     * 更新角色
     */
    @Operation(summary = "更新角色", description = "更新指定角色的信息")
    @PostMapping("/update")
    @AuthCheck("role")
    public Result<RoleVO> updateRole(@RequestBody UpdateRoleReq req) {

        RoleBO roleBOParam = RoleConverter.reqToBo(req);
        RoleBO roleBO = roleService.updateRole(req.getRoleId(), roleBOParam);

        return Result.success(RoleConverter.boToVo(roleBO));
    }

    /**
     * 删除角色
     */
    @Operation(summary = "删除角色", description = "删除指定角色")
    @PostMapping("/delete")
    @AuthCheck("role")
    public Result<Void> deleteRole(@Parameter(description = "角色编码", required = true) @RequestParam("roleCode") String roleCode) {
        roleService.deleteRole(roleCode);
        return Result.success();
    }

    /**
     * 获取角色详情
     */
    @Operation(summary = "获取角色详情", description = "根据角色ID获取角色详细信息")
    @GetMapping("/detail")
    @AuthCheck("role")
    public Result<RoleVO> getRole(@Parameter(description = "角色编码", required = true) @RequestParam("roleCode") String roleCode) {
        RoleBO roleBO = roleService.getRoleByCode(roleCode);
        if (roleBO == null) {
            return Result.error("角色不存在");
        }

        return Result.success(RoleConverter.boToVo(roleBO));
    }

    /**
     * 分页查询角色
     */
    @Operation(summary = "分页查询角色", description = "根据条件分页查询角色列表")
    @PostMapping("/query")
    @AuthCheck("role")
    public Result<PageResult<RoleVO>> getRoleList(@RequestBody QueryRoleReq req) {
        PageResult<RoleBO> resultBO = roleService.getRoleList(req.getPageNumInt(), req.getPageSizeInt(), req.getRoleName());
        PageResult<RoleVO> pageResult = new PageResult<>();
        pageResult.setList(resultBO.getList().stream().map(RoleConverter::boToVo).toList());
        pageResult.setTotal(resultBO.getTotal());
        pageResult.setPage(resultBO.getPage());
        pageResult.setPageSize(resultBO.getPageSize());
        return Result.success(pageResult);
    }

    /**
     * 获取所有角色
     */
    @Operation(summary = "获取所有角色", description = "获取系统中所有可用的角色列表")
    @GetMapping("/all")
    @AuthCheck("role")
    public Result<List<RoleVO>> getAllRoles() {
        List<RoleBO> roles = roleService.getAllRoles();
        return Result.success(roles.stream().map(RoleConverter::boToVo).toList());
    }

    /**
     * 分配权限给角色
     */
    @Operation(summary = "分配权限给角色", description = "为指定角色分配权限列表")
    @PostMapping("/permissions")
    @AuthCheck("role")
    public Result<Void> assignPermissions(@RequestBody AssignPermissionsReq req) {

        roleService.assignPermissionsToRole(req.getRoleCode(), req.getPermissions());

        return Result.success();
    }

    /**
     * 获取角色权限
     */
    @Operation(summary = "获取角色权限", description = "获取指定角色的权限列表")
    @GetMapping("/permissions")
    @AuthCheck("role")
    public Result<List<String>> getRolePermissions(@Parameter(description = "角色编码", required = true) @RequestParam("roleCode") String roleCode) {
        List<String> permissions = roleService.getRolePermissions(roleCode);
        return Result.success(permissions);
    }

    /**
     * 启用角色
     */
    @Operation(summary = "启用角色", description = "启用指定角色")
    @PostMapping("/enable")
    @AuthCheck("role")
    public Result<Void> enableRole(@Parameter(description = "角色编码", required = true) @RequestParam("roleCode") String roleCode) {
        roleService.enableRole(roleCode);
        return Result.success();
    }

    /**
     * 禁用角色
     */
    @Operation(summary = "禁用角色", description = "禁用指定角色（超级管理员角色不可禁用）")
    @PostMapping("/disable")
    @AuthCheck("role")
    public Result<Void> disableRole(@Parameter(description = "角色编码", required = true) @RequestParam("roleCode") String roleCode) {
        roleService.disableRole(roleCode);
        return Result.success();
    }
}
