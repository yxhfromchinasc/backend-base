package com.lvxin.base.web.controller.admin;

import com.lvxin.base.common.Result;
import com.lvxin.base.service.PermissionService;
import com.lvxin.base.service.bo.PermissionBO;
import com.lvxin.base.web.req.AddPermissionReq;
import com.lvxin.base.web.req.UpdatePermissionReq;
import com.lvxin.base.web.req.QueryPermissionReq;
import com.lvxin.base.web.resp.PermissionDetailResp;
import com.lvxin.base.web.resp.PermissionListResp;
import com.lvxin.base.web.annotation.AuthCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/permission")
@Tag(name = "管理端-权限管理", description = "权限创建、更新、查询等接口")
public class AdminPermissionController {

    @Resource
    private PermissionService permissionService;

    /**
     * 创建权限
     */
    @Operation(summary = "创建权限", description = "创建新的权限点")
    @PostMapping("/add")
    @AuthCheck("permission")
    public Result<PermissionDetailResp> createPermission(@Valid @RequestBody AddPermissionReq req) {
        log.info("创建权限: {}", req.getPermissionCode());

        PermissionBO permissionBO = new PermissionBO();
        permissionBO.setPermissionName(req.getPermissionName());
        permissionBO.setPermissionCode(req.getPermissionCode());
        permissionBO.setDescription(req.getDescription());

        PermissionBO createdPermission = permissionService.createPermission(permissionBO);
        PermissionDetailResp resp = convertToDetailResp(createdPermission);

        return Result.success(resp);
    }

    /**
     * 更新权限
     */
    @Operation(summary = "更新权限", description = "更新指定权限的信息")
    @PutMapping("/update")
    @AuthCheck("permission")
    public Result<PermissionDetailResp> updatePermission(@Valid @RequestBody UpdatePermissionReq req) {
        log.info("更新权限: {}", req.getPermissionCode());

        PermissionBO permissionBO = new PermissionBO();
        permissionBO.setId(req.getId());
        permissionBO.setPermissionName(req.getPermissionName());
        permissionBO.setPermissionCode(req.getPermissionCode());
        permissionBO.setDescription(req.getDescription());

        PermissionBO updatedPermission = permissionService.updatePermission(permissionBO);
        PermissionDetailResp resp = convertToDetailResp(updatedPermission);

        return Result.success(resp);
    }

    /**
     * 获取权限详情
     */
    @Operation(summary = "获取权限详情", description = "根据权限编码获取权限详细信息")
    @GetMapping("/detail")
    @AuthCheck("permission")
    public Result<PermissionDetailResp> getPermission(@Parameter(description = "权限编码", required = true) @RequestParam("permissionCode") String permissionCode) {
        PermissionBO permissionBO = permissionService.getPermissionByCode(permissionCode);
        if (permissionBO == null) {
            return Result.error("权限不存在");
        }

        PermissionDetailResp resp = convertToDetailResp(permissionBO);
        return Result.success(resp);
    }

    /**
     * 获取所有权限
     */
    @Operation(summary = "获取所有权限", description = "获取系统中所有可用的权限列表")
    @GetMapping
    @AuthCheck("permission")
    public Result<List<PermissionListResp>> getAllPermissions(@Valid QueryPermissionReq queryReq) {
        List<PermissionBO> permissions = permissionService.getAllPermissions(queryReq);
        List<PermissionListResp> respList = permissions.stream()
                .map(this::convertToListResp)
                .collect(Collectors.toList());
        return Result.success(respList);
    }

    /**
     * 删除权限（按编码）
     */
    @Operation(summary = "删除权限", description = "根据权限编码删除权限")
    @PostMapping("/delete")
    @AuthCheck("permission")
    public Result<Void> deletePermission(@Parameter(description = "权限编码", required = true) @RequestParam("permissionCode") String permissionCode) {
        log.info("删除权限: {}", permissionCode);
        permissionService.deleteByCode(permissionCode);
        return Result.success();
    }

    /**
     * 将PermissionBO转换为PermissionDetailResp
     */
    private PermissionDetailResp convertToDetailResp(PermissionBO permissionBO) {
        if (permissionBO == null) {
            return null;
        }

        PermissionDetailResp resp = new PermissionDetailResp();
        resp.setId(permissionBO.getId());
        resp.setPermissionName(permissionBO.getPermissionName());
        resp.setPermissionCode(permissionBO.getPermissionCode());
        resp.setDescription(permissionBO.getDescription());
        resp.setCreatedAt(permissionBO.getCreatedAt() != null ?
                com.lvxin.base.util.DateUtil.format(permissionBO.getCreatedAt()) : null);
        resp.setUpdatedAt(permissionBO.getUpdatedAt() != null ?
                com.lvxin.base.util.DateUtil.format(permissionBO.getUpdatedAt()) : null);

        return resp;
    }

    /**
     * 将PermissionBO转换为PermissionListResp
     */
    private PermissionListResp convertToListResp(PermissionBO permissionBO) {
        if (permissionBO == null) {
            return null;
        }

        PermissionListResp resp = new PermissionListResp();
        resp.setId(permissionBO.getId());
        resp.setPermissionName(permissionBO.getPermissionName());
        resp.setPermissionCode(permissionBO.getPermissionCode());
        resp.setDescription(permissionBO.getDescription());
        resp.setCreatedAt(permissionBO.getCreatedAt() != null ?
                com.lvxin.base.util.DateUtil.format(permissionBO.getCreatedAt()) : null);
        resp.setUpdatedAt(permissionBO.getUpdatedAt() != null ?
                com.lvxin.base.util.DateUtil.format(permissionBO.getUpdatedAt()) : null);

        return resp;
    }
}
