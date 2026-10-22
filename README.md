# 旅心基础服务系统 (Lvxin Backend)

## 项目简介

旅心基础服务系统是一个独立的基础服务项目，提供权限管理、配置中心、认证授权等核心基础模块，可作为其他业务项目的依赖基础。

## 核心模块

### 1. 权限管理模块 (RBAC)
- **PermissionService** - 权限点管理
- **RoleService** - 角色管理
- **AuthCheck** 注解 - 权限校验注解
- **AuthCheckAspect** - 权限校验切面

### 2. 认证授权模块
- **TokenService** - JWT Token管理
- **LoginService** - 登录服务（支持多种登录方式）
- **CurrentUserUtil** - 当前用户工具类

### 3. 配置中心模块
- **SystemConfigService** - 系统配置管理（Key-Value结构）
- 支持公开/私有配置
- 配置缓存机制

### 4. 用户基础模块
- **UserService** - 用户基础管理
- 用户注册、登录、权限管理

### 5. 通用模块
- **Result** - 统一返回结果
- **PageReq/PageResult** - 分页支持
- **GlobalExceptionHandler** - 全局异常处理
- **ErrorCode** - 错误码定义

### 6. 工具类
- JsonUtil、DateUtil、MD5Util、HttpUtil等通用工具

### 7. 基础设施
- 数据库配置（MyBatis-Plus）
- Redis配置
- 日志配置
- 链路追踪配置

## 技术栈

- Spring Boot 3.2.7
- Java 21
- MyBatis-Plus 3.5.5
- MySQL
- Redis
- JWT

## 项目结构

```
lvxin-backend/
├── src/main/java/com/lvxin/base/
│   ├── common/              # 通用模块
│   ├── exception/           # 异常处理
│   ├── util/                # 工具类
│   ├── config/              # 配置类
│   ├── constant/            # 常量定义
│   ├── repository/          # 数据访问层
│   │   ├── entity/          # DO实体
│   │   └── *.java          # Repository接口
│   ├── service/             # 业务逻辑层
│   │   ├── bo/             # BO对象
│   │   └── impl/           # Service实现
│   └── web/                # Web层
│       ├── annotation/     # 自定义注解
│       ├── aspect/         # AOP切面
│       └── controller/     # 控制器
└── src/main/resources/
    ├── application.yml     # 配置文件
    └── mapper/            # MyBatis XML映射
```

## 快速开始

### 1. 数据库配置

修改 `application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lvxin?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: ${DB_PASSWORD}
```

### 2. Redis配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 运行项目

```bash
mvn spring-boot:run
```

## 数据库表结构

需要创建以下基础表：

- `user` - 用户表
- `role` - 角色表
- `permission` - 权限表
- `user_role` - 用户角色关联表
- `system_config` - 系统配置表

## 注意事项

⚠️ **重要提示**：

1. 本项目已完全移除所有业务特定模块（订单、支付、微信等），仅保留基础服务模块
2. 配置文件中的第三方服务配置（如飞书、COS等）可根据实际需求进行配置
3. 数据库表结构需要根据实际需求进行初始化

## License

Copyright © 2024 旅心团队


## 部署凭据配置

仓库中的凭据由运行环境注入，请在部署平台或本地未跟踪的环境配置中设置：

- `AMAP_API_KEY`
- `CLS_SECRET_ID`
- `CLS_SECRET_KEY`
- `CLS_TOPIC_ID`
- `DB_PASSWORD`
- `DEEPSEEK_API_KEY`
- `REDIS_PASSWORD`
- `SECURITY_TOKEN_SECRET`

不要将实际凭据提交到 Git。历史中出现过的凭据应在对应服务端撤销并重新生成。
