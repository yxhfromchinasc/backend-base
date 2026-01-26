package com.lvxin.base.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具管理器
 * 管理所有可用的搜索工具，并提供统一的调用接口
 *
 * @author lvxin
 */
@Slf4j
@Component
public class ToolManager {
    
    private final Map<String, SearchTool> tools = new HashMap<>();
    
    public ToolManager(List<SearchTool> searchTools) {
        // 注册所有工具
        for (SearchTool tool : searchTools) {
            tools.put(tool.getToolName(), tool);
            log.info("注册搜索工具: {}", tool.getToolName());
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", tools.size());
    }
    
    /**
     * 获取所有工具的描述（用于Function Calling）
     */
    public Map<String, String> getAllToolDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        tools.forEach((name, tool) -> {
            descriptions.put(name, tool.getToolDescription());
        });
        return descriptions;
    }
    
    /**
     * 执行工具调用
     *
     * @param toolName 工具名称
     * @param params   参数（JSON格式）
     * @return 执行结果（JSON格式）
     */
    public String executeTool(String toolName, String params) {
        SearchTool tool = tools.get(toolName);
        if (tool == null) {
            log.warn("工具不存在: {}", toolName);
            return "{\"error\":\"工具不存在: " + toolName + "\"}";
        }
        
        long startTime = System.currentTimeMillis();
        try {
            log.info("执行工具: {}, params: {}", toolName, params);
            String result = tool.execute(params);
            long duration = System.currentTimeMillis() - startTime;
            log.info("工具执行完成: {}, 耗时: {}ms", toolName, duration);
            log.debug("工具执行结果: {}", result);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("工具执行失败: {}, 耗时: {}ms", toolName, duration, e);
            return "{\"error\":\"工具执行失败: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}
