package com.lvxin.base.ai.agent;

import com.lvxin.base.ai.pipeline.PlanConversionPipeline;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * 智能体2：计划转换器
 * 负责将Markdown格式的计划转换为JSON格式的计划表
 * 重构后：使用责任链模式 + Spring Retry
 *
 * @author lvxin
 */
@Slf4j
@Component
public class PlanConverterAgent {

    private final PlanConversionPipeline pipeline;

    public PlanConverterAgent(PlanConversionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 转换Markdown计划为JSON字符串
     * 使用Spring Retry自动处理重试逻辑
     */
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 1.5)
    )
    public String convertToJson(String markdownPlan) {
        if (markdownPlan == null || markdownPlan.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划内容不能为空");
        }
        
        log.info("开始转换计划 Pipeline，长度: {}", markdownPlan.length());
        return pipeline.execute(markdownPlan);
    }
    
    /**
     * 重试耗尽后的兜底方法
     */
    @Recover
    public String recover(Exception e, String markdownPlan) {
        log.error("计划转换重试耗尽，最终失败", e);
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "计划转换失败，请稍后重试: " + e.getMessage());
    }
}
