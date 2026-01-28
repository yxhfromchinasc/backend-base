package com.lvxin.base.web.controller.user;

import com.lvxin.base.ai.service.AIPlanningService;
import com.lvxin.base.common.Result;
import com.lvxin.base.service.bo.ItineraryBO;
import com.lvxin.base.util.CurrentUserUtil;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.web.req.AIChatReq;
import com.lvxin.base.web.req.AIConvertReq;
import com.lvxin.base.web.req.XiaohongshuToItineraryReq;
import com.lvxin.base.web.resp.AIChatResp;
import com.lvxin.base.web.resp.ItineraryDetailResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI规划控制器
 * 提供AI旅行规划相关的API接口
 *
 * @author lvxin
 */
@Slf4j
@RestController
@RequestMapping("/user/ai-planning")
@Tag(name = "用户端-AI规划", description = "AI旅行规划相关接口")
public class AIPlanningController {

    private final AIPlanningService aiPlanningService;

    public AIPlanningController(AIPlanningService aiPlanningService) {
        this.aiPlanningService = aiPlanningService;
    }

    /**
     * AI对话接口
     * 用户与AI进行多轮对话，收集需求并生成计划
     */
    @Operation(summary = "AI对话", description = "与AI进行对话，收集需求并生成旅行计划")
    @PostMapping("/chat")
    @AuthCheck
    public Result<AIChatResp> chat(@Valid @RequestBody AIChatReq request) {
        Long userId = CurrentUserUtil.getCurrentUserId();
        log.debug("AI对话请求，conversationId: {}, userId: {}", request.getConversationId(), userId);

        AIPlanningService.ChatResponse response = aiPlanningService.chat(
                request.getConversationId(),
                request.getMessage(),
                userId
        );

        AIChatResp resp = AIChatResp.builder()
                .conversationId(response.getConversationId())
                .reply(response.getReply())
                .stage(response.getStage().name())
                .planMarkdown(response.getPlanMarkdown())
                .canConvert(response.getCanConvert())
                .build();

        return Result.success(resp);
    }

    /**
     * 转换计划为计划表
     * 将AI生成的Markdown计划转换为系统计划表并保存
     */
    @Operation(summary = "转换计划为计划表", description = "将AI生成的计划转换为计划表并保存")
    @PostMapping("/convert")
    @AuthCheck
    public Result<ItineraryDetailResp> convert(@Valid @RequestBody AIConvertReq request) {
        Long userId = CurrentUserUtil.getCurrentUserId();
        log.info("转换计划为计划表，conversationId: {}, userId: {}", request.getConversationId(), userId);

        ItineraryBO itineraryBO = aiPlanningService.convertToItinerary(
                request.getConversationId(),
                userId
        );

        // 转换为响应对象
        ItineraryDetailResp resp = toItineraryDetailResp(itineraryBO);
        return Result.success(resp);
    }

    /**
     * 文本转计划表
     * 将用户输入的文本计划转换为系统计划表并保存
     */
    @Operation(summary = "文本转计划表", description = "将文本格式的旅行计划转换为计划表并保存")
    @PostMapping("/text-to-itinerary")
    @AuthCheck
    public Result<ItineraryDetailResp> textToItinerary(@Valid @RequestBody com.lvxin.base.web.req.TextToItineraryReq request) {
        Long userId = CurrentUserUtil.getCurrentUserId();
        log.info("文本转计划表，userId: {}, textLength: {}", userId, request.getText() != null ? request.getText().length() : 0);

        ItineraryBO itineraryBO = aiPlanningService.convertTextToItinerary(
                request.getText(),
                userId
        );

        // 转换为响应对象
        ItineraryDetailResp resp = toItineraryDetailResp(itineraryBO);
        return Result.success(resp);
    }

    /**
     * 小红书链接转计划表
     * 获取小红书链接中的旅游攻略，转换为系统计划表并保存
     */
    @Operation(summary = "小红书链接转计划表", description = "获取小红书链接中的旅游攻略，转换为计划表并保存")
    @PostMapping("/xiaohongshu-to-itinerary")
    @AuthCheck
    public Result<ItineraryDetailResp> xiaohongshuToItinerary(@Valid @RequestBody XiaohongshuToItineraryReq request) {
        Long userId = CurrentUserUtil.getCurrentUserId();
        log.info("小红书链接转计划表，userId: {}, url: {}", userId, request.getUrl());

        ItineraryBO itineraryBO = aiPlanningService.convertXiaohongshuToItinerary(
                request.getUrl(),
                userId
        );

        // 转换为响应对象
        ItineraryDetailResp resp = toItineraryDetailResp(itineraryBO);
        return Result.success(resp);
    }

    /**
     * BO转响应对象（统一处理封面图片）
     */
    private ItineraryDetailResp toItineraryDetailResp(ItineraryBO bo) {
        ItineraryDetailResp resp = new ItineraryDetailResp();
        resp.setItineraryKey(bo.getItineraryKey());
        resp.setUserId(bo.getUserId());
        resp.setTitle(bo.getTitle());
        resp.setCoverImageUrls(bo.getCoverImageUrls());
        
        // 向后兼容：设置单个coverImageUrl（取第一张）
        if (resp.getCoverImageUrls() != null && !resp.getCoverImageUrls().isEmpty()) {
            resp.setCoverImageUrl(resp.getCoverImageUrls().get(0));
        }
        
        resp.setJsonData(bo.getJsonData());
        resp.setVersion(bo.getVersion());
        resp.setComponentVersion(bo.getComponentVersion());
        resp.setIsCurrentVersion(bo.getIsCurrentVersion());
        resp.setStatus(bo.getStatus() != null ? bo.getStatus() : "GENERATING");
        resp.setErrorMessage(bo.getErrorMessage());
        resp.setCreatedAt(bo.getCreatedAt());
        resp.setUpdatedAt(bo.getUpdatedAt());
        return resp;
    }
}
