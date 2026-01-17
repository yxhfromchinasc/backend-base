# 数据库表创建脚本说明

## 文件说明

本目录包含所有数据表的创建SQL脚本：

1. **all_tables.sql** - 所有表的完整创建脚本（推荐使用）
2. **user.sql** - 用户表
3. **role.sql** - 角色表
4. **permission.sql** - 权限表
5. **system_config.sql** - 系统配置表
6. **itinerary.sql** - 计划表
7. **itinerary_alter.sql** - 计划表迁移脚本（表已存在时使用）

## 快速开始

### 方式1：一次性创建所有表（推荐）

直接执行 `all_tables.sql` 文件，它会按正确的顺序创建所有表：

```bash
mysql -u root -p lvxin < all_tables.sql
```

或者在MySQL客户端中：

```sql
source all_tables.sql;
```

### 方式2：逐个创建表

如果需要单独创建某个表，可以执行对应的SQL文件：

```bash
# 创建用户表
mysql -u root -p lvxin < user.sql

# 创建权限表
mysql -u root -p lvxin < permission.sql

# 创建角色表
mysql -u root -p lvxin < role.sql

# 创建系统配置表
mysql -u root -p lvxin < system_config.sql

# 创建计划表
mysql -u root -p lvxin < itinerary.sql
```

## 表结构说明

### 1. user（用户表）
- 存储用户基本信息
- 通过 `roles` 字段（JSON格式）存储用户角色
- `phone` 和 `username` 字段有唯一约束

### 2. permission（权限表）
- 存储系统权限点
- `permission_code` 字段有唯一约束
- 格式：模块:操作，如 `user:create`、`system:view`

### 3. role（角色表）
- 存储角色信息
- 通过 `permissions` 字段（JSON格式）存储角色拥有的权限点
- `role_code` 字段有唯一约束
- `is_system` 标识系统角色（不可修改或删除）

### 4. system_config（系统配置表）
- 存储系统配置（key-value结构）
- `config_key` 字段有唯一约束
- 支持多种配置类型：string、number、boolean、json

### 5. itinerary（计划表）
- 存储用户的行程计划表
- 通过 `itinerary_key` 和 `version` 复合唯一键支持版本管理
- `is_current_version` 标识当前使用版本
- 外键关联 `user` 表

## 注意事项

1. **执行顺序**：如果使用 `all_tables.sql`，表会按正确的依赖顺序创建
2. **外键约束**：`itinerary` 表有外键关联 `user` 表，确保先创建 `user` 表
3. **字符集**：所有表使用 `utf8mb4` 字符集，支持emoji等特殊字符
4. **软删除**：`itinerary` 表支持软删除（`is_deleted` 字段）
5. **版本管理**：`itinerary` 表支持多版本存储，通过 `is_current_version` 标识当前版本

## 验证

执行完SQL后，可以使用以下命令验证：

```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESC user;
DESC role;
DESC permission;
DESC system_config;
DESC itinerary;

-- 查看表的索引
SHOW INDEX FROM itinerary;
```

## 迁移说明

如果表已存在，需要迁移：
- 计划表迁移：参考 `itinerary_alter.sql` 或 `EXECUTE_SQL.md`
- 其他表：如果字段结构有变化，需要手动编写迁移SQL
