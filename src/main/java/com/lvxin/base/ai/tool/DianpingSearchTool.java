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
 * 大众点评搜索工具
 * 使用高德地图POI搜索作为替代方案
 * 可以查询餐厅、酒店、景点等信息
 *
 * @author lvxin
 */
@Slf4j
@Component
public class DianpingSearchTool implements SearchTool {
    
    @Value("${ai.tools.dianping.enabled:false}")
    private Boolean enabled;
    
    @Value("${ai.tools.amap.api-key:}")
    private String amapApiKey;
    
    @Autowired(required = false)
    private HttpClientUtil httpClientUtil;
    
    private static final String AMAP_API_BASE = "https://restapi.amap.com/v3";
    
    @Override
    public String getToolName() {
        return "dianping_search";
    }
    
    @Override
    public String getToolDescription() {
        return """
            大众点评搜索工具，用于查询：
            1. 餐厅搜索：根据地点、菜系、价格等条件搜索餐厅，返回餐厅名称、地址、人均消费、评分、必点菜品等信息
            2. 酒店搜索：根据地点、价格等条件搜索酒店，返回酒店名称、地址、价格、评分等信息
            3. 景点搜索：根据地点搜索景点，返回景点名称、地址、门票价格、开放时间等信息
            
            参数格式：
            {
                "type": "restaurant" | "hotel" | "attraction",
                "location": "地点（城市或具体地址）",
                "keyword": "搜索关键词（可选）",
                "price_range": "价格范围（可选，如：100-200）",
                "cuisine": "菜系（餐厅搜索时，可选）"
            }
            """;
    }
    
    @Override
    public String execute(String params) {
        if (!enabled) {
            log.warn("大众点评搜索工具未启用");
            return JSON.toJSONString(new JSONObject().fluentPut("error", "大众点评服务未启用"));
        }
        
        try {
            JSONObject paramObj = JSON.parseObject(params);
            String type = paramObj.getString("type");
            String location = paramObj.getString("location");
            String keyword = paramObj.getString("keyword");
            String priceRange = paramObj.getString("price_range");
            String cuisine = paramObj.getString("cuisine");
            
            log.info("调用大众点评搜索: type={}, location={}, keyword={}", type, location, keyword);
            
            // 使用高德地图POI搜索作为替代方案
            return searchWithAmap(type, location, keyword, priceRange, cuisine);
            
        } catch (Exception e) {
            log.error("大众点评搜索失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
    
    /**
     * 使用高德地图POI搜索替代大众点评
     */
    private String searchWithAmap(String type, String location, String keyword, String priceRange, String cuisine) {
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
            
            // 构建搜索关键词
            StringBuilder keywordsBuilder = new StringBuilder();
            
            // 根据类型添加关键词
            if ("restaurant".equals(type)) {
                if (keyword != null && !keyword.isEmpty()) {
                    keywordsBuilder.append(keyword);
                } else if (cuisine != null && !cuisine.isEmpty()) {
                    keywordsBuilder.append(cuisine);
                } else {
                    keywordsBuilder.append("餐厅");
                }
            } else if ("hotel".equals(type)) {
                if (keyword != null && !keyword.isEmpty()) {
                    keywordsBuilder.append(keyword);
                } else {
                    keywordsBuilder.append("酒店");
                }
            } else if ("attraction".equals(type)) {
                if (keyword != null && !keyword.isEmpty()) {
                    keywordsBuilder.append(keyword);
                } else {
                    keywordsBuilder.append("景点");
                }
            } else {
                keywordsBuilder.append(keyword != null ? keyword : "地点");
            }
            
            String keywords = keywordsBuilder.toString();
            
            // 构建高德地图POI搜索URL
            StringBuilder urlBuilder = new StringBuilder(AMAP_API_BASE + "/place/text?key=" + amapApiKey
                    + "&keywords=" + URLEncoder.encode(keywords, StandardCharsets.UTF_8));
            
            if (location != null && !location.isEmpty()) {
                urlBuilder.append("&city=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));
            }
            
            urlBuilder.append("&offset=20") // 返回20条结果
                    .append("&page=1")
                    .append("&extensions=all"); // 返回详细信息
            
            String url = urlBuilder.toString();
            
            log.debug("调用高德地图POI搜索（替代大众点评）: type={}, keywords={}, location={}, URL: {}", 
                    type, keywords, location, url);
            
            String response = httpClientUtil.get(url);
            JSONObject jsonResponse = JSON.parseObject(response);
            
            if ("1".equals(jsonResponse.getString("status"))) {
                JSONArray pois = jsonResponse.getJSONArray("pois");
                JSONArray results = new JSONArray();
                
                if (pois != null) {
                    for (int i = 0; i < pois.size(); i++) {
                        JSONObject poi = pois.getJSONObject(i);
                        
                        // 根据类型过滤POI
                        String poiType = poi.getString("type");
                        if (type != null) {
                            if ("restaurant".equals(type) && !poiType.contains("餐饮") && 
                                !poiType.contains("美食") && !poiType.contains("餐厅")) {
                                continue;
                            }
                            if ("hotel".equals(type) && !poiType.contains("住宿") && 
                                !poiType.contains("酒店") && !poiType.contains("宾馆")) {
                                continue;
                            }
                            if ("attraction".equals(type) && !poiType.contains("风景名胜") && 
                                !poiType.contains("景点") && !poiType.contains("公园")) {
                                continue;
                            }
                        }
                        
                        JSONObject result = new JSONObject();
                        result.put("name", poi.getString("name"));
                        result.put("address", poi.getString("address"));
                        result.put("location", poi.getString("location"));
                        result.put("type", poiType);
                        result.put("tel", poi.getString("tel"));
                        result.put("distance", poi.getString("distance"));
                        
                        // 提取详细信息
                        if (poi.getJSONObject("biz_ext") != null) {
                            JSONObject bizExt = poi.getJSONObject("biz_ext");
                            result.put("rating", bizExt.getString("rating"));
                            result.put("cost", bizExt.getString("cost"));
                            result.put("level", bizExt.getString("level"));
                            result.put("opentime", bizExt.getString("opentime"));
                        }
                        
                        results.add(result);
                    }
                }
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("type", type);
                result.put("location", location);
                result.put("count", results.size());
                result.put("results", results);
                return JSON.toJSONString(result);
            } else {
                return JSON.toJSONString(new JSONObject()
                        .fluentPut("success", false)
                        .fluentPut("error", jsonResponse.getString("info")));
            }
            
        } catch (Exception e) {
            log.error("高德地图POI搜索失败（替代大众点评）", e);
            return JSON.toJSONString(new JSONObject()
                    .fluentPut("success", false)
                    .fluentPut("error", "搜索失败: " + e.getMessage()));
        }
    }
}
