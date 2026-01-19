# 异步计划生成实现方案

## 设计概述

实现异步计划生成功能，用户触发转换时立即返回，后台异步执行计划生成。

## 数据库变更

### 1. 添加字段
- `status` VARCHAR(20): 计划状态（NORMAL-正常，GENERATING-生成中，FAILED-生成失败）
- `original_plan` LONGTEXT: 原始计划内容（用户输入的文本或AI对话的markdown）
- `error_message` VARCHAR(500): 错误信息（生成失败时记录）

执行SQL：`sql/itinerary_add_status.sql`

## 核心流程

### 1. 用户触发转换
```
用户点击"采纳并生成计划表"或"文本转计划表"
    ↓
后端立即创建计划（status=GENERATING, jsonData=null, originalPlan=原始内容）
    ↓
返回前端：{itineraryKey, status: "GENERATING", message: "任务生成中，请稍后在计划表查看"}
    ↓
开启异步任务执行生成
```

### 2. 异步任务执行
```
异步任务开始
    ↓
调用AI转换服务生成JSON
    ↓
更新计划：status=NORMAL, jsonData=生成的JSON
    ↓
如果失败：status=FAILED, errorMessage=错误信息
```

### 3. 前端处理
```
显示"生成中"状态
    ↓
轮询检查状态（每3-5秒）
    ↓
状态变为NORMAL：允许点击查看
    ↓
状态变为FAILED：显示错误信息，允许重试
```

## 实现要点

### 1. ItineraryService 新增方法
```java
// 创建生成中的计划（jsonData可以为空）
ItineraryBO createGeneratingItinerary(
    Long userId, 
    String title, 
    String originalPlan, 
    String coverImageUrl
);

// 更新计划状态和JSON数据
void updateItineraryStatus(
    String itineraryKey, 
    String status, 
    String jsonData, 
    String errorMessage
);
```

### 2. AIPlanningService 修改
```java
// 异步转换计划
@Async
void convertToItineraryAsync(String conversationId, String itineraryKey, Long userId);

// 异步文本转计划
@Async
void convertTextToItineraryAsync(String text, String itineraryKey, Long userId);
```

### 3. Controller 修改
- `convertToItinerary`: 立即创建计划，返回，异步执行
- `textToItinerary`: 立即创建计划，返回，异步执行

## 前端修改

### 1. 计划列表显示
- 显示状态标签（生成中/生成失败）
- 生成中的计划不可点击
- 生成失败的计划显示错误提示

### 2. 轮询机制
- 检测到GENERATING状态的计划时开始轮询
- 轮询间隔：3-5秒
- 最多轮询：60次（约3-5分钟）
- 状态变为NORMAL或FAILED时停止轮询

## 错误处理

1. **生成失败**：记录错误信息，状态设为FAILED
2. **超时处理**：超过5分钟未完成，自动标记为FAILED
3. **重试机制**：用户可以重新触发生成（使用originalPlan）

## 注意事项

1. 异步任务需要捕获异常，避免任务失败导致状态卡在GENERATING
2. 需要设置任务超时时间（建议5分钟）
3. 前端轮询需要限制次数，避免无限轮询
4. 考虑使用WebSocket推送状态更新（可选，后续优化）
