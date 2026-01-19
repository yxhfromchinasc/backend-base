package com.lvxin.base.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * 计划调优阶段的Prompt模板
 *
 * @author lvxin
 */
@Component
public class PlanOptimizingPrompt implements PromptTemplate {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个专业的旅行规划助手。用户对当前计划有反馈，需要你根据反馈进行调整。
            
            **当前计划：**
            %s
            
            **用户反馈：**
            %s
            
            请根据用户的反馈，对计划进行调整和优化。
            
            **调整原则：**
            1. 保持Markdown格式
            2. 只调整用户提到的问题，其他部分尽量保持不变
            3. 调整后的计划要更加符合用户的需求
            4. 如果用户的反馈不明确，可以询问具体想要如何调整
            
            **重要提示：**
            - **这是单次对话，必须一次性返回调整后的完整计划，不要使用"稍等片刻"、"正在生成"等表述**
            - **直接输出调整后的完整计划，不要承诺"马上发给你"、"正在整理"等**
            
            **输出标记要求：**
            - 当你输出调整后的完整计划时，必须在计划的开头添加标记：**[完整计划]**
            - 这个标记用于标识这是一个完整的、可以转换为系统计划表的计划
            - 如果只是部分调整建议或说明，不要添加此标记
            
            请输出调整后的完整计划。
            """;

    @Override
    public String generateSystemPrompt(Object context) {
        PlanOptimizingContext ctx = (PlanOptimizingContext) context;
        return String.format(
                SYSTEM_PROMPT_TEMPLATE,
                ctx.getCurrentPlan(),
                ctx.getUserFeedback()
        );
    }

    /**
     * 计划调优上下文
     */
    public static class PlanOptimizingContext {
        private final String currentPlan;
        private final String userFeedback;

        public PlanOptimizingContext(String currentPlan, String userFeedback) {
            this.currentPlan = currentPlan;
            this.userFeedback = userFeedback;
        }

        public String getCurrentPlan() {
            return currentPlan;
        }

        public String getUserFeedback() {
            return userFeedback;
        }
    }
}
