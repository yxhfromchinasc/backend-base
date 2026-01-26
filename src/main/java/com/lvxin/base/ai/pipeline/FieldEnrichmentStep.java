package com.lvxin.base.ai.pipeline;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lvxin.base.ai.monitor.PerformanceMonitorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 步骤3：字段补全
 * 确保必要的结构字段存在
 */
@Slf4j
@Component
@Order(3)
public class FieldEnrichmentStep implements ConversionPipelineStep {

    @Override
    public String process(String currentJson, String originalMarkdown) {
        log.info("[Pipeline] Step 3: Enriching fields...");
        
        PerformanceMonitorContext.recordStage("字段补全开始");
        long startTime = System.currentTimeMillis();
        
        try {
            // 解析JSON
            long parseStartTime = System.currentTimeMillis();
            JSONObject jsonObject = JSON.parseObject(currentJson);
            long parseDuration = System.currentTimeMillis() - parseStartTime;
            PerformanceMonitorContext.recordDuration("解析JSON", parseDuration);
            
            // 补全基础字段
            long enrichStartTime = System.currentTimeMillis();
            if (!jsonObject.containsKey("title")) jsonObject.put("title", "AI生成的旅行计划");
            if (!jsonObject.containsKey("days")) jsonObject.put("days", Collections.emptyList());
            long enrichDuration = System.currentTimeMillis() - enrichStartTime;
            PerformanceMonitorContext.recordDuration("补全字段", enrichDuration);
            
            // 版本字段通常由Service层在保存前设置，但这里也可以兜底
            // jsonObject.putIfAbsent("version", 1);
            
            // 序列化JSON
            long serializeStartTime = System.currentTimeMillis();
            String result = JSON.toJSONString(jsonObject);
            long serializeDuration = System.currentTimeMillis() - serializeStartTime;
            PerformanceMonitorContext.recordDuration("序列化JSON", serializeDuration);
            
            long totalDuration = System.currentTimeMillis() - startTime;
            PerformanceMonitorContext.recordDuration("字段补全总耗时", totalDuration);
            PerformanceMonitorContext.recordStage("字段补全完成");
            
            return result;
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            PerformanceMonitorContext.recordDuration("字段补全总耗时(失败)", totalDuration);
            log.warn("字段补全步骤失败，返回原JSON", e);
            return currentJson;
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
