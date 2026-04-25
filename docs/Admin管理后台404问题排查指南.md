# Admin管理后台 404问题排查指南

## 问题症状
艺术家认证列表、艺荐官管理列表等页面显示404，数据加载失败。

## 快速排查步骤

### 1. 检查后端服务是否运行
```bash
# 检查Admin服务（8090）
curl http://localhost:8090/admin/user/artist/list?page=1&size=5

# 检查网关（8080）
curl http://localhost:8080/api/admin/user/artist/list?page=1&size=5

# 检查前端代理（5174）
curl http://localhost:5174/api/admin/user/artist/list?page=1&size=5
```

### 2. 检查服务进程
```bash
ps aux | grep -E "(8090|8080|5174)" | grep -v grep
```

### 3. 检查网关路由是否加载
```bash
grep "RouteDefinition.*admin" /path/to/logs/gateway.log
```
确保能看到 `shiyiju-admin`、`shiyiju-admin-artist`、`shiyiju-admin-promoter` 等路由。

---

## 常见问题及解决方案

### 问题一：API路径重复（已修复）

**症状**：请求变成 `/api/admin/api/admin/user/artist/list`

**原因**：
- 前端 `request.js` 中 `baseURL` 设置为 `/api/admin`
- 组件调用时又写了 `/api/admin/user/artist/list`
- 导致实际请求路径为 `/api/admin` + `/api/admin/user/artist/list`

**解决方案**：
修改组件中的API调用，移除多余的 `/api/admin` 前缀：

```javascript
// 错误写法（会导致路径重复）
const data = await request.get('/api/admin/user/artist/list', { params })

// 正确写法
const data = await request.get('/user/artist/list', { params })
```

**涉及文件**：
- `admin/src/views/user/artist.vue`
- `admin/src/views/user/promoter.vue`
- 其他使用 `/api/admin/xxx` 的组件

---

### 问题二：网关路由未加载

**症状**：网关日志中没有admin路由的加载记录

**原因**：
- `application-local.yml` 覆盖了主配置文件
- 主配置文件的路由没有同步到local配置

**解决方案**：
将admin相关路由添加到 `application-local.yml`：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ... 其他路由 ...

        # 运营后台 - 带 /api 前缀的管理接口
        - id: shiyiju-admin
          uri: http://localhost:8090
          predicates:
            - Path=/api/admin/**
          filters:
            - RewritePath=/api/admin(?<segment>/?.*), /admin$\{segment}
        - id: shiyiju-admin-artist
          uri: http://localhost:8090
          predicates:
            - Path=/user/artist/list,/user/artist/approve,/user/artist/reject
          filters:
            - RewritePath=/user/artist(?<segment>/?.*), /admin/user/artist$\{segment}
        - id: shiyiju-admin-promoter
          uri: http://localhost:8090
          predicates:
            - Path=/user/promoter/list,/user/promoter/add,/user/promoter/freeze
          filters:
            - RewritePath=/user/promoter(?<segment>/?.*), /admin/user/promoter$\{segment}
```

**涉及文件**：
- `backend/shiyiju-gateway/src/main/resources/application-local.yml`

---

### 问题三：前端代理未配置

**症状**：前端Vite开发服务器返回404

**原因**：
- `vite.config.js` 中没有配置 `/api/admin` 的代理

**解决方案**：
在 `vite.config.js` 中添加代理配置：

```javascript
server: {
  port: 5174,
  proxy: {
    // 管理员接口 -> admin服务(8090)
    '/api/admin': {
      target: 'http://localhost:8090',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api\/admin/, '/admin')
    },
    // 其他代理...
  }
}
```

**涉及文件**：
- `admin/vite.config.js`

---

### 问题四：服务未启动

**症状**：连接被拒绝或超时

**解决方案**：
```bash
# 重启Admin服务
cd backend/shiyiju-admin
mvn clean package -DskipTests -q
java -jar target/shiyiju-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=local &

# 重启网关
cd backend/shiyiju-gateway
mvn clean package -DskipTests -q
java -jar target/shiyiju-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active=local &

# 重启前端
cd admin
npx vite --host 0.0.0.0 --port 5174 &
```

---

## 排查流程图

```
1. 浏览器F12检查Network请求
   ↓ 请求URL是什么？
   ├─ /api/admin/api/admin/... → 问题一：路径重复
   ├─ /api/admin/... → 继续检查
   │
2. curl测试各端口
   ├─ 5174返回200 → 前端代理正常，继续3
   ├─ 5174返回404 → 问题三：代理未配置
   │
3. curl测试网关
   ├─ 8080返回200 → 网关正常，继续4
   ├─ 8080返回404 → 问题二：路由未加载
   │
4. curl测试Admin服务
   ├─ 8090返回200 → 服务正常
   ├─ 连接拒绝 → 问题四：服务未启动
```

---

## 相关文件路径

### 后端
- `backend/shiyiju-admin/` - Admin管理服务
- `backend/shiyiju-gateway/` - API网关
- `backend/shiyiju-user/` - 用户服务

### 前端
- `admin/src/api/request.js` - 请求封装（baseURL配置）
- `admin/src/views/user/artist.vue` - 艺术家认证页面
- `admin/src/views/user/promoter.vue` - 艺荐官管理页面
- `admin/vite.config.js` - Vite配置

### 配置文件
- `backend/shiyiju-gateway/src/main/resources/application.yml` - 网关主配置
- `backend/shiyiju-gateway/src/main/resources/application-local.yml` - 网关本地配置

---

## 维护记录

| 日期 | 问题 | 解决方案 | 状态 |
|------|------|----------|------|
| 2026-04-25 | API路径重复导致404 | 修改组件中的API调用路径，移除多余的/api/admin前缀 | 已修复 |
| 2026-04-25 | application-local.yml未包含admin路由 | 添加所有admin相关路由配置 | 已修复 |
