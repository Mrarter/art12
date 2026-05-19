# 测试环境搭建指南

## 概览

测试环境用于使用真实微信账号进行集成测试，数据与生产环境隔离。
测试环境通过 `VITE_ENV=test` 标识，前端会自动显示红色「测试」浮标。

---

## 前提条件

| 项目 | 说明 |
|------|------|
| 微信小程序测试号 | 在 [微信公众平台](https://mp.weixin.qq.com) 申请测试号，获取测试 appid/secret |
| HTTPS 域名 | 测试环境必须使用 HTTPS，微信小程序要求合法域名 |
| 测试数据库 | 独立的 MySQL 数据库，与生产隔离 |
| 测试文件存储 | 独立的 COS Bucket 或本地存储目录 |

---

## 前端配置

### 1. 创建 `.env.test`（已创建）

```bash
# 测试环境 API 地址
VITE_API_BASE_URL=https://test-api.shiyiju.com/api
VITE_MP_GATEWAY_ORIGIN=https://test-api.shiyiju.com
VITE_MP_FILE_ORIGIN=https://test-file.shiyiju.com
VITE_ENV=test
VITE_ENV_LABEL=测试环境
```

### 2. 启动测试环境

```bash
# Vite 模式指定
npm run dev -- --mode test

# 或构建测试包
npm run build -- --mode test
```

### 3. 测试环境标识

- **红色浮标**：页面左上角显示闪烁「测试」标识（`TestEnvBadge.vue`）
- **点击浮标**：显示当前 API 地址
- **控制台日志**：红色醒目输出 `[测试环境]` 标记

---

## 后端配置

### application-test.yml

在 `backend/shiyiju-gateway/src/main/resources/` 创建：

```yaml
spring:
  datasource:
    url: jdbc:mysql://test-db-host:3306/shiyiju_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true
    username: test_user
    password: test_password

wechat:
  # 使用测试小程序的 appid 和 secret
  appid: wx_test_appid_xxxxx
  secret: test_secret_xxxxx

upload:
  local:
    path: /data/test-uploads
  cdn-url: https://test-file.shiyiju.com
```

### 启动后端测试环境

```bash
# 使用 test profile 启动
cd backend
mvn spring-boot:run -Dspring.profiles.active=test
```

---

## 微信登录流程（测试环境）

### 小程序端

```
用户点击「微信登录」
  → uni.login({ provider: 'weixin' })
    → 微信返回临时 code
  → POST /auth/wx-login { code, nickname, avatar }
    → 后端用测试 appid/secret 换取 openId
    → 查询/创建用户（测试数据库）
    → 生成 JWT Token
    → 存入 Redis（测试环境实例）
  → 保存 token → 登录完成
```

### 测试环境 vs 生产环境

| 维度 | 测试环境 | 生产环境 |
|------|---------|---------|
| 小程序 AppId | 测试号 | 正式号 |
| 数据库 | shiyiju_test | shiyiju_prod |
| 微信登录 | 测试号白名单用户 | 所有用户 |
| API 域名 | test-api.shiyiju.com | api.shiyiju.com |
| 文件存储 | 测试 COS Bucket | 生产 COS Bucket |
| 支付 | 沙箱环境 | 正式环境 |

---

## 数据隔离策略

### 1. 数据库隔离
测试环境使用独立的 MySQL 数据库 `shiyiju_test`，通过 `spring.profiles.active=test` 切换。

### 2. 用户隔离
- 测试号微信登录产生的用户仅在测试数据库
- 可通过修改 `application-test.yml` 的 `wechat.appid` 控制访问范围

### 3. 文件存储
测试环境文件上传至独立目录 `/data/test-uploads/` 或测试 COS Bucket。

### 4. Redis 隔离
建议测试环境使用独立的 Redis 实例或不同的 database index：
```yaml
spring:
  data:
    redis:
      database: 1  # 生产用 0，测试用 1
```

---

## 常见问题

### Q: 测试环境微信登录返回「appid 不匹配」
A: 检查 `wechat.appid` 配置是否为测试小程序的 AppId，且该 AppId 已配置白名单。

### Q: 测试环境 API 请求报 401
A: 检查 token 是否已过期。测试环境的 Redis 数据可能被定期清理，需重新登录。

### Q: 测试环境文件上传失败
A: 检查 `upload.local.path` 目录是否存在且有写入权限，或 COS 配置是否正确。

### Q: 如何清空测试数据？
A: 直接删除测试数据库并重建：
```sql
DROP DATABASE shiyiju_test;
CREATE DATABASE shiyiju_test DEFAULT CHARSET utf8mb4;
```
然后重新运行初始化 SQL。
