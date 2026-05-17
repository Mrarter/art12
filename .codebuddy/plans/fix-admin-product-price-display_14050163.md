---
name: fix-admin-product-price-display
overview: 修复管理后台作品列表价格显示为涨价后的正确价格，让 admin 的 priceRise/currentPrice 计算与 PriceGrowthService 保持一致
todos:
  - id: fix-admin-price-rise
    content: 修改 ProductAdminPersistenceService.listProducts()，添加 original_price/price_rise 查询并修正基准价计算
    status: completed
  - id: rebuild-and-verify
    content: 编译启动相关后端服务并验证管理后台价格显示正确
    status: completed
    dependencies:
      - fix-admin-price-rise
---

管理后台作品列表的价格列显示的是基础价（如¥100），应该显示涨价后的当前价格（即经过PriceGrowthService计算的价格增长后的金额）。

问题分析：`ProductAdminPersistenceService.listProducts()` 的SQL未查询 `original_price` 和 `price_rise` 字段，使用简化公式独立计算涨幅，与 `PriceGrowthService` 的完整计算结果不一致。

简化公式缺陷：

- 未使用成熟期日增长率（始终用基础日增长率0.02%）
- 未考虑艺术家等级系数（认证/人气/大师）
- 未考虑销售次数加成
- 未使用 `originalPrice` 作为基准价

## 技术方案

### 实现思路

修改 `ProductAdminPersistenceService.listProducts()` 方法，在SQL SELECT中添加 `original_price` 和 `price_rise` 字段。使用 `COALESCE(original_price, price)` 作为基准价（与 `PriceGrowthService.resolveBasePrice()` 一致）。如果DB中已有 `price_rise`（由定时任务每小时间计算并存储），直接使用；否则回退到简化计算作为兜底。

### 关键决策

1. **复用DB中已存储的 `price_rise`**：`PriceGrowthService.updateAllPriceRise()` 每小时定时计算并存储 `price_rise` 到数据库，管理后台直接读取即可保证与用户端前端显示一致，无需重复简化计算。
2. **基准价使用 `originalPrice` 优先**：与 `PriceGrowthService.resolveBasePrice()` 保持一致，`COALESCE(original_price, price)`。
3. **动态字段检查**：使用现有的 `schemaInspector.hasColumn()` 模式检查 `original_price` 和 `price_rise` 列是否存在，确保兼容性。

### 修改文件

**唯一需要修改的文件**：`/Users/master/CodeBuddy/art12/backend/shiyiju-admin/src/main/java/com/shiyiju/admin/service/ProductAdminPersistenceService.java`

具体修改点：

1. 在SQL SELECT中添加 `a.original_price` 和 `a.price_rise`（动态检查列存在）
2. 修改行处理逻辑：用 `COALESCE(original_price, price)` 作为 basePrice；若有存储的 `price_rise` 则使用之
3. 修改 `calculateAdminCurrentPrice` 的调用，传入正确 basePrice

### 无需修改的文件

- 管理前端 `admin/src/views/product/list.vue`：已正确使用 `getDisplayPrice(row)` 优先显示 `currentPrice`
- `PriceGrowthService`、`ProductService` 等：逻辑不变

# Agent Extensions

[subagent:code-explorer] 已在需求分析阶段完成代码探索，无需在执行阶段再次调用。