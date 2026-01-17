package com.lvxin.base.web.controller;

import com.lvxin.base.common.PageReq;
import com.lvxin.base.common.PageResult;
import com.lvxin.base.common.Result;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.service.ItineraryService;
import com.lvxin.base.service.bo.ItineraryBO;
import com.lvxin.base.util.CurrentUserUtil;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.req.UpsertItineraryReq;
import com.lvxin.base.web.resp.ItineraryDetailResp;
import com.lvxin.base.web.resp.ItinerarySummaryResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 计划表控制器
 */
@Slf4j
@RestController
@RequestMapping("/itinerary")
@Tag(name = "计划表管理", description = "计划表相关接口")
public class ItineraryController {

    @Resource
    private ItineraryService itineraryService;

    /**
     * 创建或更新计划表（Upsert）
     */
    @Operation(summary = "创建或更新计划表", description = "创建新计划表或更新现有计划表。itineraryKey为null时创建，否则更新")
    @PostMapping("/upsert")
    @AuthCheck // 需要登录
    public Result<ItineraryDetailResp> upsertItinerary(@Valid @RequestBody UpsertItineraryReq req) {
        try {
            Long userId = CurrentUserUtil.getCurrentUserId();
            
            ItineraryBO bo = itineraryService.upsertItinerary(
                    userId,
                    req.getItineraryKey(),
                    req.getTitle(),
                    req.getCoverImageUrl(),
                    req.getJsonData(),
                    req.getCurrentVersion()
            );
            
            ItineraryDetailResp resp = toDetailResp(bo);
            return Result.success(resp);
        } catch (BusinessException e) {
            log.warn("创建或更新计划表业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建或更新计划表系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取计划表详情
     */
    @Operation(summary = "获取计划表详情", description = "根据计划表Key获取计划表详情")
    @GetMapping("/detail")
    @AuthCheck // 需要登录
    public Result<ItineraryDetailResp> getItineraryDetail(@RequestParam String itineraryKey) {
        try {
            Long userId = CurrentUserUtil.getCurrentUserId();
            
            ItineraryBO bo = itineraryService.getItineraryByKeyAndUserId(itineraryKey, userId);
            
            ItineraryDetailResp resp = toDetailResp(bo);
            return Result.success(resp);
        } catch (BusinessException e) {
            log.warn("获取计划表详情业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取计划表详情系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }


    /**
     * 删除计划表
     */
    @Operation(summary = "删除计划表", description = "删除计划表（软删除）")
    @PostMapping("/delete")
    @AuthCheck // 需要登录
    public Result<Void> deleteItinerary(@RequestParam String itineraryKey) {
        try {
            Long userId = CurrentUserUtil.getCurrentUserId();
            
            itineraryService.deleteItinerary(itineraryKey, userId);
            
            return Result.success();
        } catch (BusinessException e) {
            log.warn("删除计划表业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除计划表系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取用户计划表列表（分页）
     */
    @Operation(summary = "获取计划表列表", description = "获取当前用户的计划表列表（分页）")
    @GetMapping("/list")
    @AuthCheck // 需要登录
    public Result<PageResult<ItinerarySummaryResp>> getItineraryList(@Valid PageReq pageReq) {
        try {
            Long userId = CurrentUserUtil.getCurrentUserId();
            
            PageResult<ItineraryBO> boPageResult = itineraryService.getUserItineraries(userId, pageReq);
            
            // 转换为摘要响应列表
            List<ItinerarySummaryResp> respList = boPageResult.getList().stream()
                    .map(this::toSummaryResp)
                    .collect(Collectors.toList());
            
            // 构建分页结果
            PageResult<ItinerarySummaryResp> respPageResult = new PageResult<>();
            respPageResult.setList(respList);
            respPageResult.setTotal(boPageResult.getTotal());
            respPageResult.setPage(boPageResult.getPageInt());
            respPageResult.setPageSize(boPageResult.getPageSizeInt());
            
            return Result.success(respPageResult);
        } catch (BusinessException e) {
            log.warn("获取计划表列表业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取计划表列表系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取用户计划表列表（不分页）
     */
    @Operation(summary = "获取计划表列表（全部）", description = "获取当前用户的所有计划表列表（不分页）")
    @GetMapping("/list-all")
    @AuthCheck // 需要登录
    public Result<List<ItinerarySummaryResp>> getAllItineraries() {
        try {
            Long userId = CurrentUserUtil.getCurrentUserId();
            
            List<ItineraryBO> boList = itineraryService.getUserItineraries(userId);
            
            // 转换为摘要响应列表
            List<ItinerarySummaryResp> respList = boList.stream()
                    .map(this::toSummaryResp)
                    .collect(Collectors.toList());
            
            return Result.success(respList);
        } catch (BusinessException e) {
            log.warn("获取计划表列表业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取计划表列表系统异常", e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * BO转详情响应
     */
    private ItineraryDetailResp toDetailResp(ItineraryBO bo) {
        ItineraryDetailResp resp = new ItineraryDetailResp();
        BeanUtils.copyProperties(bo, resp);
        return resp;
    }

    /**
     * BO转摘要响应
     */
    private ItinerarySummaryResp toSummaryResp(ItineraryBO bo) {
        ItinerarySummaryResp resp = new ItinerarySummaryResp();
        resp.setItineraryKey(bo.getItineraryKey());
        resp.setTitle(bo.getTitle());
        resp.setCoverImageUrl(bo.getCoverImageUrl());
        resp.setVersion(bo.getVersion());
        resp.setComponentVersion(bo.getComponentVersion());
        resp.setIsCurrentVersion(bo.getIsCurrentVersion());
        resp.setUpdatedAt(bo.getUpdatedAt());
        return resp;
    }
}
