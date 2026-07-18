---
name: 重构登录注册页面
overview: 重构手机端登录与注册页面，优化UI布局与交互体验，实现登录与注册表单平滑切换，完善表单验证与错误处理，采用响应式设计兼容不同移动设备。
design:
  architecture:
    framework: vue
  styleKeywords:
    - Dark Luxury
    - Gold Accent
    - Smooth Transition
    - Minimalist
    - Responsive
  fontSystem:
    fontFamily: PingFang SC
    heading:
      size: 32rpx
      weight: 700
    subheading:
      size: 28rpx
      weight: 600
    body:
      size: 28rpx
      weight: 400
  colorSystem:
    primary:
      - "#D4AF37"
      - "#E8A838"
    background:
      - "#0D0D0D"
      - "#1A1A1A"
      - "#242424"
    text:
      - "#FFFFFF"
      - "#B3B3B3"
      - "#999999"
    functional:
      - "#FF6B6B"
      - "#4CAF50"
      - "#FFA500"
todos:
  - id: check-backend-register-api
    content: 检查后端是否已有注册API接口 `/user/register`，分析UserController.java
    status: completed
  - id: create-backend-register-api
    content: 如后端缺失注册接口，在UserController.java中创建POST /user/register接口，包含参数校验、验证码验证、用户创建、Token生成
    status: completed
    dependencies:
      - check-backend-register-api
  - id: update-user-service
    content: 在UserService.java中添加用户注册业务逻辑方法，包含手机号唯一性检查、密码加密(如有)、用户信息保存
    status: completed
    dependencies:
      - create-backend-register-api
  - id: refactor-login-vue
    content: 使用[skill:frontend-design]重构frontend/src/pages/login/index.vue，实现登录/注册Tab平滑切换、表单验证、倒计时、错误处理
    status: completed
    dependencies:
      - check-backend-register-api
  - id: add-register-api
    content: 在前端API文件frontend/src/api/user.js中添加register方法，调用后端注册接口
    status: completed
    dependencies:
      - check-backend-register-api
  - id: enhance-request-error-handling
    content: 增强frontend/src/api/request.js的网络异常处理，添加超时、网络错误、服务器错误的统一处理
    status: completed
  - id: test-login-register-flow
    content: 测试登录和注册完整流程，验证表单验证、倒计时、错误处理、响应式适配
    status: completed
    dependencies:
      - refactor-login-vue
      - add-register-api
      - update-user-service
---

## 产品概述

重构拾艺局手机端登录与注册页面，优化整体UI布局与交互体验，实现登录与注册表单的平滑切换。

## 核心功能

1. **登录/注册一体化设计** - 单一页面内通过Tab平滑切换登录和注册表单
2. **手机号验证登录** - 手机号+验证码登录，包含实时格式校验和60秒倒计时
3. **手机号注册** - 支持昵称+手机号+验证码注册流程
4. **微信登录集成** - 保留并优化微信一键登录功能
5. **游客模式** - 支持游客体验入口
6. **表单安全校验** - 前端实时验证 + 后端API校验双重保障
7. **响应式设计** - 使用rpx单位和safe-area适配不同移动设备
8. **完善的错误处理** - 网络异常、验证码错误、登录失败等场景的友好提示
9. **加载状态管理** - 按钮loading状态、请求状态反馈

## 技术栈确认

- **框架**: UniApp + Vue 2 (保持现有架构)
- **状态管理**: Pinia (已有 user store)
- **样式**: SCSS (保持现有暗色奢华主题)
- **网络请求**: 基于已有 request.js 封装
- **设计风格**: 暗色奢华 (#0D0D0D背景, #D4AF37金色强调色)

## 实施方案

### 1. 后端API检查与扩展

- 检查后端是否已有注册接口 `/user/register`
- 如不存在，在 `UserController.java` 中新增注册接口
- 注册API设计: `POST /user/register`，参数: `{ phone, code, nickname }`
- 后端需实现: 验证码校验、手机号唯一性检查、用户创建、Token生成

### 2. 前端页面重构 (`pages/login/index.vue`)

#### 页面结构设计

```
login-page (最小高度100vh)
├── hero-bg (背景图，绝对定位)
├── page-shade (渐变遮罩)
├── brand-section (品牌展示区)
│   ├── brand-mark (Logo容器)
│   └── brand-copy (应用名称 + Slogan)
├── form-container (表单容器 - 核心交互区)
│   ├── tab-bar (登录/注册切换标签)
│   │   ├── tab-item (登录 - 选中态金色下划线)
│   │   └── tab-item (注册 - 选中态金色下划线)
│   ├── form-wrapper (表单切换容器 - CSS transition)
│   │   ├── login-form (登录表单 - v-show控制)
│   │   │   ├── 手机号输入 (带图标 + 实时校验)
│   │   │   ├── 验证码输入 + 倒计时按钮
│   │   │   └── 登录按钮 (loading状态)
│   │   └── register-form (注册表单 - v-show控制)
│   │       ├── 昵称输入 (2-20字符校验)
│   │       ├── 手机号输入 (实时格式校验)
│   │       ├── 验证码输入 + 倒计时按钮
│   │       └── 注册按钮 (loading状态)
├── third-party-section (第三方登录区)
│   ├── 微信登录按钮
│   └── 游客登录按钮
└── agreement-footer (协议声明)
    └── 《用户协议》《隐私政策》链接
```

#### 核心功能实现细节

**Tab平滑切换**:

- 使用 `v-show` 控制表单显示，配合 CSS `transition: opacity 0.3s, transform 0.3s`
- 切换时添加 `transform: translateX` 滑动效果
- 下划线指示器使用 `:after` 伪元素 + `transition: left 0.3s`

**手机号格式校验**:

- 实时校验: `@input` 事件触发 `/^1[3-9]\d{9}$/` 正则
- 输入框边框颜色动态变化: 默认灰色 → 聚焦金色 → 错误红色
- 错误提示: 输入框下方显示红色文字，500ms后自动消失

**验证码倒计时**:

- 点击"获取验证码"后启动60秒倒计时
- 使用 `setInterval` 实现，组件销毁时 `clearInterval`
- 倒计时期间按钮禁用，显示剩余秒数
- 网络错误时清除倒计时，允许重新发送

**错误提示优化**:

- 统一错误处理函数 `handleLoginError(error)`
- 网络异常: "网络连接超时，请检查网络设置"
- 验证码错误: "验证码错误或已过期，请重新获取"
- 手机号未注册: "该手机号未注册，请先注册"
- 服务器错误: 显示后端返回的具体错误信息

**加载状态管理**:

- 按钮添加 `:loading="loading"` 属性
- 请求期间按钮禁用，显示loading图标
- 使用 `try-catch-finally` 确保loading状态正确重置

### 3. 网络异常处理增强

在 `api/request.js` 中增强拦截器:

```javascript
// 请求超时处理
timeout: 15000, // 15秒超时
timeoutErrorMessage: '网络连接超时，请检查网络',

// 响应拦截器增强
if (statusCode === 0) {
  // 网络错误
  return Promise.reject({ message: '网络连接失败，请检查网络设置' })
}
```

### 4. 响应式设计实现

**单位选择**:

- 全部使用 `rpx` 单位 (UniApp自适应)
- 字体大小: 24rpx - 34rpx 区间
- 间距: 使用 `28rpx` 倍数
- 安全区域: 使用 `env(safe-area-inset-bottom)` 和 `env(safe-area-inset-top)`

**不同设备适配**:

- 小屏 (iPhone SE): 品牌区高度缩小为 200rpx
- 中屏 (iPhone 12): 标准布局 310rpx
- 大屏 (iPhone 15 Pro Max): 增大间距至 40rpx
- 平板: 表单区域最大宽度 600rpx，居中显示

### 5. 表单数据安全校验

**前端校验**:

- 手机号: `/^1[3-9]\d{9}$/`
- 验证码: `/^\d{6}$/`
- 昵称: 2-20个字符，禁止特殊符号

**后端校验** (在 `UserController.java` 中):

- 使用 `@Valid` 注解 + Bean Validation
- 验证码正确性校验 (调用Redis验证)
- 手机号唯一性检查 (查询数据库)
- 请求频率限制 (同一手机号60秒内只能发送一次验证码)

### 6. 文件修改清单

| 文件路径 | 操作 | 说明 |
| --- | --- | --- |
| `backend/shiyiju-user/src/main/java/com/shiyiju/user/controller/UserController.java` | [MODIFY] | 新增注册接口 `/user/register` |
| `backend/shiyiju-user/src/main/java/com/shiyiju/user/service/UserService.java` | [MODIFY] | 新增用户注册业务逻辑 |
| `frontend/src/pages/login/index.vue` | [MODIFY] | 重构登录页面，集成注册表单 |
| `frontend/src/api/user.js` | [MODIFY] | 新增 `register` API调用 |
| `frontend/src/api/request.js` | [MODIFY] | 增强网络异常处理 |
| `frontend/src/styles/login.scss` | [NEW] | 登录页面专用样式 (可选，也可放在vue文件中) |


## 设计风格定位

**Dark Luxury (暗色奢华)** - 与拾艺局整体风格保持一致

### 色彩方案

- **主背景**: #0D0D0D (深黑)
- **卡片背景**: #1A1A1A / #242424
- **主强调色**: #D4AF37 (金色)
- **辅助强调色**: #E8A838 (暖金)
- **价格色**: #FF6B6B (红色)
- **文字主色**: #FFFFFF
- **文字次色**: #B3B3B3
- **文字占位符**: #999999
- **错误提示**: #FF6B6B
- **成功提示**: #4CAF50

### 页面布局设计 (自上而下)

#### 1. 品牌展示区 (顶部固定)

- **高度**: 310rpx (中屏标准)
- **Logo容器**: 118rpx × 118rpx，圆角32rpx，半透明背景
- **应用名称**: 52rpx，粗体800，白色
- **Slogan**: 24rpx，浅灰色 #B3B3B3
- **背景**: 博物馆图片 + 渐变遮罩 (radial-gradient金色光晕)

#### 2. 表单切换区 (中间核心)

- **Tab栏**: 
- 两个Tab: "登录" / "注册"
- 字体: 32rpx，粗体700
- 未选中: #B3B3B3
- 选中: #FFFFFF，底部2rpx金色下划线
- 下划线动画: transition: left 0.3s ease
- **表单容器**: 
- 背景: rgba(23, 23, 25, 0.94)，圆角18rpx
- 内边距: 24rpx
- 切换动画: opacity 0.3s + transform 0.3s

#### 3. 登录表单内容

1. **手机号输入**

- 容器高度: 88rpx，背景 #202024，圆角14rpx
- 左侧图标: 28rpx × 28rpx (电话图标)
- placeholder: "请输入手机号"，颜色 #999999
- 聚焦时边框: 1rpx solid #D4AF37
- 错误时边框: 1rpx solid #FF6B6B

2. **验证码输入 + 按钮**

- 容器: flex布局，gap 16rpx
- 输入框: flex:1，样式同手机号
- 按钮: 200rpx × 88rpx，背景 #D4AF37，圆角14rpx
- 禁用态: 背景 #343436，文字 #777166
- 字体: 25rpx，粗体700

3. **登录按钮**

- 尺寸: 100% × 92rpx
- 背景: #D4AF37，圆角46rpx
- 字体: 30rpx，粗体800，颜色 #16130B
- 禁用态: opacity 0.5
- loading: 旋转动画

#### 4. 注册表单内容

1. **昵称输入**

- 样式同手机号输入
- 左侧图标: 用户图标
- placeholder: "请输入昵称 (2-20字符)"
- 实时校验: 2-20字符，禁止特殊符号

2. **手机号输入** (同登录表单)
3. **验证码输入** (同登录表单)
4. **注册按钮**

- 样式同登录按钮
- 文字: "注册"

#### 5. 第三方登录区 (表单下方)

- **微信登录按钮**:
- 高度: 90rpx，背景 rgba(255,255,255,0.055)，圆角45rpx
- 边框: 1rpx solid rgba(255,255,255,0.16)
- 图标 + 文字: "微信登录"

- **游客登录按钮**:
- 高度: 72rpx，背景透明
- 文字: "游客体验"，颜色 #8F887E

#### 6. 协议声明 (最底部)

- 文字: 22rpx，颜色 #8F887E
- 链接: "《用户协议》" 和 "《隐私政策》"
- 链接颜色: #C9A227
- 位置: 固定底部，padding-bottom: env(safe-area-inset-bottom)

### 交互设计细节

#### 动画效果

1. **Tab切换动画**:

- 当前表单: opacity 1 → 0, transform: translateX(0) → (-30rpx)
- 目标表单: opacity 0 → 1, transform: translateX(30rpx) → 0
- 持续时间: 300ms，ease-in-out

2. **输入框焦点动画**:

- 边框颜色过渡: transition: border-color 0.2s
- 聚焦时轻微放大: transform: scale(1.02)

3. **按钮点击反馈**:

- 按下时: transform: scale(0.98)
- 松开恢复: transform: scale(1)

4. **错误提示动画**:

- 显示: opacity 0 → 1, margin-top: -10rpx → 0
- 自动消失: 500ms后 opacity → 0

#### 加载状态设计

- **按钮loading**: 使用uni的 `:loading` 属性
- **全局loading**: 请求期间显示 `uni.showLoading({ title: '登录中...' })`
- **超时处理**: 15秒后自动清除loading并显示错误提示

#### 错误提示设计

- **输入框错误**: 边框变红 + 下方显示红色文字 (22rpx)
- **Toast提示**: 使用 `uni.showToast({ icon: 'none', duration: 3000 })`
- **模态框**: 配置类错误使用 `uni.showModal()`

### 响应式适配方案

#### 小屏设备 (宽度 < 375px，如iPhone SE)

```
@media screen and (max-width: 375px) {
  .brand-section { min-height: 200rpx; }
  .app-name { font-size: 42rpx; }
  .feature-strip { gap: 8rpx; }
}
```

#### 大屏设备 (宽度 > 428px，如iPhone 15 Pro Max)

```
@media screen and (min-width: 428px) {
  .brand-section { min-height: 350rpx; }
  .form-container { max-width: 600rpx; margin: 0 auto; }
}
```

#### 平板设备 (宽度 > 768px)

```
@media screen and (min-width: 768px) {
  .login-page { padding: 64rpx 100rpx; }
  .form-container { max-width: 500rpx; }
}
```

#### 安全区域适配

```
.login-section {
  padding-bottom: calc(34rpx + env(safe-area-inset-bottom));
}
.phone-login-popup {
  padding-bottom: calc(34rpx + env(safe-area-inset-bottom));
}
```

## Agent Extensions

### Skill

- **frontend-design**: 用于创建高质量的前端界面设计，确保登录注册页面达到生产级别的视觉质量。
- 用途: 优化登录注册页面的UI设计，包括布局、配色、交互动效
- 预期成果: 生成具有视觉吸引力的暗色奢华风格登录注册界面

### Integration

- **tcb**: CloudBase集成，用于用户认证和数据存储(如果需要云端能力)
- 用途: 可选，如果需要将用户数据同步到云端
- 预期成果: 实现用户数据的云端存储和同步