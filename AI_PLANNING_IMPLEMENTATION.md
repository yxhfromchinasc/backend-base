# AI规划功能实现文档

## 📋 概述

实现了基于阿里云通义千问的AI旅行规划功能，采用双智能体架构：
- **智能体1**：旅行规划助手（两阶段对话）
- **智能体2**：计划转换器（MD → JSON）

## 🏗️ 架构设计

### 包结构

```
com.lvxin.base.ai/
├── agent/                    # 智能体
│   ├── TravelPlanningAgent.java      # 智能体1：旅行规划助手
│   └── PlanConverterAgent.java       # 智能体2：计划转换器
├── client/                   # AI客户端
│   └── DashScopeClient.java          # 通义千问客户端封装
├── config/                   # 配置
│   └── DashScopeConfig.java          # DashScope配置
├── model/                    # 数据模型
│   ├── AIConversation.java           # 对话模型
│   ├── ChatMessage.java              # 消息模型
│   ├── ConversationStage.java        # 对话阶段枚举
│   └── TravelRequirement.java        # 旅行需求模型
├── prompt/                   # Prompt模板（策略模式）
│   ├── PromptTemplate.java           # Prompt接口
│   ├── RequirementCollectingPrompt.java    # 需求收集Prompt
│   ├── PlanGeneratingPrompt.java           # 计划生成Prompt
│   ├── PlanOptimizingPrompt.java           # 计划调优Prompt
│   └── PlanConverterPrompt.java            # 计划转换Prompt
└── service/                  # 服务层
    ├── AIConversationService.java    # 对话管理服务（Redis存储）
    └── AIPlanningService.java        # AI规划服务（协调两个智能体）
```

### 设计模式应用

1. **策略模式**：`PromptTemplate`接口，不同阶段使用不同的Prompt策略
2. **模板方法模式**：智能体1内部的两阶段处理流程
3. **职责分离**：AI客户端、智能体、服务层、控制器层清晰分离
4. **依赖注入**：使用Spring的依赖注入管理组件

## 🔄 工作流程

### 智能体1：旅行规划助手

#### 阶段1：需求收集（REQUIREMENT_COLLECTING）
- 通过多轮对话收集用户需求
- 收集信息：目的地、时间、预算、偏好、人数、特殊需求
- 每次只问1-2个问题，友好自然
- 当需求收集完整后，自动切换到阶段2

#### 阶段2：计划生成与调优（PLAN_GENERATING）
- 首次生成：根据需求生成Markdown格式的详细计划
- 计划调优：根据用户反馈调整计划
- 用户满意后（关键词识别），切换到PLAN_READY状态

### 智能体2：计划转换器

- 接收Markdown格式的计划
- 使用AI转换为JSON格式的计划表
- 验证JSON格式
- 补全必需字段
- 保存到数据库

## 📡 API接口

### 1. AI对话接口

```
POST /api/user/ai-planning/chat
请求体：
{
    "conversationId": "会话ID（可选，首次为空）",
    "message": "用户消息"
}

响应：
{
    "success": true,
    "data": {
        "conversationId": "会话ID",
        "reply": "AI回复",
        "stage": "REQUIREMENT_COLLECTING|PLAN_GENERATING|PLAN_READY",
        "planMarkdown": "MD格式计划（阶段2才有）",
        "canConvert": false  // 是否可以转换
    }
}
```

### 2. 转换计划为计划表接口

```
POST /api/user/ai-planning/convert
请求体：
{
    "conversationId": "会话ID"
}

响应：
{
    "success": true,
    "data": {
        "itineraryKey": "计划表Key",
        "title": "计划表标题",
        ...
    }
}
```

## 🔧 配置

在 `application.yml` 中配置：

```yaml
ai:
  dashscope:
    api-key: your-dashscope-api-key  # 请替换为实际的API Key
    default-model: qwen-plus          # qwen-plus（性价比高）或 qwen-max（效果更好）
    timeout: 30000                    # 请求超时时间（毫秒）
    max-retries: 3                    # 最大重试次数
```

## 💾 数据存储

- **对话历史**：存储在Redis中，Key格式：`ai:conversation:{conversationId}`
- **TTL**：24小时自动过期
- **计划表**：转换后保存到MySQL数据库的`itinerary`表

## ✨ 代码质量保证

1. **清晰的包结构**：按职责划分，易于维护
2. **设计模式应用**：策略模式、模板方法模式等
3. **异常处理**：统一的异常处理和错误码
4. **日志记录**：关键操作都有日志记录
5. **参数验证**：使用Jakarta Validation进行参数校验
6. **权限控制**：使用`@AuthCheck`注解进行权限验证
7. **事务管理**：转换保存使用`@Transactional`保证数据一致性

## 🚀 使用步骤

1. **配置API Key**：在`application.yml`中配置通义千问的API Key
2. **启动服务**：启动Spring Boot应用
3. **前端调用**：
   - 首次对话：`POST /api/user/ai-planning/chat`，`conversationId`为空
   - 继续对话：使用返回的`conversationId`继续对话
   - 转换计划：当`canConvert=true`时，调用`POST /api/user/ai-planning/convert`

## 📝 注意事项

1. **API Key安全**：不要将API Key提交到代码仓库，使用环境变量或配置中心管理
2. **成本控制**：建议设置token限制和使用频率限制
3. **错误处理**：AI服务可能不稳定，需要做好重试和降级处理
4. **数据验证**：AI返回的JSON需要严格验证，避免脏数据

## 🔮 后续优化方向

1. **需求提取优化**：使用Function Calling提取结构化需求
2. **计划预览**：转换前让用户预览计划
3. **错误恢复**：AI返回格式错误时的自动修复
4. **成本监控**：添加token使用量监控
5. **多模型支持**：支持切换不同的AI模型
