package com.lvxin.base.ai.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lvxin.base.ai.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 高德地图搜索工具
 * 用于查询交通路线、地点信息等
 *
 * @author lvxin
 */
@Slf4j
@Component
public class AmapSearchTool implements SearchTool {
    
    @Value("${ai.tools.amap.api-key:}")
    private String apiKey;
    
    @Value("${ai.tools.amap.enabled:false}")
    private Boolean enabled;
    
    @Autowired(required = false)
    private HttpClientUtil httpClientUtil;
    
    private static final String AMAP_API_BASE = "https://restapi.amap.com/v3";
    
    @Override
    public String getToolName() {
        return "amap_search";
    }
    
    @Override
    public String getToolDescription() {
        return """
            高德地图搜索工具，用于查询：
            1. 地点搜索（place）：根据关键词搜索地点，返回详细地址、坐标、类型等信息
            2. 路径规划（route）：查询两点之间的驾车路线，返回详细路线、距离、时间、费用
            3. 步行规划（walking）：查询两点之间的步行路线（100km内），返回详细路线、距离、时间
            4. 公交查询（bus）：查询公交线路、站点信息
            5. 周边搜索（around）：根据坐标和关键词搜索周边POI（餐厅、酒店、景点等）
            6. 天气查询（weather）：查询指定城市的天气信息（当前天气和未来预报）
            
            参数格式：
            {
                "type": "place" | "route" | "walking" | "bus" | "around" | "weather",
                "query": "搜索关键词（地点搜索、周边搜索时）",
                "city": "城市名称（地点搜索、天气查询时）",
                "origin": "起点地址或坐标（路径规划、步行规划时）",
                "destination": "终点地址或坐标（路径规划、步行规划时）",
                "location": "中心点坐标，格式：经度,纬度（周边搜索时）",
                "radius": "搜索半径，单位米，默认3000（周边搜索时）",
                "strategy": "路径策略：0-速度优先，1-费用优先，2-距离优先，3-不走高速（路径规划时）"
            }
            """;
    }
    
    @Override
    public String execute(String params) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("高德地图API未启用或未配置API Key");
            return JSON.toJSONString(new JSONObject().fluentPut("error", "高德地图服务未启用"));
        }
        
        try {
            JSONObject paramObj = JSON.parseObject(params);
            String type = paramObj.getString("type");
            
            switch (type) {
                case "place":
                    return searchPlace(paramObj);
                case "route":
                    return searchRoute(paramObj);
                case "walking":
                    return searchWalking(paramObj);
                case "bus":
                    return searchBus(paramObj);
                case "around":
                    return searchAround(paramObj);
                case "weather":
                    return searchWeather(paramObj);
                default:
                    return JSON.toJSONString(new JSONObject().fluentPut("error", "不支持的类型: " + type));
            }
        } catch (Exception e) {
            log.error("高德地图搜索失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", "搜索失败: " + e.getMessage()));
        }
    }
    
    /**
     * 地点搜索
     */
    private String searchPlace(JSONObject params) {
        try {
            String query = params.getString("query");
            String city = params.getString("city"); // 可选，城市名称
            
            // 构建API URL
            StringBuilder urlBuilder = new StringBuilder(AMAP_API_BASE + "/place/text?key=" + apiKey 
                    + "&keywords=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
            if (city != null && !city.isEmpty()) {
                urlBuilder.append("&city=").append(URLEncoder.encode(city, StandardCharsets.UTF_8));
            }
            String url = urlBuilder.toString();
            
            // 调用高德API
            log.info("调用高德地图地点搜索: {}, URL: {}", query, url);
            
            if (httpClientUtil == null) {
                log.warn("HTTP客户端未配置，返回模拟数据");
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "HTTP客户端未配置，返回模拟数据");
                result.put("query", query);
                return JSON.toJSONString(result);
            }
            
            try {
                String response = httpClientUtil.get(url);
                log.debug("高德地图API响应: {}", response);
                return response;
            } catch (Exception e) {
                log.error("高德地图API调用失败", e);
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("error", "API调用失败: " + e.getMessage());
                return JSON.toJSONString(result);
            }
            
        } catch (Exception e) {
            log.error("地点搜索失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
    
    /**
     * 路径规划
     */
    private String searchRoute(JSONObject params) {
        try {
            String origin = params.getString("origin");
            String destination = params.getString("destination");
            String strategy = params.getString("strategy");
            if (strategy == null || strategy.isEmpty()) {
                strategy = "0";
            }
            
            String url = AMAP_API_BASE + "/direction/driving?key=" + apiKey
                    + "&origin=" + URLEncoder.encode(origin, StandardCharsets.UTF_8)
                    + "&destination=" + URLEncoder.encode(destination, StandardCharsets.UTF_8)
                    + "&strategy=" + strategy;
            
            log.info("调用高德地图路径规划: {} -> {}, URL: {}", origin, destination, url);
            
            if (httpClientUtil == null) {
                log.warn("HTTP客户端未配置，返回模拟数据");
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "HTTP客户端未配置，返回模拟数据");
                result.put("origin", origin);
                result.put("destination", destination);
                return JSON.toJSONString(result);
            }
            
            try {
                String response = httpClientUtil.get(url);
                log.debug("高德地图API响应: {}", response);
                return response;
            } catch (Exception e) {
                log.error("高德地图API调用失败", e);
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("error", "API调用失败: " + e.getMessage());
                return JSON.toJSONString(result);
            }
            
        } catch (Exception e) {
            log.error("路径规划失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
    
    /**
     * 公交查询
     */
    private String searchBus(JSONObject params) {
        try {
            String origin = params.getString("origin");
            String destination = params.getString("destination");
            String city = params.getString("city");
            
            String url = AMAP_API_BASE + "/direction/transit/integrated?key=" + apiKey
                    + "&origin=" + URLEncoder.encode(origin, StandardCharsets.UTF_8)
                    + "&destination=" + URLEncoder.encode(destination, StandardCharsets.UTF_8)
                    + "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8);
            
            log.info("调用高德地图公交查询: {} -> {} ({}), URL: {}", origin, destination, city, url);
            
            if (httpClientUtil == null) {
                log.warn("HTTP客户端未配置，返回模拟数据");
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "HTTP客户端未配置，返回模拟数据");
                return JSON.toJSONString(result);
            }
            
            try {
                String response = httpClientUtil.get(url);
                log.debug("高德地图API响应: {}", response);
                return response;
            } catch (Exception e) {
                log.error("高德地图API调用失败", e);
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("error", "API调用失败: " + e.getMessage());
                return JSON.toJSONString(result);
            }
            
        } catch (Exception e) {
            log.error("公交查询失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
    
    /**
     * 步行路径规划
     */
    private String searchWalking(JSONObject params) {
        try {
            String origin = params.getString("origin");
            String destination = params.getString("destination");
            
            String url = AMAP_API_BASE + "/direction/walking?key=" + apiKey
                    + "&origin=" + URLEncoder.encode(origin, StandardCharsets.UTF_8)
                    + "&destination=" + URLEncoder.encode(destination, StandardCharsets.UTF_8);
            
            log.info("调用高德地图步行规划: {} -> {}, URL: {}", origin, destination, url);
            
            if (httpClientUtil == null) {
                log.warn("HTTP客户端未配置，返回模拟数据");
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "HTTP客户端未配置，返回模拟数据");
                result.put("origin", origin);
                result.put("destination", destination);
                return JSON.toJSONString(result);
            }
            
            try {
                String response = httpClientUtil.get(url);
                log.debug("高德地图API响应: {}", response);
                return response;
            } catch (Exception e) {
                log.error("高德地图API调用失败", e);
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("error", "API调用失败: " + e.getMessage());
                return JSON.toJSONString(result);
            }
            
        } catch (Exception e) {
            log.error("步行规划失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
    
    /**
     * 周边搜索
     */
    private String searchAround(JSONObject params) {
        try {
            String location = params.getString("location"); // 格式：经度,纬度
            String keywords = params.getString("query"); // 搜索关键词
            String radius = params.getString("radius"); // 搜索半径，单位米，默认3000
            if (radius == null || radius.isEmpty()) {
                radius = "3000";
            }
            
            StringBuilder urlBuilder = new StringBuilder(AMAP_API_BASE + "/place/around?key=" + apiKey
                    + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                    + "&radius=" + radius);
            
            if (keywords != null && !keywords.isEmpty()) {
                urlBuilder.append("&keywords=").append(URLEncoder.encode(keywords, StandardCharsets.UTF_8));
            }
            
            String url = urlBuilder.toString();
            
            log.info("调用高德地图周边搜索: location={}, keywords={}, radius={}, URL: {}", location, keywords, radius, url);
            
            if (httpClientUtil == null) {
                log.warn("HTTP客户端未配置，返回模拟数据");
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "HTTP客户端未配置，返回模拟数据");
                result.put("location", location);
                result.put("keywords", keywords);
                return JSON.toJSONString(result);
            }
            
            try {
                String response = httpClientUtil.get(url);
                log.debug("高德地图API响应: {}", response);
                return response;
            } catch (Exception e) {
                log.error("高德地图API调用失败", e);
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("error", "API调用失败: " + e.getMessage());
                return JSON.toJSONString(result);
            }
            
        } catch (Exception e) {
            log.error("周边搜索失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
    
    /**
     * 天气查询
     */
    private String searchWeather(JSONObject params) {
        try {
            String city = params.getString("city"); // 城市名称或城市adcode
            
            if (city == null || city.isEmpty()) {
                return JSON.toJSONString(new JSONObject().fluentPut("error", "城市名称不能为空"));
            }
            
            // 高德天气API：https://restapi.amap.com/v3/weather/weatherInfo
            String url = AMAP_API_BASE + "/weather/weatherInfo?key=" + apiKey
                    + "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&extensions=all"; // all表示返回预报天气，base表示返回当前天气
            
            log.info("调用高德地图天气查询: city={}, URL: {}", city, url);
            
            if (httpClientUtil == null) {
                log.warn("HTTP客户端未配置，返回模拟数据");
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "HTTP客户端未配置，返回模拟数据");
                result.put("city", city);
                return JSON.toJSONString(result);
            }
            
            try {
                String response = httpClientUtil.get(url);
                log.debug("高德地图API响应: {}", response);
                return response;
            } catch (Exception e) {
                log.error("高德地图API调用失败", e);
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("error", "API调用失败: " + e.getMessage());
                return JSON.toJSONString(result);
            }
            
        } catch (Exception e) {
            log.error("天气查询失败", e);
            return JSON.toJSONString(new JSONObject().fluentPut("error", e.getMessage()));
        }
    }
}
