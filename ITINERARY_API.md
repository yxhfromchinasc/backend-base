# 计划表API使用说明

## 概述

计划表模块提供了完整的CRUD功能，支持用户创建、查询、更新和删除自己的行程计划表。所有接口都需要用户登录（通过Token认证）。

## 数据库表结构

### itinerary 表

执行以下SQL脚本创建数据表：

```sql
-- 文件位置：src/main/resources/sql/itinerary.sql
```

表结构说明：
- `id`: 主键，自增
- `itinerary_id`: 计划表唯一标识（UUID），前端使用
- `user_id`: 用户ID，关联user表
- `title`: 计划表标题
- `cover_image_url`: 封面图片URL（可选）
- `json_data`: 计划表JSON数据（LONGTEXT）
- `version`: 用户版本号（每次保存时递增）
- `component_version`: 组件版本号（如v1、v2）
- `is_current_version`: 是否是当前使用版本（0-历史版本，1-当前版本）
- `created_at`: 创建时间
- `updated_at`: 更新时间
- `is_deleted`: 软删除标记（0-未删除，1-已删除）

## API接口

### 1. 创建计划表

**接口地址**: `POST /api/itinerary/create`

**请求头**:
```
Authorization: {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "title": "杭州三日游",
  "coverImageURL": "https://example.com/cover.jpg",
  "jsonData": "{...完整的计划表JSON...}"
}
```

**响应**:
```json
{
  "success": true,
  "code": 200,
  "message": null,
  "data": {
    "itineraryId": "uuid-string",
    "userId": 1,
    "title": "杭州三日游",
    "coverImageURL": "https://example.com/cover.jpg",
    "jsonData": "{...}",
    "version": 1,
    "componentVersion": "v1",
    "isCurrentVersion": 1,
    "createdAt": "2024-01-17 10:00:00",
    "updatedAt": "2024-01-17 10:00:00"
  }
}
```

### 2. 获取计划表详情

**接口地址**: `GET /api/itinerary/detail?itineraryId={itineraryId}`

**请求头**:
```
Authorization: {token}
```

**响应**: 同创建接口的data字段

### 3. 更新计划表

**接口地址**: `POST /api/itinerary/update?itineraryId={itineraryId}`

**请求头**:
```
Authorization: {token}
Content-Type: application/json
```

**请求体**（所有字段可选，只传需要更新的字段）:
```json
{
  "title": "杭州三日游（更新）",
  "coverImageURL": "https://example.com/new-cover.jpg",
  "jsonData": "{...更新后的JSON...}"
}
```

**注意**: 如果更新了`jsonData`，版本号会自动递增。

**响应**: 同创建接口的data字段

### 4. 删除计划表

**接口地址**: `POST /api/itinerary/delete?itineraryId={itineraryId}`

**请求头**:
```
Authorization: {token}
```

**响应**:
```json
{
  "success": true,
  "code": 200,
  "message": null,
  "data": null
}
```

### 5. 获取计划表列表（分页）

**接口地址**: `GET /api/itinerary/list?pageNum=1&pageSize=20`

**请求头**:
```
Authorization: {token}
```

**响应**:
```json
{
  "success": true,
  "code": 200,
  "message": null,
  "data": {
    "list": [
      {
        "itineraryId": "uuid-string",
        "title": "杭州三日游",
        "coverImageURL": "https://example.com/cover.jpg",
        "version": 1,
        "componentVersion": "v1",
        "isCurrentVersion": 1,
        "updatedAt": "2024-01-17 10:00:00"
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 20
  }
}
```

### 6. 获取计划表列表（全部）

**接口地址**: `GET /api/itinerary/list-all`

**请求头**:
```
Authorization: {token}
```

**响应**:
```json
{
  "success": true,
  "code": 200,
  "message": null,
  "data": [
    {
      "itineraryId": "uuid-string",
      "title": "杭州三日游",
      "coverImageURL": "https://example.com/cover.jpg",
      "version": 1,
      "componentVersion": "v1",
      "updatedAt": "2024-01-17 10:00:00"
    }
  ]
}
```

## 版本管理

- **用户版本号（version）**: 每次更新`jsonData`时自动递增，用于追踪用户的编辑历史
- **组件版本号（componentVersion）**: 标识生成该计划表的组件版本，用于兼容性检查
- **是否是当前使用版本（isCurrentVersion）**: 
  - `1` - 当前使用版本（默认）
  - `0` - 历史版本
  - 新创建的计划表默认为当前使用版本
  - 更新计划表时，更新后的版本自动成为当前使用版本
  - 查询接口默认只返回当前使用版本的计划表

## 权限控制

- 所有接口都需要通过`@AuthCheck`注解进行登录校验
- 用户只能操作自己的计划表（通过`userId`进行权限校验）
- 尝试访问其他用户的计划表会返回403错误

## 前端集成

### iOS端

已创建`APIService.swift`文件，提供了完整的API调用封装：

```swift
// 创建计划表
let response = try await APIService.shared.createItinerary(
    title: "杭州三日游",
    coverImageURL: "https://example.com/cover.jpg",
    jsonData: jsonString
)

// 获取计划表详情
let detail = try await APIService.shared.getItineraryDetail(itineraryId: "uuid")

// 更新计划表
let updated = try await APIService.shared.updateItinerary(
    itineraryId: "uuid",
    title: "新标题",
    coverImageURL: nil,
    jsonData: updatedJsonString
)

// 删除计划表
try await APIService.shared.deleteItinerary(itineraryId: "uuid")

// 获取列表
let list = try await APIService.shared.getAllItineraries()
```

### Token配置

目前iOS端的`APIService.swift`中使用了占位符token：
```swift
private let mockToken = "MOCK_TOKEN_PLACEHOLDER"
```

**TODO**: 后续需要替换为实际的登录token。

### 后端地址配置

目前iOS端的`APIService.swift`中使用了本地地址：
```swift
private let baseURL = "http://localhost:8080/api"
```

**TODO**: 生产环境需要替换为实际的后端服务器地址。

## 错误处理

所有接口统一返回格式：
```json
{
  "success": false,
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

常见错误码：
- `400`: 请求参数错误
- `401`: 未授权（Token无效或过期）
- `403`: 无权限（尝试访问其他用户的计划表）
- `404`: 资源不存在
- `500`: 服务器内部错误

## 注意事项

1. **软删除**: 删除操作是软删除，数据不会真正从数据库中删除，只是标记`is_deleted=1`
2. **版本递增**: 只有更新`jsonData`时才会递增版本号，更新`title`或`coverImageURL`不会递增版本
3. **当前使用版本**: 
   - 新创建的计划表默认`is_current_version=1`（当前使用版本）
   - 更新计划表时，更新后的版本自动设置为`is_current_version=1`
   - 查询接口默认只返回`is_current_version=1`的计划表
4. **JSON数据**: `jsonData`字段存储完整的计划表JSON，包括所有days、blocks等信息
5. **唯一标识**: `itinerary_id`是UUID格式，前端使用此字段作为计划表的唯一标识

## 数据库迁移

如果表已存在，需要执行以下SQL添加新字段：

```sql
-- 文件位置：src/main/resources/sql/itinerary_alter.sql
```
