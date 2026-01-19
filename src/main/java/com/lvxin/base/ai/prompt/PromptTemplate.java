package com.lvxin.base.ai.prompt;

/**
 * Prompt模板接口
 * 定义不同阶段的Prompt生成策略
 *
 * @author lvxin
 */
public interface PromptTemplate {
    /**
     * 生成系统Prompt
     *
     * @param context 上下文信息
     * @return 系统Prompt
     */
    String generateSystemPrompt(Object context);
}
