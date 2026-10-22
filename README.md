# Lvxin Backend Foundation

## Overview

Lvxin Backend is a standalone foundation service that provides permission management, configuration management, authentication, and authorization. It can serve as a shared foundation for other business applications.

## Core Modules

### 1. Role-Based Access Control (RBAC)

- **PermissionService** — Permission management
- **RoleService** — Role management
- **AuthCheck** — Annotation for permission checks
- **AuthCheckAspect** — Aspect for enforcing permission checks

### 2. Authentication and Authorization

- **TokenService** — JWT token management
- **LoginService** — Login service supporting multiple login methods
- **CurrentUserUtil** — Access to the current user context

### 3. Configuration Management

- **SystemConfigService** — Key-value configuration management
- Public and private configuration entries
- Configuration caching

### 4. User Management

- **UserService** — Core user management
- User registration, login, and permission management

### 5. Common Components

- **Result** — Standard API response format
- **PageReq / PageResult** — Pagination support
- **GlobalExceptionHandler** — Global exception handling
- **ErrorCode** — Error code definitions

### 6. Utilities

Shared utilities include `JsonUtil`, `DateUtil`, `MD5Util`, and `HttpUtil`.

### 7. Infrastructure

- Database integration with MyBatis-Plus
- Redis configuration
- Logging
- Request tracing

## Technology Stack

- Spring Boot 3.2.7
- Java 21
- MyBatis-Plus 3.5.5
- MySQL
- Redis
- JWT

## Project Structure

```text
lvxin-backend/
├── src/main/java/com/lvxin/base/
│   ├── common/              # Shared components
│   ├── exception/           # Exception handling
│   ├── util/                # Utility classes
│   ├── config/              # Configuration classes
│   ├── constant/            # Constants
│   ├── repository/          # Data access layer
│   │   ├── entity/          # Data objects
│   │   └── *.java           # Repository interfaces
│   ├── service/             # Business logic layer
│   │   ├── bo/              # Business objects
│   │   └── impl/            # Service implementations
│   └── web/                # Web layer
│       ├── annotation/     # Custom annotations
│       ├── aspect/         # Aspects
│       └── controller/     # Controllers
└── src/main/resources/
    ├── application.yml     # Application configuration
    └── mapper/             # MyBatis XML mappings
```

## Getting Started

### 1. Configure MySQL

Configure the database connection in `application.yml` or the active profile configuration. Supply credentials through environment variables:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/lvxin?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
```

### 2. Configure Redis

Configure Redis for your environment:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      password: ${REDIS_PASSWORD}
```

### 3. Initialize the Database

Create the required tables before starting the application:

- `user` — Users
- `role` — Roles
- `permission` — Permissions
- `user_role` — User-role associations
- `system_config` — System configuration

Initialize the schema according to the entity definitions and your deployment requirements.

### 4. Run the Application

```bash
mvn spring-boot:run
```

## Deployment Credentials

Provide the following environment variables as required by your active configuration:

- `CLS_SECRET_ID`
- `CLS_SECRET_KEY`
- `CLS_TOPIC_ID`
- `DB_HOST`
- `DB_PASSWORD`
- `DB_USERNAME`
- `REDIS_HOST`
- `REDIS_PASSWORD`
- `SECURITY_TOKEN_SECRET`

Store actual credentials in your deployment platform or an untracked local configuration. Never commit credentials to Git. Revoke and replace any credentials that previously appeared in repository history.

## Configuration Notes

1. Business-specific modules such as orders, payments, and WeChat integration have been removed; the project retains shared foundation services.
2. Configure optional third-party integrations, such as Feishu and Tencent Cloud COS, according to your needs.
3. Initialize the database schema for your environment before use.

## License

Copyright © 2024 Lvxin Team
