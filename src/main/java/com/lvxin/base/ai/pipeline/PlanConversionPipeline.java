package com.lvxin.base.ai.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 转换流水线管理器
 *
 * @author lvxin
 */
@Slf4j
@Component
public class PlanConversionPipeline {

    private final List<ConversionPipelineStep> steps;

    // Spring会自动注入所有实现了接口的Bean，并按@Order排序
    public PlanConversionPipeline(List<ConversionPipelineStep> steps) {
        this.steps = steps;
    }

    public String execute(String markdown) {
        log.info("🚀 [Pipeline] 开始执行转换流水线，Steps: {}", steps.size());
        String currentJson = null;
        
        for (ConversionPipelineStep step : steps) {
            String stepName = step.getClass().getSimpleName();
            log.info("👉 [Pipeline] 正在执行步骤: {}", stepName);
            long startTime = System.currentTimeMillis();
            
            try {
                currentJson = step.process(currentJson, markdown);
                
                long duration = System.currentTimeMillis() - startTime;
                int dataLength = currentJson != null ? currentJson.length() : 0;
                log.info("✅ [Pipeline] 步骤 {} 完成，耗时: {}ms，当前数据长度: {}", stepName, duration, dataLength);
                
                // 打印中间数据预览 (只在Debug模式或关键节点打印，这里为了响应用户需求打印Info但截断)
                if (currentJson != null) {
                    String preview = currentJson.length() > 200 ? 
                            currentJson.substring(0, 100) + " ... " + currentJson.substring(currentJson.length() - 100) : 
                            currentJson;
                    log.info("📄 [Pipeline] 数据预览: {}", preview);
                }
                
            } catch (Exception e) {
                log.error("❌ [Pipeline] 步骤 {} 执行失败", stepName, e);
                throw e;
            }
        }
        
        log.info("🎉 [Pipeline] 流水线执行完毕");
        return currentJson;
    }
}
