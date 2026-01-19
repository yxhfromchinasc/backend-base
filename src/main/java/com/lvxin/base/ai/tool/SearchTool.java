package com.lvxin.base.ai.tool;

/**
 * 搜索工具接口
 * 定义各种搜索工具的统一接口
 *
 * @author lvxin
 */
public interface SearchTool {
    
    /**
     * 工具名称
     */
    String getToolName();
    
    /**
     * 工具描述（用于AI理解工具用途）
     */
    String getToolDescription();
    
    /**
     * 执行搜索
     *
     * @param params 搜索参数（JSON格式）
     * @return 搜索结果（JSON格式）
     */
    String execute(String params);
}
