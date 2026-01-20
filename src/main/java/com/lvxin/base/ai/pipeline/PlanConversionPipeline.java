package com.lvxin.base.ai.pipeline;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 转换流水线管理器
 *
 * @author lvxin
 */
@Component
public class PlanConversionPipeline {

    private final List<ConversionPipelineStep> steps;

    // Spring会自动注入所有实现了接口的Bean，并按@Order排序
    public PlanConversionPipeline(List<ConversionPipelineStep> steps) {
        this.steps = steps;
    }

    public String execute(String markdown) {
        String currentJson = null;
        for (ConversionPipelineStep step : steps) {
            currentJson = step.process(currentJson, markdown);
        }
        return currentJson;
    }
}
