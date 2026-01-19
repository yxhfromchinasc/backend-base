package com.lvxin.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lvxin.base.common.PageResult;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import com.lvxin.base.repository.ItineraryRepository;
import com.lvxin.base.repository.entity.ItineraryDO;
import com.lvxin.base.service.ItineraryService;
import com.lvxin.base.service.bo.ItineraryBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 计划表服务实现类
 */
@Slf4j
@Service
public class ItineraryServiceImpl implements ItineraryService {

    @Resource
    private ItineraryRepository itineraryRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItineraryBO upsertItinerary(Long userId, String itineraryKey, String title, String coverImageUrl, String jsonData, Integer currentVersion) {
        // 参数校验
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划表标题不能为空");
        }
        if (!StringUtils.hasText(jsonData)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划表JSON数据不能为空");
        }

        // 判断是新增还是更新
        boolean isCreate = !StringUtils.hasText(itineraryKey);

        if (isCreate) {
            // ========== 新增逻辑 ==========
            log.info("创建计划表: userId={}, title={}", userId, title);

            // 生成计划表唯一标识（Key）
            String newItineraryKey = UUID.randomUUID().toString();

            // 创建计划表DO
            ItineraryDO itineraryDO = new ItineraryDO();
            itineraryDO.setItineraryKey(newItineraryKey);
            itineraryDO.setUserId(userId);
            itineraryDO.setTitle(title);
            itineraryDO.setCoverImageUrl(coverImageUrl);
            itineraryDO.setJsonData(jsonData);
            itineraryDO.setVersion(1); // 新增时版本号为1
            itineraryDO.setComponentVersion("v1"); // 默认组件版本为v1
            itineraryDO.setIsCurrentVersion(1); // 新创建的计划表默认为当前使用版本
            itineraryDO.setStatus("NORMAL"); // 默认状态为正常
            itineraryDO.setIsDeleted(0);

            // 保存到数据库
            int result = itineraryRepository.insert(itineraryDO);
            if (result <= 0) {
                log.error("创建计划表失败: userId={}, title={}", userId, title);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建计划表失败");
            }

            log.info("创建计划表成功: itineraryKey={}, userId={}", newItineraryKey, userId);

            // 转换为BO并返回
            return toBO(itineraryDO);
        } else {
            // ========== 更新逻辑 ==========
            log.info("更新计划表: itineraryKey={}, userId={}, currentVersion={}", itineraryKey, userId, currentVersion);

            // 获取当前计划表（包含权限校验）
            ItineraryBO existingItinerary = getItineraryByKeyAndUserId(itineraryKey, userId);

            // 版本校验：检查当前版本是否是最新版本
            if (existingItinerary.getIsCurrentVersion() == null || existingItinerary.getIsCurrentVersion() != 1) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "当前版本不是最新版本，请刷新后重试");
            }

            // 并发控制：校验前端传入的版本号是否与当前版本一致
            if (currentVersion != null && !currentVersion.equals(existingItinerary.getVersion())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, 
                    String.format("版本冲突：当前版本为 %d，你提交的版本为 %d，请刷新后重试", 
                        existingItinerary.getVersion(), currentVersion));
            }

            // 创建新版本：将旧版本标记为历史版本，创建新版本记录
            // 1. 将当前版本标记为历史版本
            itineraryRepository.clearCurrentVersionFlag(itineraryKey);
            
            // 2. 创建新版本记录
            ItineraryDO newVersionDO = new ItineraryDO();
            newVersionDO.setItineraryKey(itineraryKey); // 使用相同的itineraryKey作为key
            newVersionDO.setUserId(userId);
            newVersionDO.setTitle(title);
            newVersionDO.setCoverImageUrl(coverImageUrl);
            newVersionDO.setJsonData(jsonData);
            newVersionDO.setVersion(existingItinerary.getVersion() + 1); // 版本号递增
            newVersionDO.setComponentVersion(existingItinerary.getComponentVersion());
            newVersionDO.setIsCurrentVersion(1); // 新版本为当前使用版本
            newVersionDO.setStatus(existingItinerary.getStatus() != null ? existingItinerary.getStatus() : "NORMAL"); // 继承原状态或默认NORMAL
            newVersionDO.setIsDeleted(0);
            
            // 保存新版本
            int result = itineraryRepository.insert(newVersionDO);
            if (result <= 0) {
                log.error("创建计划表新版本失败: itineraryKey={}", itineraryKey);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新计划表失败");
            }
            
            log.info("创建计划表新版本成功: itineraryKey={}, newVersion={}", itineraryKey, newVersionDO.getVersion());
            return toBO(newVersionDO);
        }
    }

    @Override
    public ItineraryBO getItineraryByKey(String itineraryKey) {
        log.debug("获取计划表: itineraryKey={}", itineraryKey);

        if (!StringUtils.hasText(itineraryKey)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划表Key不能为空");
        }

        ItineraryDO itineraryDO = itineraryRepository.selectByItineraryKey(itineraryKey);
        if (itineraryDO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "计划表不存在");
        }

        return toBO(itineraryDO);
    }

    @Override
    public ItineraryBO getItineraryByKeyAndUserId(String itineraryKey, Long userId) {
        log.debug("获取计划表: itineraryKey={}, userId={}", itineraryKey, userId);

        ItineraryBO itinerary = getItineraryByKey(itineraryKey);

        // 权限校验：只能获取自己的计划表
        if (!itinerary.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该计划表");
        }

        return itinerary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItinerary(String itineraryKey, Long userId) {
        log.info("删除计划表: itineraryKey={}, userId={}", itineraryKey, userId);

        // 获取计划表（包含权限校验）
        getItineraryByKeyAndUserId(itineraryKey, userId);

        // 查询数据库中的DO对象
        ItineraryDO itineraryDO = itineraryRepository.selectByItineraryKey(itineraryKey);
        if (itineraryDO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "计划表不存在");
        }

        // 软删除（MyBatis-Plus的@TableLogic会自动处理）
        int result = itineraryRepository.deleteById(itineraryDO.getId());
        if (result <= 0) {
            log.error("删除计划表失败: itineraryKey={}", itineraryKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除计划表失败");
        }

        log.info("删除计划表成功: itineraryKey={}", itineraryKey);
    }

    @Override
    public PageResult<ItineraryBO> getUserItineraries(Long userId, com.lvxin.base.common.PageReq pageReq) {
        log.debug("获取用户计划表列表（分页）: userId={}, pageNum={}, pageSize={}", 
                userId, pageReq.getPageNum(), pageReq.getPageSize());

        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户ID不能为空");
        }

        // 构建查询条件
        QueryWrapper<ItineraryDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("updated_at");

        // 分页查询
        Page<ItineraryDO> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
        Page<ItineraryDO> resultPage = itineraryRepository.selectPage(page, queryWrapper);

        // 转换为BO列表
        List<ItineraryBO> boList = resultPage.getRecords().stream()
                .map(this::toBO)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<ItineraryBO> pageResult = new PageResult<>();
        pageResult.setList(boList);
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setPage(pageReq.getPageNum());
        pageResult.setPageSize(pageReq.getPageSize());

        return pageResult;
    }

    @Override
    public List<ItineraryBO> getUserItineraries(Long userId) {
        log.debug("获取用户计划表列表: userId={}", userId);

        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户ID不能为空");
        }

        List<ItineraryDO> doList = itineraryRepository.selectByUserId(userId);

        // 转换为BO列表
        return doList.stream()
                .map(this::toBO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItineraryBO createGeneratingItinerary(Long userId, String title, String originalPlan, String coverImageUrl) {
        log.info("创建生成中的计划表: userId={}, title={}", userId, title);

        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划表标题不能为空");
        }

        // 生成计划表唯一标识（Key）
        String newItineraryKey = UUID.randomUUID().toString();

        // 创建计划表DO（状态为GENERATING，jsonData可以为空）
        ItineraryDO itineraryDO = new ItineraryDO();
        itineraryDO.setItineraryKey(newItineraryKey);
        itineraryDO.setUserId(userId);
        itineraryDO.setTitle(title);
        itineraryDO.setCoverImageUrl(coverImageUrl);
        itineraryDO.setJsonData("{}"); // 生成中时使用空的JSON对象
        itineraryDO.setOriginalPlan(originalPlan);
        itineraryDO.setVersion(1);
        itineraryDO.setComponentVersion("v1");
        itineraryDO.setIsCurrentVersion(1);
        itineraryDO.setStatus("GENERATING"); // 状态为生成中
        itineraryDO.setIsDeleted(0);

        // 保存到数据库
        int result = itineraryRepository.insert(itineraryDO);
        if (result <= 0) {
            log.error("创建生成中的计划表失败: userId={}, title={}", userId, title);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建计划表失败");
        }

        log.info("创建生成中的计划表成功: itineraryKey={}, userId={}", newItineraryKey, userId);
        return toBO(itineraryDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItineraryStatus(String itineraryKey, String status, String jsonData, String errorMessage) {
        log.info("更新计划表状态: itineraryKey={}, status={}", itineraryKey, status);

        if (!StringUtils.hasText(itineraryKey)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划表Key不能为空");
        }
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态不能为空");
        }

        // 获取当前计划表
        ItineraryDO itineraryDO = itineraryRepository.selectByItineraryKey(itineraryKey);
        if (itineraryDO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "计划表不存在");
        }

        // 更新状态和JSON数据
        itineraryDO.setStatus(status);
        if (StringUtils.hasText(jsonData)) {
            itineraryDO.setJsonData(jsonData);
        }
        if (StringUtils.hasText(errorMessage)) {
            itineraryDO.setErrorMessage(errorMessage);
        }

        // 更新数据库
        int result = itineraryRepository.updateById(itineraryDO);
        if (result <= 0) {
            log.error("更新计划表状态失败: itineraryKey={}", itineraryKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新计划表状态失败");
        }

        log.info("更新计划表状态成功: itineraryKey={}, status={}", itineraryKey, status);
    }

    /**
     * DO转BO
     */
    private ItineraryBO toBO(ItineraryDO itineraryDO) {
        if (itineraryDO == null) {
            return null;
        }
        ItineraryBO bo = new ItineraryBO();
        BeanUtils.copyProperties(itineraryDO, bo);
        return bo;
    }
}
