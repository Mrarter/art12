---
name: fix-artwork-price-logs
overview: 修复管理后台作品编辑弹窗中的"涨价记录"不显示的问题，需要修复网关路由和让 PriceGrowthService 写入涨价日志
todos:
  - id: fix-gateway-routes
    content: 在 application.yml 和 application-local.yml 中添加 /api/admin/artwork/price/** 路由到 product 服务
    status: completed
  - id: fix-pricegrowth-logging
    content: 修改 PriceGrowthService，注入 ArtworkPriceLogMapper 并在价格更新时写入涨价日志
    status: completed
  - id: rebuild-and-verify
    content: 编译启动 gateway 和 product 服务，验证涨价记录正常显示
    status: completed
    dependencies:
      - fix-gateway-routes
      - fix-pricegrowth-logging
---

修复管理后台（admin）作品编辑弹窗中"涨价记录"区域不显示任何数据的问题。用户在编辑作品时，弹窗中"涨价记录"分区一直显示"暂无可计算的涨价记录"空状态，没有任何历史涨价记录。

需要解决两个核心问题：

1. 前端请求涨价记录 API 返回 404（网关路由错误）
2. 即使路由通了，数据库 `artwork_price_log` 表中也没有任何记录（PriceGrowthService 从不写日志）

## 技术栈

- Spring Cloud Gateway（路由配置 YAML）
- Spring Boot + MyBatis-Plus（PriceGrowthService）
- MySQL（artwork_price_log 表）

## 实现方案

### 方案概述

1. 修复网关路由：将 `/api/admin/artwork/price/**` 请求路由到 product 服务（端口8082），而不是通用规则路由到的 admin 服务（8090）
2. 在 `PriceGrowthService` 中注入 `ArtworkPriceLogMapper`，在价格更新时写入 `artwork_price_log` 表

### 关键决策

**网关路由**：需要在通用 `/api/admin/**` 规则之前添加一条更精确的路由规则。Spring Cloud Gateway 按路由定义顺序匹配，先定义的路由优先级更高。将 `/api/admin/artwork/price/**` 的路由放在 `shiyiju-admin-artist-manage` 之后、`shiyiju-admin` 之前，确保被优先匹配。

**日志写入策略**：

- `updateSinglePrice()`（浏览/收藏时触发）：每次计算后写入一条 DAILY 类型的涨价日志，记录 oldPriceRise → newPriceRise 的变化
- `updateAllPriceRise()`（定时任务，每小时）：为避免每小时写入大量重复日志，只在 `priceRise` 相比上次存储值有显著变化（>0.0001）时写入
- 日志中 `old_price`/`new_price` 存储的是 `currentPrice`（基准价 * (1+priceRise)）而非 priceRise 本身，以便与前端 `formatPriceLogPrice()` 兼容

### 风险控制

- `PriceGrowthService` 当前依赖 `ArtworkMapper`，新增 `ArtworkPriceLogMapper` 依赖不会破坏现有逻辑
- 日志写入放在 try-catch 中，即使写入失败也不影响价格计算主流程
- 定时任务每小时写入一条日志（当 priceRise 变化时），不会造成日志洪水

### 无需修改的文件

- 前端 `admin/src/views/product/list.vue` - 前端逻辑已正确，只需要 API 正常返回数据
- `ArtworkPriceServiceImpl`、`ArtworkPriceAdminController` - 逻辑不变

## Agent Extensions

### SubAgent

- **code-explorer**：已在需求分析阶段用于探索路由配置和 PriceGrowthService 代码，无需在执行阶段再次调用。