package com.lvxin.base.ai.pipeline;

/**
 * 转换流水线步骤接口
 * 责任链模式/管道模式
 *
 * @author lvxin
 */
public interface ConversionPipelineStep {
    
    /**
     * 执行步骤
     * @param currentJson 当前流转的JSON（第一步时可能为null）
     * @param originalMarkdown 原始Markdown
     * @return 处理后的JSON
     */
    String process(String currentJson, String originalMarkdown);
    
    /**
     * 排序顺序（越小越先执行）
     */
    int getOrder();
}
