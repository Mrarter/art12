---
name: fix-collation-and-data-display
overview: 统一数据库表字符集校对规则，修复艺术家/作品列表"系统繁忙"报错，检查测试数据并确保后台和小程序前端都能正常显示
todos:
  - id: check-table-collations
    content: 使用 [subagent:code-explorer] 连接MySQL查询各表实际字符集校对规则，记录需要修复的表清单
    status: completed
  - id: fix-table-collations
    content: 执行 ALTER TABLE CONVERT 统一所有相关表字符集为 utf8mb4_unicode_ci
    status: completed
    dependencies:
      - check-table-collations
  - id: restart-services
    content: 重启 shiyiju-admin 后端服务（kill 旧进程后重新启动）
    status: completed
    dependencies:
      - fix-table-collations
  - id: verify-fix
    content: 使用 [subagent:code-explorer] 验证各列表接口（用户/艺术家/作品）是否正常返回数据
    status: completed
    dependencies:
      - restart-services
---

## 需求概述

修复因数据库表字符集校对规则不一致导致的后台和小程序前端数据无法显示的问题。

## 核心问题

1. **艺术家管理列表"系统繁忙"** - `artist_profile` 表（utf8mb4_unicode_ci）与 `users` 表（utf8mb4_0900_ai_ci）字符集不一致，LEFT JOIN 查询报错
2. **后台作品列表看不到数据** - `ProductAdminPersistenceService.listProducts()` 的多表 JOIN 同样受字符集冲突影响
3. **用户列表可能看不到测试数据** - 需确认测试数据存储位置是否正确
4. **小程序前端空数据** - 前端调用后端接口，间接受相同问题影响（shiyiju-product 模块使用 MyBatis-Plus 单表查询，不受影响）

## 修复目标

1. 统一所有相关数据库表的字符集校对规则为 utf8mb4_unicode_ci
2. 确保所有 JOIN 查询不再报错
3. 验证修复后后台和小程序前端数据正常显示

## 技术方案

### 技术栈

- 数据库: MySQL 8.0
- 后端框架: Spring Boot 3.1.5 + MyBatis + JDBC Template
- 管理后台: Vue.js (admin/)
- 小程序前端: uni-app (frontend/)

### 实现方案

#### 方案选择：ALTER TABLE CONVERT TO 统一字符集

直接在 MySQL 中执行 ALTER TABLE 语句，将相关表的字符集校对规则统一为 `utf8mb4_unicode_ci`（与项目 SQL 初始化脚本一致，如 `/backend/sql/init_database.sql` 第9行）。

**理由**：

1. 问题根源是部分表在创建时使用了 MySQL 8.0 默认字符集（utf8mb4_0900_ai_ci），与 SQL 脚本指定的 `utf8mb4_unicode_ci` 不一致
2. `ALTER TABLE ... CONVERT TO CHARACTER SET` 可安全转换现有数据，不丢失内容
3. 修正后的字符集与数据库初始化脚本一致，保证长期一致性
4. 无需修改任何 Java 代码，最小侵入性
5. 不涉及 SQL 注入或数据丢失风险，操作可回滚

#### 涉及的查询（不需改代码，但需确认修复后正常运行）

| 查询方法 | 文件位置 | JOIN 关系 | 当前状态 |
| --- | --- | --- | --- |
| `listArtists()` | UserAdminPersistenceService.java:585-720 | artist_profile LEFT JOIN users | **已确认报错** |
| `listProducts()` | ProductAdminPersistenceService.java:811-1001 | artwork + artist_profile + users + artist_certifications + artwork_category | **可能报错** |
| shiyiju-product 模块 | ProductService.java | 单表 MyBatis-Plus | **不受影响** |


### 执行步骤

1. **探查实际字符集** - 连接 MySQL 查询各表实际使用的字符集校对规则
2. **执行 ALTER TABLE 修复** - 统一所有相关表的 collation 为 utf8mb4_unicode_ci
3. **重启后端服务** - 使修改生效
4. **验证修复效果** - 测试各列表接口是否正常返回数据

### 性能与可靠性

- ALTER TABLE CONVERT 会在表上加元数据锁，但小表（百/千行级别）执行时间极短
- 建议在业务低峰期执行
- 操作前可备份表结构作为回滚预案

## Agent Extensions

### SubAgent

- **code-explorer**
- 用途：探查数据库表实际字符集状态，以及确认受影响的代码范围
- 预期结果：获取每张表的实际字符集校对规则，确认修复目标

- **code-explorer**（第二次使用）
- 用途：修复后验证各接口是否正常返回数据
- 预期结果：确认用户列表、艺术家列表、作品列表接口返回正常