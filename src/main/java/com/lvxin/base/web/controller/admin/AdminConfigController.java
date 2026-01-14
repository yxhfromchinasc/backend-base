package com.lvxin.base.web.controller.admin;

import com.lvxin.base.common.Result;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.service.SystemConfigService;
import com.lvxin.base.service.bo.SystemConfigBO;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.req.SetConfigReq;
import com.lvxin.base.web.req.UpdateConfigValueReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端-系统配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/config")
@Tag(name = "管理端-系统配置管理", description = "系统配置管理相关接口")
public class AdminConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @Operation(summary = "获取所有配置", description = "获取所有系统配置（管理员）")
    @GetMapping("/list")
    @AuthCheck("config")
    public Result<List<SystemConfigBO>> getAllConfigs() {
        try {
            List<SystemConfigBO> configs = systemConfigService.getAllConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取系统配置列表系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 根据配置键获取配置
     */
    @Operation(summary = "获取配置详情", description = "根据配置键获取配置详情")
    @GetMapping("/detail")
    @AuthCheck("config")
    public Result<SystemConfigBO> getConfig(@RequestParam String configKey) {
        try {
            SystemConfigBO config = systemConfigService.getConfig(configKey);
            if (config == null) {
                return Result.error("配置不存在");
            }
            return Result.success(config);
        } catch (BusinessException e) {
            log.warn("获取配置详情业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取配置详情系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 设置配置
     */
    @Operation(summary = "设置配置", description = "创建或更新系统配置")
    @PostMapping("/set")
    @AuthCheck("config")
    public Result<SystemConfigBO> setConfig(@Valid @RequestBody SetConfigReq req) {
        try {
            SystemConfigBO config = systemConfigService.setConfig(
                    req.getConfigKey(),
                    req.getConfigValue(),
                    req.getConfigType(),
                    req.getDescription(),
                    req.getIsPublic()
            );
            
            return Result.success(config);
        } catch (BusinessException e) {
            log.warn("设置配置业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("设置配置系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 更新配置值
     */
    @Operation(summary = "更新配置值", description = "更新配置的值")
    @PostMapping("/update-value")
    @AuthCheck("config")
    public Result<SystemConfigBO> updateConfigValue(@Valid @RequestBody UpdateConfigValueReq req) {
        try {
            SystemConfigBO config = systemConfigService.updateConfigValue(req.getConfigKey(), req.getConfigValue());
            
            return Result.success(config);
        } catch (BusinessException e) {
            log.warn("更新配置值业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新配置值系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 删除配置
     */
    @Operation(summary = "删除配置", description = "根据配置键删除配置")
    @PostMapping("/delete")
    @AuthCheck("config")
    public Result<Void> deleteConfig(@RequestParam String configKey) {
        try {
            systemConfigService.deleteConfig(configKey);
            return Result.success();
        } catch (BusinessException e) {
            log.warn("删除配置业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除配置系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

}

