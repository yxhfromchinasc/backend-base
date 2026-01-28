package com.lvxin.base.service;

import com.lvxin.base.common.PageReq;
import com.lvxin.base.common.PageResult;
import com.lvxin.base.service.bo.ItineraryBO;

import java.util.List;

/**
 * 计划表服务接口
 * 提供计划表相关的业务逻辑
 */
public interface ItineraryService {

    /**
     * 创建或更新计划表（Upsert）
     *
     * @param userId 用户ID
     * @param itineraryKey 计划表唯一标识（Key），新增时为null，更新时必须提供
     * @param title 计划表标题
     * @param coverImageUrls 封面图片URL列表（可选）
     * @param jsonData 计划表JSON数据
     * @param currentVersion 当前版本号（用于并发控制），新增时不需要，更新时必须提供
     * @return 计划表业务对象
     */
    ItineraryBO upsertItinerary(Long userId, String itineraryKey, String title, List<String> coverImageUrls, String jsonData, Integer currentVersion);

    /**
     * 根据计划表唯一标识（Key）获取计划表
     *
     * @param itineraryKey 计划表唯一标识（Key）
     * @return 计划表业务对象
     */
    ItineraryBO getItineraryByKey(String itineraryKey);

    /**
     * 根据计划表唯一标识（Key）和用户ID获取计划表（用于权限校验）
     *
     * @param itineraryKey 计划表唯一标识（Key）
     * @param userId 用户ID
     * @return 计划表业务对象
     */
    ItineraryBO getItineraryByKeyAndUserId(String itineraryKey, Long userId);

    /**
     * 删除计划表（软删除）
     *
     * @param itineraryKey 计划表唯一标识（Key）
     * @param userId 用户ID（用于权限校验）
     */
    void deleteItinerary(String itineraryKey, Long userId);

    /**
     * 获取用户的所有计划表列表（分页）
     *
     * @param userId 用户ID
     * @param pageReq 分页请求
     * @return 分页结果
     */
    PageResult<ItineraryBO> getUserItineraries(Long userId, PageReq pageReq);

    /**
     * 获取用户的所有计划表列表（不分页）
     *
     * @param userId 用户ID
     * @return 计划表列表
     */
    List<ItineraryBO> getUserItineraries(Long userId);

    /**
     * 创建生成中的计划表（用于异步生成）
     * jsonData可以为空，状态为GENERATING
     *
     * @param userId 用户ID
     * @param title 计划表标题
     * @param originalPlan 原始计划内容（用户输入的文本或AI对话的markdown）
     * @param coverImageUrls 封面图片URL列表（可选）
     * @return 计划表业务对象
     */
    ItineraryBO createGeneratingItinerary(Long userId, String title, String originalPlan, List<String> coverImageUrls);

    /**
     * 更新计划表状态和JSON数据（用于异步生成完成后更新）
     *
     * @param itineraryKey 计划表唯一标识（Key）
     * @param status 状态（NORMAL-正常，FAILED-生成失败）
     * @param jsonData 计划表JSON数据（生成成功时必填）
     * @param errorMessage 错误信息（生成失败时可选）
     */
    void updateItineraryStatus(String itineraryKey, String status, String jsonData, String errorMessage);
}
