# 登录状态频繁过期问题修复方案

## 问题诊断

### 根本原因分析

| 问题 | 现状 | 影响 |
|------|------|------|
| **无 Token 刷新机制** | 只在 401 时跳转登录页 | 用户操作被迫中断，体验差 |
| **无过期时间追踪** | 只存 token，不知何时过期 | 无法主动续期 |
| **粗暴重定向** | 401 直接跳转首页 | 用户丢失当前页面和操作 |
| **游客 token** | `guest_token` 是假 token | 触发 401，体验差 |
| **并发刷新冲突** | 多个请求同时触发刷新 | 可能导致 token 混乱 |

### 现有代码缺陷

1. **request.js** (原方案)
   - 只在 401 响应时处理，不主动检测
   - 401 时直接 `navigateTo` 登录页，丢失路由
   - 无重试机制处理 token 刷新

2. **store/modules/user.js**
   - 只存储 token 字符串，无过期时间
   - 无游客模式特殊处理

3. **login/index.vue**
   - `afterLogin()` 只跳首页，不支持路由恢复

---

## 修复方案

### 1. 新增 `utils/auth.js` - Token 管理模块

```javascript
// 核心功能：
├── Token 存储结构化（含过期时间）
├── Token 有效性检测
├── 无感刷新机制（即将过期自动续期）
├── 并发刷新防抖（避免多个请求同时刷新）
├── 路由恢复（登录后回到原页面）
└── 游客模式识别
```

**关键常量：**
```javascript
const TOKEN_EXPIRE_BUFFER = 5 * 60 * 1000  // 提前 5 分钟刷新
const REFRESH_COOLDOWN = 60 * 1000        // 刷新冷却 60 秒
```

### 2. 重写 `api/request.js` - 智能请求封装

**新增特性：**

```javascript
// 1. Token 有效性预检
checkAndRefreshToken()  // 发起请求前检测并自动刷新

// 2. 401 时智能重试
if (401 && !isGuest) {
  await tryRefreshToken()  // 先尝试刷新
  if (newToken) {
    resolve(requestWithRetry(options, retryCount + 1))  // 重试
  } else {
    handleUnauthorized()  // 刷新失败才跳转
  }
}

// 3. 防抖机制
let isHandling401 = false  // 防止重复触发

// 4. 路由保存与恢复
handleUnauthorized() {
  saveRedirectUrl(currentPath)  // 保存当前页面
  uni.navigateTo({ url: '/pages/login/index' })
}
```

### 3. 更新 `store/modules/user.js` - 增强状态管理

```javascript
state: {
  tokenData: getTokenData(),  // 完整的 token 数据（含过期时间）
  ...
}

actions: {
  setTokenWithExpiry(token, expiresAt, userId) { ... },
  onLoginSuccess(token, userInfo) { ... }  // 集成 auth 模块
}
```

### 4. 新增后端接口 `POST /user/auth/refresh`

**功能：** 根据旧 token 生成新 token

```java
@PostMapping("/auth/refresh")
public Result<LoginVO> refreshToken(
    @RequestHeader("Authorization") String authHeader
) {
    String oldToken = authHeader.substring(7);
    LoginVO vo = userService.refreshToken(oldToken);
    return vo != null ? Result.success(vo) : Result.fail(401, "Token已过期");
}
```

### 5. 更新 `login/index.vue` - 支持路由恢复

```javascript
afterLogin() {
  // 1. 优先使用 URL 参数指定的跳转
  if (this.redirect) {
    uni.navigateTo({ url: this.redirect })
    return
  }
  
  // 2. 尝试从 auth 模块恢复之前保存的重定向 URL
  const savedRedirect = getAndClearRedirectUrl()
  if (savedRedirect) {
    uni.navigateTo({ url: savedRedirect })
    return
  }
  
  // 3. 默认跳首页
  uni.switchTab({ url: '/pages/index/index' })
}
```

---

## 修复后的用户流程

### 场景1：Token 即将过期

```
用户操作 → 请求前检测到 token 即将过期 → 自动刷新 token → 重试请求 → 操作继续（无感知）
```

### 场景2：Token 已过期

```
用户操作 → 请求返回 401 → 尝试刷新 → 刷新成功 → 重试请求 → 操作成功
```

### 场景3：游客模式

```
用户以游客身份浏览 → 尝试购买 → 检测到 isGuest → 跳转登录页（不触发 401）
```

### 场景4：登录过期后恢复

```
用户在某页面操作 → token 过期 → 401 → 保存当前页面路径 → 跳转登录
→ 登录成功 → 自动回到原页面 → 继续操作
```

---

## 文件变更清单

### 新增文件
- `frontend/src/utils/auth.js` - Token 管理模块

### 修改文件
| 文件 | 变更 |
|------|------|
| `frontend/src/api/request.js` | 重写，集成智能 token 管理 |
| `frontend/src/store/modules/user.js` | 增强，支持 token 过期时间 |
| `frontend/src/pages/login/index.vue` | 支持路由恢复 |
| `frontend/src/api/user.js` | 添加 refreshToken API |
| `backend/shiyiju-user/.../UserController.java` | 添加 refreshToken 接口 |
| `backend/shiyiju-user/.../UserService.java` | 实现 refreshToken 方法 |

---

## 测试建议

### 功能测试
1. 登录后 token 是否正确存储（含过期时间）
2. Token 即将过期时是否自动刷新
3. 401 时是否正确处理（刷新 vs 跳转）
4. 登录页跳转后是否正确恢复原页面
5. 游客模式是否正确识别（不触发 401）

### 边界测试
1. 多个请求同时触发 401 是否只刷新一次
2. Token 刷新失败时是否正确跳转登录
3. 网络异常时重试机制是否正常工作
4. 并发刷新时是否正确防抖

---

## 相关配置

### 后端 Token 有效期
```java
// JwtUtil.java
private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;  // 7 天
```

### 前端刷新策略
```javascript
// auth.js
const TOKEN_EXPIRE_BUFFER = 5 * 60 * 1000  // 提前 5 分钟刷新
const REFRESH_COOLDOWN = 60 * 1000         // 60 秒冷却
```
