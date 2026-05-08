---
name: all-pages-real-api
overview: 将 11 个纯 Mock 或混合页面全部接入真实后端 API 数据，涉及 admin 后台 6 页 + 小程序前端 5 页。
todos:
  - id: phase-1a
    content: 前端 gallery/detail.vue：移除 loadMockData 降级逻辑，纯 API 模式
    status: completed
  - id: phase-1b
    content: admin OrderList.vue：接入 OrderAdminController 真实 API，映射 trade_order 字段
    status: completed
    dependencies:
      - phase-1a
  - id: phase-1c
    content: 前端 artist/home.vue：onLoad 调用 getArtistInfo API，替换硬编码数据
    status: completed
  - id: phase-1d
    content: 后端 PriceRuleConfig：暴露 GET/POST 接口 + admin 前端接入
    status: completed
  - id: phase-2a
    content: 新建 PurchaseIntentController+Service + admin/frontend 两端接入
    status: completed
  - id: phase-2b
    content: admin WorkList.vue：接入 ProductAdminController API
    status: completed
  - id: phase-3a
    content: 新建 CertificateController+Service + 前后端证书详情页接入
    status: completed
  - id: phase-3b
    content: 新建 CirculationController+Service + 前后端流通记录页接入
    status: completed
  - id: verify
    content: 全量编译验证 + API 冒烟测试
    status: completed
    dependencies:
      - phase-1a
      - phase-1b
      - phase-1c
      - phase-1d
      - phase-2a
      - phase-2b
      - phase-3a
      - phase-3b
---

将 11 个仍在使用模拟数据的页面全部切换为真实后端 API 数据。覆盖 admin 后台和前端小程序两端。

## 现状对比

| 模块 | 页面 | 状态 | 后端支持 |
| --- | --- | --- | --- |
| admin OrderList | 正式订单 | 纯 Mock | 已有 OrderAdminController + OrderService |
| admin WorkList | 作品管理 | 纯 Mock | 已有 product/artwork 表 |
| admin IntentList | 收藏意向单 | 纯 Mock | 需新建 API |
| admin CertificateList | 收藏证书 | 纯 Mock | 需新建 API |
| admin CirculationRecordList | 流通记录 | 纯 Mock | 需新建 API |
| admin PriceRuleConfig | 涨价规则 | 纯 Mock | 已有 PriceGrowthService |
| frontend artist/home | 艺术家主页 | 纯 Mock | 已有 getArtistInfo API |
| frontend cert-detail | 证书详情 | 纯 Mock | 需新建 API |
| frontend circulation-detail | 流通详情 | 纯 Mock | 需新建 API |
| frontend order/intent | 收藏意向单 | 纯 Mock | 需新建 API |
| frontend gallery/detail | 作品详情 | 混合模式 | 已有 getProductDetail API |


## 核心功能

- Admin 列表页接入真实数据库：统计数字 + 分页表格 + 筛选搜索
- 前端详情页按路由参数加载真实数据
- 意向单提交流程对接后端接口
- 涨价规则配置保存与加载对接 PriceGrowthService

## 技术方案

### 第一阶段：有现成后端支持（4 个页面，推荐先做）

#### 1. gallery/detail.vue（前端，已有 API）

- 现状：`loadMockData()` 作为后备逻辑，且有多处 `||` 兜底字符串
- 改动：移除 `loadMockData()`，由 `fetchDetail()` 直接调 `getProductDetail(id)`，api 失败时显示空状态而非降级 mock
- 注意：`loadCommission()` 已调真实 API，保留；`triggerPriceOnCollect()` 已调真实 API，保留

#### 2. admin OrderList.vue（前后端已有）

- 现状：纯 mock `refresh()` 只打 console.log
- 改动：接入现有 `request.get('/order/list', { params })`，映射 `trade_order` 表字段到页面所需字段（title/sub/statusText/amountText 等），统计卡数据用 `getOrderStats()`
- 后端 API：`GET /admin/order/list` + `GET /admin/order/stats`（已有）

#### 3. frontend artist/home.vue（前端已有 API）

- 现状：`data()` 中硬编码 artist/works/trustItems，`onLoad` 只取 ID 不调 API
- 改动：`onLoad` 调用 `getArtistInfo(this.artistId)` 填充 `artist`、`works`、`stats`，`trustItems` 保留为静态 UI 引用
- 后端 API：`GET /user/artist/{userId}`（已有）

#### 4. admin PriceRuleConfig.vue（部分已有）

- 现状：纯前端计算 + `saveConfig()` 只 alert
- 改动：新增 `admin/src/api/priceRule.js`，配置通过已有的 PriceGrowthService 暴露 API：`GET /admin/price-rule/get` + `POST /admin/price-rule/save`
- 后端改动：ProductAdminController 新增价格规则读取/保存接口（映射到 price_rule_config 或系统配置表）

### 第二阶段：新建少量后端（4 个页面）

#### 5-6. admin WorkList.vue + frontend artist/home.vue 作品部分

- 现状：WorkList 纯 mock；artist/home 作品列表硬编码
- 后端：`ProductAdminController` 已经存在，扩展 `GET /admin/product/list` 支持更多筛选参数
- 前端 admin：`admin/src/api/work.js` 封装列表/详情/操作 API
- 前端 artist/home 作品列表：使用 `getProductList({ authorId: artistId })` 替代硬编码

#### 7-8. admin IntentList.vue + frontend order/intent.vue

- 现状：均纯 mock
- 后端：需新建 `PurchaseIntentController` + `PurchaseIntentService`，操作 `purchase_intent` 表
- API 设计：
- `GET /admin/intent/list` - 后台意向单列表/统计
- `POST /admin/intent/confirm` - 顾问确认意向
- `POST /api/intent/create` - 前端提交收藏意向
- `GET /api/intent/{id}` - 前端获取意向详情

### 第三阶段：新建前后端（3 个页面）

#### 9-10. admin CertificateList.vue + frontend certificate-detail.vue

- 现状：均纯 mock
- 后端：需新建 `CertificateController` + `CertificateService`，操作 `collection_certificate` 表
- API 设计：
- `GET /admin/certificate/list` - 后台证书列表
- `GET /api/certificate/{id}` - 前端证书详情
- `POST /admin/certificate/generate` - 生成证书

#### 11. admin CirculationRecordList.vue + frontend circulation-detail.vue

- 现状：均纯 mock
- 后端：需新建 `CirculationController` + `CirculationService`，操作 `circulation_record` 表
- API 设计：
- `GET /admin/circulation/list` - 后台流通记录列表
- `GET /api/circulation/{artworkId}` - 前端流通详情
- `POST /admin/circulation/update` - 更新保管状态

### 数据流

```
admin 前端                     admin 后端
┌───────────────┐           ┌───────────────────┐
│ OrderList.vue  │──GET────→│ OrderAdminController│──→ trade_order 表
│ WorkList.vue   │──GET────→│ ProductAdminContrlr│──→ artwork 表
│ IntentList.vue │──GET────→│ IntentController   │──→ purchase_intent 表
│ CertificateLst │──GET────→│ CertificateContrlr │──→ collection_certificate
│ CirculationLst │──GET────→│ CirculationContrlr │──→ circulation_record
│ PriceRuleCfg   │──GET/SAVE│ ProductAdminContrlr│──→ price_rule_config
└───────────────┘           └───────────────────┘

小程序前端 (Uni-App)          后端 API
┌───────────────┐           ┌───────────────────┐
│ artist/home   │──GET────→│ /user/artist/{id}  │
│ gallery/detail│──GET────→│ /product/detail/{id}│
│ order/intent  │──POST───→│ /intent/create     │
│ cert-detail   │──GET────→│ /certificate/{id}  │
│ circul-detail │──GET────→│ /circulation/{id}  │
└───────────────┘           └───────────────────┘
```

### 目录结构

```
admin/src/api/
├── request.js           [MODIFY] 无需修改，已有 baseURL=/api/admin
├── work.js              [NEW] 作品管理 API：getWorkList, updateWorkStatus
├── intent.js            [NEW] 意向单管理 API：getIntentList, confirmIntent
├── certificate.js       [NEW] 证书管理 API：getCertList, generateCert
├── circulation.js       [NEW] 流通记录 API：getCirculationList
└── priceRule.js         [NEW] 涨价规则 API：getPriceRule, savePriceRule

backend/shiyiju-admin/src/main/java/com/shiyiju/admin/
├── controller/
│   ├── PurchaseIntentController.java     [NEW] /admin/intent/**
│   ├── CertificateController.java        [NEW] /admin/certificate/**
│   └── CirculationController.java        [NEW] /admin/circulation/**
├── service/
│   ├── PurchaseIntentService.java        [NEW] 意向单 CRUD
│   ├── CertificateService.java           [NEW] 证书 CRUD
│   └── CirculationService.java           [NEW] 流通记录 CRUD

frontend/src/api/
├── order.js             [NEW] 前端订单/意向单 API：createIntent, getIntentDetail
└── certificate.js       [NEW] 前端证书 API：getCertDetail

admin/src/views/
├── work/WorkList.vue        [MODIFY] mock → API
├── order/IntentList.vue     [MODIFY] mock → API
├── order/OrderList.vue      [MODIFY] mock → API（已有后端）
├── certificate/CertificateList.vue [MODIFY] mock → API
├── work/CirculationRecordList.vue  [MODIFY] mock → API
├── system/PriceRuleConfig.vue      [MODIFY] mock → API

frontend/src/pages/
├── artist/home.vue             [MODIFY] mock → API
├── gallery/detail.vue           [MODIFY] 移除 mock 降级
├── order/certificate-detail.vue [MODIFY] mock → API
├── order/circulation-detail.vue [MODIFY] mock → API
└── order/intent.vue             [MODIFY] mock → API
```

# Agent Extensions

无。