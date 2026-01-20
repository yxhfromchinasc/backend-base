package com.lvxin.base.ai.pipeline;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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
        
        try {
            JSONObject jsonObject = JSON.parseObject(currentJson);
            
            // 补全基础字段
            if (!jsonObject.containsKey("title")) jsonObject.put("title", "AI生成的旅行计划");
            if (!jsonObject.containsKey("days")) jsonObject.put("days", Collections.emptyList());
            
            // 版本字段通常由Service层在保存前设置，但这里也可以兜底
            // jsonObject.putIfAbsent("version", 1);
            
            return JSON.toJSONString(jsonObject);
        } catch (Exception e) {
            log.warn("字段补全步骤失败，返回原JSON", e);
            return currentJson;
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
