package com.lvxin.base.ai.event;

import com.lvxin.base.ai.agent.PlanConverterAgent;
import com.lvxin.base.constant.ItineraryStatus;
import com.lvxin.base.service.ItineraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 计划表生成事件监听器
 * 负责异步处理计划转换任务
 *
 * @author lvxin
 */
@Slf4j
@Component
public class ItineraryGenerationListener {

    private final PlanConverterAgent converterAgent;
    private final ItineraryService itineraryService;

    public ItineraryGenerationListener(PlanConverterAgent converterAgent, ItineraryService itineraryService) {
        this.converterAgent = converterAgent;
        this.itineraryService = itineraryService;
    }

    /**
     * 处理生成事件
     * 只有在事务提交成功后才执行
     */
    @Async("defaultExecutor") // 使用线程池管理器中定义的默认线程池
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGenerationEvent(ItineraryGenerationEvent event) {
        String itineraryKey = event.getItineraryKey();
        String planMarkdown = event.getPlanMarkdown();
        Long userId = event.getUserId();
        
        long startTime = System.currentTimeMillis();
        log.info("========== [Event] 异步计划转换开始 ==========");
        log.info("[{}] 开始处理生成事件，用户ID: {}", itineraryKey, userId);
        
        try {
            // 步骤1：调用转换Agent生成JSON
            // 注意：converterAgent内部已经重构为使用Retryable和Pipeline
            String jsonString = converterAgent.convertToJson(planMarkdown);
            
            // 步骤2：补全和验证JSON (这一步也可以移入Pipeline，但目前保留在Service层逻辑中或此处)
            // 由于PlanConverterAgent现在通过Pipeline返回最终JSON，这里假设返回的已经是验证过的
            
            // 步骤3：更新计划状态为正常，并保存JSON数据
            itineraryService.updateItineraryStatus(
                    itineraryKey,
                    ItineraryStatus.NORMAL.name(),
                    jsonString,
                    null
            );
            
            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[{}] ✅ 计划转换并保存成功！总耗时: {}ms", itineraryKey, totalDuration);
            
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[{}] ❌ 计划转换失败，总耗时: {}ms", itineraryKey, totalDuration, e);
            
            // 更新状态为失败
            String errorMessage = "生成失败：" + e.getMessage();
            if (errorMessage.length() > 500) errorMessage = errorMessage.substring(0, 497) + "...";
            
            try {
                itineraryService.updateItineraryStatus(
                        itineraryKey,
                        ItineraryStatus.FAILED.name(),
                        null,
                        errorMessage
                );
            } catch (Exception updateEx) {
                log.error("[{}] 更新失败状态异常", itineraryKey, updateEx);
            }
        }
        log.info("========== [Event] 异步计划转换结束 ==========");
    }
}