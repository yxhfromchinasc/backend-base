package com.lvxin.base.ai.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lvxin.base.ai.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 通用网络搜索工具
 * 使用高德地图POI搜索作为网络搜索的替代方案
 * 可以搜索地点、服务、商家等信息
 *
 * @author lvxin
 */
@Slf4j
@Component
public class WebSearchTool implements SearchTool {
    
    @Value("${ai.tools.web-search.enabled:false}")
    private Boolean enabled;
    
    @Value("${ai.tools.web-search.provider:amap}")
    private String provider; // amap（使用高德地图POI搜索）
    
    @Value("${ai.tools.amap.api-key:}")
    private String amapApiKey;
    
    @Autowired(required = false)
    private HttpClientUtil httpClientUtil;
    
    private static final String AMAP_API_BASE = "https://restapi.amap.com/v3";
    
    @Override
    public String getToolName() {
        return "web_search";
    }
    
    @Override
    public String getToolDescription() {
        return """
            通用网络搜索工具，用于搜索最新的网络信息：
            1. 实时信息搜索：搜索最新的新闻、事件、数据等
            2. 知识查询：查询特定主题的详细信息
            3. 价格查询：查询商品、服务的最新价格
            
            参数格式：
            {
                "query": "搜索关键词",
                "max_results": 搜索结果数量（可选，默认5）
            }
            """;
    }
    
    @Override
    public String execute(String params) {
        if (!enabled) {
            log.warn("网络搜索工具未启用");
            return JSON.toJSONString(new JSONObject().fluentPut("error", "网络搜索服务未启用"));
        }
        
        try {
            JSONObject paramObj = JSON.parseObject(params);
            String query = paramObj.getString("query");
            int maxResults = paramObj.getIntValue("max_results", 5);
            
            log.info("调用网络搜索: query={}, provider={}, maxResults={}", query, provider, maxResults);
            
            // 使用高德地图POI搜索作为网络搜索的替代方案
            if ("amap".equals(provider) || amapApiKey != null && !amapApiKey.isEmpty()) {
                return searchWithAmap(query, maxResults);
            }
            
            // 如果没有配置高德地图，返回模拟数据
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("message", "网络搜索功能需要配置高德地图API Key");
            result.put("query", query);
            result.put("results", new JSONArray());
            return JSON.toJSONString(result);
            
        } catch (Exception e) {
            log.error("网络搜索失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
    
    /**
     * 使用高德地图POI搜索
     */
    private String searchWithAmap(String query, int maxResults) {
        try {
            if (amapApiKey == null || amapApiKey.isEmpty()) {
                return JSON.toJSONString(new JSONObject()
                        .fluentPut("success", false)
                        .fluentPut("error", "高德地图API Key未配置"));
            }
            
            if (httpClientUtil == null) {
                return JSON.toJSONString(new JSONObject()
                        .fluentPut("success", false)
                        .fluentPut("error", "HTTP客户端未配置"));
            }
            
            // 构建高德地图POI搜索URL
            String url = AMAP_API_BASE + "/place/text?key=" + amapApiKey
                    + "&keywords=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&offset=" + maxResults
                    + "&page=1"
                    + "&extensions=all"; // 返回详细信息
            
            log.debug("调用高德地图POI搜索: query={}, URL: {}", query, url);
            
            String response = httpClientUtil.get(url);
            JSONObject jsonResponse = JSON.parseObject(response);
            
            if ("1".equals(jsonResponse.getString("status"))) {
                JSONArray pois = jsonResponse.getJSONArray("pois");
                JSONArray results = new JSONArray();
                
                if (pois != null) {
                    int count = Math.min(pois.size(), maxResults);
                    for (int i = 0; i < count; i++) {
                        JSONObject poi = pois.getJSONObject(i);
                        JSONObject result = new JSONObject();
                        result.put("title", poi.getString("name"));
                        result.put("address", poi.getString("address"));
                        result.put("location", poi.getString("location"));
                        result.put("type", poi.getString("type"));
                        result.put("tel", poi.getString("tel"));
                        result.put("distance", poi.getString("distance"));
                        
                        // 提取详细信息
                        if (poi.getJSONObject("biz_ext") != null) {
                            JSONObject bizExt = poi.getJSONObject("biz_ext");
                            result.put("rating", bizExt.getString("rating"));
                            result.put("cost", bizExt.getString("cost"));
                        }
                        
                        results.add(result);
                    }
                }
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("query", query);
                result.put("provider", "amap");
                result.put("count", results.size());
                result.put("results", results);
                return JSON.toJSONString(result);
            } else {
                return JSON.toJSONString(new JSONObject()
                        .fluentPut("success", false)
                        .fluentPut("error", jsonResponse.getString("info")));
            }
            
        } catch (Exception e) {
            log.error("高德地图POI搜索失败", e);
            return JSON.toJSONString(new JSONObject()
                    .fluentPut("success", false)
                    .fluentPut("error", "搜索失败: " + e.getMessage()));
        }
    }
}
