package com.lvxin.base.ai.pipeline;

import com.lvxin.base.ai.monitor.PerformanceMonitorContext;
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
        PerformanceMonitorContext.recordStage("Pipeline开始");
        long pipelineStartTime = System.currentTimeMillis();
        
        String currentJson = null;
        int stepIndex = 0;
        for (ConversionPipelineStep step : steps) {
            stepIndex++;
            String stepName = step.getClass().getSimpleName();
            PerformanceMonitorContext.recordStage("Step" + stepIndex + "-" + stepName + "开始");
            long stepStartTime = System.currentTimeMillis();
            
            try {
                currentJson = step.process(currentJson, markdown);
                long stepDuration = System.currentTimeMillis() - stepStartTime;
                PerformanceMonitorContext.recordDuration("Step" + stepIndex + "-" + stepName, stepDuration);
                PerformanceMonitorContext.recordStage("Step" + stepIndex + "-" + stepName + "完成");
            } catch (Exception e) {
                long stepDuration = System.currentTimeMillis() - stepStartTime;
                PerformanceMonitorContext.recordDuration("Step" + stepIndex + "-" + stepName + "(失败)", stepDuration);
                throw e;
            }
        }
        
        long pipelineDuration = System.currentTimeMillis() - pipelineStartTime;
        PerformanceMonitorContext.recordDuration("Pipeline总耗时", pipelineDuration);
        PerformanceMonitorContext.recordStage("Pipeline完成");
        
        return currentJson;
    }
}
