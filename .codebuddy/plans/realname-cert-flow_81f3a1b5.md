---
name: realname-cert-flow
overview: 为拾艺局项目构建完整的实名认证流程，包含后端数据库/实体/Mapper/Service/Controller、前端对接真实API、管理后台审核页面。
todos:
  - id: create-entity-mapper
    content: 创建 RealnameCertification 实体类和 Mapper
    status: completed
  - id: add-schema-init
    content: 在 UserSchemaInitializer 中添加建表逻辑，users 表添加 real_name_verified 字段
    status: completed
    dependencies:
      - create-entity-mapper
  - id: add-service-methods
    content: 在 UserService 中添加实名认证的业务方法
    status: completed
    dependencies:
      - add-schema-init
  - id: add-controller-api
    content: 在 UserController 中添加实名认证的 REST 端点
    status: completed
    dependencies:
      - add-service-methods
  - id: add-frontend-api
    content: 在 frontend/src/api/user.js 中添加实名认证 API 方法
    status: completed
    dependencies:
      - add-controller-api
  - id: rewrite-realname-vue
    content: 重写 frontend/src/pages/user/realname.vue 对接后端 API
    status: completed
    dependencies:
      - add-frontend-api
  - id: update-settings-vue
    content: 修改 settings.vue 使用后端 API 获取认证状态
    status: completed
    dependencies:
      - add-frontend-api
  - id: create-admin-page
    content: 创建管理后台实名认证审核页面并注册路由
    status: completed
    dependencies:
      - add-controller-api
  - id: compile-deploy-verify
    content: 编译后端、重启服务并验证完整流程
    status: completed
    dependencies:
      - add-controller-api
      - rewrite-realname-vue
      - update-settings-vue
      - create-admin-page
---

## 实名认证流程

构建完整的实名认证系统，覆盖用户端提交认证、后台审核管理、状态同步三个环节。

### 用户端流程

1. 用户进入"实名认证"页面，填写真实姓名和身份证号（含本地校验位验证）
2. 上传身份证正反面照片
3. 完成人脸核验（本地模拟）
4. 提交认证申请，等待平台审核
5. 设置页显示认证状态（未认证/审核中/已认证/已拒绝）

### 后端功能

1. 认证申请提交接口：保存姓名、身份证号（脱敏存储）、证件照URL、审核状态
2. 认证状态查询接口：返回当前用户的认证状态和审核备注
3. 证件照上传接口：将图片存储到文件服务
4. 审核接口：管理员通过/拒绝认证申请

### 管理后台

1. 实名认证审核列表：分页展示待审核/已通过/已拒绝的申请记录
2. 审核操作：查看证件照片，通过或拒绝申请（含拒绝原因）
3. 状态同步：审核通过后更新用户表的实名认证标志位

## 技术栈

- **后端框架**: Spring Boot 3 + MyBatis Plus + JdbcTemplate (`shiyiju-user` 模块)
- **前端框架**: uni-app (Vue 3, Options API) + uni-ui
- **管理后台**: Vue 3 + Element Plus
- **数据库**: MySQL `shiyiju_local` (本地开发) / `shiyiju` (Docker)
- **认证方式**: 请求头 `X-User-Id`（网关注入）

## 实现方案

### 数据库设计

**新建表 `realname_certifications`**:

```sql
CREATE TABLE IF NOT EXISTS realname_certifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  real_name VARCHAR(100) DEFAULT NULL COMMENT '真实姓名',
  id_card VARCHAR(64) DEFAULT NULL COMMENT '身份证号（脱敏存储）',
  id_card_hash VARCHAR(128) DEFAULT NULL COMMENT '身份证号SHA256（查重用）',
  id_front_url VARCHAR(512) DEFAULT NULL COMMENT '身份证正面照URL',
  id_back_url VARCHAR(512) DEFAULT NULL COMMENT '身份证背面照URL',
  face_verified TINYINT DEFAULT 0 COMMENT '人脸核验状态',
  status INT DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
  reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
  review_time DATETIME DEFAULT NULL COMMENT '审核时间',
  reviewer_id BIGINT DEFAULT NULL COMMENT '审核人ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_status (status),
  UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

**修改 `users` 表**:

```sql
ALTER TABLE users ADD COLUMN real_name_verified TINYINT DEFAULT 0
```

### 后端架构

```
UserSchemaInitializer.ensureRealnameTable() -- 启动时自动创建
       |
RealnameCertification (Entity, @TableName)
       |
RealnameCertificationMapper (extends BaseMapper)
       |
UserService 新增方法:
  - submitRealnameCert(userId, dto)
  - getRealnameCertStatus(userId)
  - listRealnameCert(page, size, status)
  - approveRealnameCert(id, reviewerId)
  - rejectRealnameCert(id, reviewerId, reason)
       |
UserController 新增端点:
  - POST /user/realname/submit        -- 提交申请
  - GET  /user/realname/status        -- 查询状态
  - POST /user/realname/admin/list    -- 管理后台列表
  - POST /user/realname/admin/approve -- 审核通过
  - POST /user/realname/admin/reject  -- 审核拒绝
```

### 前端架构

- `realname.vue`: 重写提交逻辑，替换 localStorage 为 API 调用；onMounted 时调 `getRealnameCertStatus()` 回填状态
- `settings.vue`: onShow 时调 `getRealnameCertStatus()` 替代 localStorage
- `user.js`: 新增 `submitRealnameCert` / `getRealnameCertStatus` / `getRealnameAdminList` / `approveRealnameCert` / `rejectRealnameCert`
- 管理后台: 新增 `admin/src/views/user/realname.vue` + 路由注册

### 关键设计

1. **身份证号处理**: 提交时 SHA256 哈希存储用于去重；返回前端只展示脱敏版本（`410***********1234`）
2. **证件照上传**: 复用现有 `/api/file/upload` 接口
3. **重名冲突检测**: `id_card_hash` 防重复认证，已存在返回 409 错误
4. **状态流转**: 待审核(0) -> 通过(1) / 拒绝(2)；拒绝后可重新提交
5. **人脸核验**: 保持本地模拟，预留 `faceVerified` 字段
6. **权限控制**: 管理员接口校验 admin token

### 注意事项

- 身份证号禁止明文打印日志
- 表创建用 `CREATE TABLE IF NOT EXISTS`，与现有 `UserSchemaInitializer` 模式一致
- `settings.vue` 兼容旧版 localStorage 数据作为降级

## Agent Extensions

### SubAgent

- **code-explorer**: 在探索阶段已用于全面分析现有用户认证相关代码结构