# 金额展示与计算审计清单

更新时间：2026-06-17

## 审计目标

- 前端所有金额统一按“元”展示
- 前端页面中的金额计算统一按“元”语义处理
- 前后端交互时，如果后端仍使用“分”存储或传输，必须在边界处显式转换
- 避免出现 100 倍放大、100 倍缩小、列表和详情金额不一致、展示金额和实际下单金额不一致的问题

## 当前统一口径

| 项目 | 口径 |
|---|---|
| 前端最终展示 | 元 |
| 前端页面内计算 | 元语义 |
| 后端存储 | 需按字段逐一确认，历史上大量字段疑似为分 |
| 前后端边界 | 必须显式做元/分转换，禁止隐式混用 |
| 公共价格工具 | `frontend/src/utils/price.js` |

## 核心公共入口

| 文件 | 作用 | 状态 |
|---|---|---|
| `/Users/master/CodeBuddy/art12/frontend/src/utils/price.js` | H5 价格格式化、分转元、金额展示统一入口 | 已作为主入口使用 |

## H5 前端金额位置

### 订单模块

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/confirm.vue` | 展示 + 提交 | 商品金额、优惠、邮费、合计、提交订单金额 | 已修正，需继续联调 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/pay.vue` | 展示 + 传参 | 待支付金额、应付金额、支付参数 `amount` | 已修正，需重点回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/detail.vue` | 展示 | 商品金额、运费、实付款 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/list.vue` | 展示 | 订单列表金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/refund.vue` | 展示 | 退款金额、订单金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/logistics.vue` | 展示 | 商品金额、订单金额 | 基本已统一，建议回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/track.vue` | 展示 | 订单金额摘要 | 基本已统一，建议回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/settle.vue` | 展示 + 计算 | 小计、总价、邮费 | 基本已统一，建议回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/order/intent.vue` | 展示 | 意向价格、参考价格 | 已修正 |

### 转售模块

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/frontend/src/pages/resale/publish.vue` | 展示 + 计算 | 转售价格、作品评估价格、卖家实际收入 | 已修正，需重点回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/resale/detail.vue` | 展示 | 转售价、成交价、历史价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/resale/my.vue` | 展示 | 转售金额、收益 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/resale/market.vue` | 展示 | 转售市场价格 | 已修正 |

### 拍卖模块

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/frontend/src/pages/auction/index.vue` | 展示 | 起拍价、当前价 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/auction/session.vue` | 展示 | 场次价格、成交价 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/auction/bidding.vue` | 展示 + 计算 | 当前价、加价幅度、保证金 | 已修正，建议回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/auction/detail.vue` | 展示 + 计算 | `currentPrice`、`deposit`、`increment`、出价记录 | 待重点复查 |

### 用户与资产模块

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/frontend/src/pages/user/purchased.vue` | 展示 | 购买价、成交价 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/user/favorites.vue` | 展示 | 收藏作品价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/user/history.vue` | 展示 | 浏览记录价格 | 基本已统一，建议回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/user/wallet.vue` | 展示 | 余额、收支、提现金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/user/invoice.vue` | 展示 | 发票金额、示例金额 | 待复查 |

### 经纪人 / 分销模块

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/index.vue` | 展示 | 累计收益、可提现金额、佣金 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/commission.vue` | 展示 | 佣金金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/withdraw.vue` | 展示 + 提交 | 提现金额、可提现金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/withdrawLog.vue` | 展示 | 提现记录金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/team.vue` | 展示 | 团队金额、贡献金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/team-detail.vue` | 展示 | 成交额、佣金 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/earnings.vue` | 展示 | `item.amount` 等收益字段 | 待重点复查 |

### 作品与交易入口

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/frontend/src/pages/gallery/detail.vue` | 展示 | 售价、评估价、参考价 | 已修正，建议回归 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/gallery/index.vue` | 展示 | 作品列表价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/category/index.vue` | 展示 | 分类页价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/search/index.vue` | 展示 | 搜索结果价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/cart/index.vue` | 展示 + 计算 | 单价、合计、结算金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/index/index.vue` | 展示 | 首页推荐价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/pages/message/chat.vue` | 展示 | 订单/作品引用价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/frontend/src/components/PriceGrowthCard.vue` | 展示 | 价格走势显示 | 已修正 |

## Admin 后台金额位置

### 订单模块

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/admin/src/views/order/list.vue` | 展示 | 订单金额、作品信息、详情弹窗单价/小计 | 已修正，建议重点回归 |
| `/Users/master/CodeBuddy/art12/admin/src/views/order/detail.vue` | 展示 | 商品金额、实付金额、退款金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/order/OrderList.vue` | 展示 | 订单列表金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/order/IntentList.vue` | 展示 | 意向价格、报价金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/order/aftersale.vue` | 展示 | 售后金额、退款金额 | 已修正 |

### 作品 / 转售 / 用户 / 分销模块

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/admin/src/views/product/list.vue` | 展示 | 发布价、转售价、价格列 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/product/audit.vue` | 展示 | 审核作品价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/resale/ResaleList.vue` | 展示 | 转售价、收益、服务费 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/resale/ResaleStats.vue` | 展示 | 转售统计金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/user/list.vue` | 展示 | 消费额、余额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/user/artist.vue` | 展示 | 销售额、估值、作品价格 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/user/promoter.vue` | 展示 | 佣金、提现、团队业绩 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/user/user-profile.vue` | 展示 | 钱包、累计消费、收益 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/promotion/commission.vue` | 展示 | 佣金金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/promotion/withdraw.vue` | 展示 | 提现金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/promotion/report.vue` | 展示 | 分销报表金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/work/WorkList.vue` | 展示 | 作品定价 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/work/CirculationRecordList.vue` | 展示 | 流转金额 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/artist-score/ArtworkPriceControl.vue` | 展示 + 设置 | 评估价、调价 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/views/artist-score/ArtworkPriceLog.vue` | 展示 | 价格变更记录 | 已修正 |
| `/Users/master/CodeBuddy/art12/admin/src/components/ArtistDetailDialog.vue` | 展示 | 销售额、作品价格 | 已修正 |

## 后端金额链路

| 文件 | 类型 | 价格相关内容 | 状态 |
|---|---|---|---|
| `/Users/master/CodeBuddy/art12/backend/shiyiju-order/src/main/java/com/shiyiju/order/service/OrderService.java` | 计算 | 下单金额、订单项金额、支付金额 | 待重点复查 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-order/src/main/java/com/shiyiju/order/controller/OrderController.java` | 接口 | 创建订单、支付金额出参 | 待重点复查 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-order/src/main/java/com/shiyiju/order/entity/Order.java` | 存储 | `totalAmount`、`payAmount` 等 | 待确认单位 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-order/src/main/java/com/shiyiju/order/entity/OrderItem.java` | 存储 | 单价、小计 | 待确认单位 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-admin/src/main/java/com/shiyiju/admin/service/OrderService.java` | 查询聚合 | 后台订单列表、详情金额 | 已联动修正展示层 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-admin/src/main/java/com/shiyiju/admin/service/ProductAdminPersistenceService.java` | 查询/保存 | 作品价格、转售价 | 待持续验证 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-admin/src/main/java/com/shiyiju/admin/service/UserAdminPersistenceService.java` | 查询聚合 | 消费额、余额、收益 | 待持续验证 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-admin/src/main/java/com/shiyiju/admin/service/PromotionService.java` | 计算 | 佣金、提现、报表 | 待持续验证 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-admin/src/main/java/com/shiyiju/admin/service/AuctionService.java` | 计算/查询 | 拍卖金额、保证金、成交价 | 待重点复查 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-user/src/main/java/com/shiyiju/user/service/ResaleService.java` | 计算 | 转售价、艺术家收益、平台费 | 待持续验证 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-user/src/main/java/com/shiyiju/user/service/WalletService.java` | 计算 | 余额、收支、提现 | 待持续验证 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-user/src/main/java/com/shiyiju/user/service/ledger/LedgerService.java` | 记账 | 收支流水 | 待持续验证 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-user/src/main/java/com/shiyiju/user/controller/WalletController.java` | 接口 | 钱包金额出参 | 待持续验证 |
| `/Users/master/CodeBuddy/art12/backend/shiyiju-user/src/main/java/com/shiyiju/user/controller/ResaleController.java` | 接口 | 转售金额出参 | 待持续验证 |

## 本轮重点问题

| 问题 | 现象 | 风险 |
|---|---|---|
| 元 / 分混用 | 支付页、订单页、后台订单页可能出现 100 倍放大或缩小 | 高 |
| 展示与计算分离不彻底 | 模板展示已转元，但计算仍可能沿用原值 | 高 |
| 后端字段单位不清晰 | `amount`、`price`、`payAmount`、`totalAmount` 等字段命名未带单位 | 高 |
| 示例文案污染 | 发票页、演示页、说明页可能残留错误金额示例 | 中 |

## 同一商品跨页面价格对照样本

> 说明：本表优先记录已经在联调和截图中实际出现过的样本，用来观察“同一商品 / 同一订单 / 同一笔交易”在不同页面中的价格是否一致。

| 商品 / 订单 | 页面位置 | 显示价格 | 预期 / 参考值 | 判断 |
|---|---|---|---|---|
| 秋思 2 / `SYJ202605200824560101` | H5 订单详情 `/pages/order/detail?id=8` 商品金额 | `¥10,000.00` | 用户反馈后台应为 `¥110.00` | 错误，疑似放大约 90.91 倍 |
| 秋思 2 / `SYJ202605200824560101` | H5 订单详情 `/pages/order/detail?id=8` 实付款 | `¥10,000.00` | 用户反馈后台应为 `¥110.00` | 错误，和商品金额同步异常 |
| 秋思 2 / `SYJ202605200824560101` | Admin 订单列表 `/order/list` 订单金额 | `¥0.00` | 用户反馈后台正确金额应为 `¥110.00` | 错误，疑似取值链路异常 |
| 测试作品 / `SYJ202606161626260101` | Admin 订单列表 `/order/list` 订单金额 | `¥110.00` | `¥110.00` | 正常 |
| 测试作品 / `SYJ202606161626260101` | Admin 订单详情弹窗 单价 | `¥110.00` | `¥110.00` | 正常 |
| 测试作品 / `SYJ202606161626260101` | Admin 订单详情弹窗 小计 | `¥110.00` | `¥110.00` | 正常 |
| 继续测试作品 / 确认订单页 | H5 确认订单 `/pages/order/confirm?artworkId=40&quantity=1` 商品金额 | `¥547.25` | `¥547.25` | 正常 |
| 继续测试作品 / 支付页 `orderId=41` | H5 支付页 `/pages/order/pay?orderId=41&amount=54725&paymentMethod=wechat` 待支付金额 | `¥54,725.00` | 应与确认订单页一致，为 `¥547.25` | 严重错误，放大 100 倍 |
| 继续测试作品 / 支付页 `orderId=41` | H5 支付页 订单信息区作品价格 | `¥54,725.00` | `¥547.25` | 严重错误，放大 100 倍 |
| 继续测试作品 / 支付页 `orderId=41` | H5 支付页 费用明细商品金额 | `¥54,725.00` | `¥547.25` | 严重错误，放大 100 倍 |
| 继续测试作品 / 支付页 `orderId=41` | H5 支付页 应付金额 | `¥54,725.00` | `¥547.25` | 严重错误，放大 100 倍 |
| 继续测试作品 / `SYJ202606161952450101` | Admin 订单列表 `/order/list` 订单金额 | `¥54,725.00` | 应与确认订单页一致，为 `¥547.25` | 严重错误，后台也被放大 100 倍 |
| 测试作品 / 转售发布页 | H5 转售发布 `/pages/resale/publish?artworkId=16&price=110` 输入转售价 | `1` 或 `10000`（用户输入） | 应按元输入 | 输入语义需保持“元” |
| 测试作品 / 转售发布页 | H5 转售发布 作品评估价格 | `¥1.10` | 由作品原始价格 `110` 元换算后应明确一致 | 需继续确认评估价字段口径 |
| 测试作品 / 转售发布页 | H5 转售发布 卖家实际收入 | `¥0.80`（当输入 `1` 时） | 与转售价、费率计算一致 | 计算口径已转元，但需继续核对源字段 |
| 测试价格统一6.17 / 作品 `96` | H5 作品详情 `/product/96` 创建后、下单前 | `price=¥617.17`，`originalPrice=¥617.17`，`currentPrice=¥636.00` | 同一作品对外展示口径应唯一 | 错误，同时存在基础价和实时价两套口径 |
| 测试价格统一6.17 / 作品 `96` | H5 作品列表 `/product/list`（买家 `13800138001` 下单前） | `price=¥617.17`，`currentPrice=¥636.00` | 应与作品详情、订单确认口径一致 | 错误，浏览态已出现双价格 |
| 测试价格统一6.17 / 作品 `96` | 运营后台作品列表 `/admin/product/list`（2026-06-17 复测） | `price=¥6.36`，`originalPrice=¥6.1717`，`currentPrice=¥6.36` | 至少应与 H5 成交后作品详情 `¥636.00 / ¥617.17 / ¥636.00` 同量级 | 错误，后台作品列表仍缩小 100 倍 |
| 测试价格统一6.17 / 作品 `96` | 运营后台作品列表 `/admin/product/list`（2026-06-17 修复后复测） | `price=¥636.00`，`originalPrice=¥617.17`，`currentPrice=¥636.00` | 应与 H5 成交后作品详情 `¥636.00 / ¥617.17 / ¥636.00` 同步 | 正常，后台作品列表价格显示已修正 |
| 测试价格统一6.17 / 订单 `SYJ202606170914040102` | 订单服务直购创建响应（买家 `13800138001`） | `totalAmount=¥636.00`，`payAmount=¥636.00` | 若下单按实时价，应与浏览页主展示价明确一致 | 与 `currentPrice` 一致，但与 `price=¥617.17` 不一致 |
| 测试价格统一6.17 / 订单 `SYJ202606170914030103` | 订单服务直购创建响应（买家 `13800138002`） | `totalAmount=¥636.00`，`payAmount=¥636.00` | 同上 | 与 `currentPrice` 一致 |
| 测试价格统一6.17 / 订单 `SYJ202606170914390104` | 订单服务直购创建响应（买家 `13800138003`） | `totalAmount=¥636.00`，`payAmount=¥636.00` | 同上 | 与 `currentPrice` 一致 |
| 测试价格统一6.17 / 订单 `SYJ202606170914040102` | H5 订单详情 `/order/orders/43` 商品金额 | `¥636.00` | 应与下单金额一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914040102` | H5 订单详情 `/order/orders/43` 实付款 | `¥636.00` | 应与下单金额一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914040102` | H5 订单详情 商品行单价 / 小计 | `¥636.00 / ¥636.00` | 应与订单金额一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914030103` | H5 订单详情 `/order/orders/42` 商品金额 / 实付款 | `¥636.00 / ¥636.00` | 应与下单金额一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914390104` | H5 订单详情 `/order/orders/44` 商品金额 / 实付款 | `¥636.00 / ¥636.00` | 应与下单金额一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914040102` | Admin 订单列表 `/admin/order/list` 订单金额 | `¥636.00` | 应与 H5 订单详情一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914030103` | Admin 订单列表 `/admin/order/list` 订单金额 | `¥636.00` | 应与 H5 订单详情一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914040102` | Admin 订单详情 `/admin/order/detail/43` 商品金额 / 实付金额 | `¥636.00 / ¥636.00` | 应与 H5 订单详情一致 | 正常 |
| 测试价格统一6.17 / 订单 `SYJ202606170914040102` | Admin 订单详情 商品行单价 / 小计 | `¥636.00 / ¥636.00` | 应与 H5 订单详情一致 | 正常 |
| 测试价格统一6.17 / 作品 `96` | 成交后 H5 作品详情（买家 `13800138001`、`13800138002`、`13800138003`） | `price=¥636.00`，`originalPrice=¥617.17`，`currentPrice=¥636.00` | 成交后不应把基础价直接改写为订单成交价，除非全链路都以该价为唯一口径 | 错误，作品价格字段在成交后被改写 |

## 2026-06-17 实测账号矩阵

> 样本作品：`测试价格统一6.17`（作品 ID `96`），创建账号 `13800138000 / userId=101`。  
> 测试买家：`13800138001`、`13800138002`、`13800138003`。  
> 创建时写入作品基础价 `¥617.17`，但产品服务实时价为 `¥636.00`。
> 备注：`/api/user/phone-login` 在 `2026-06-17` 实测时返回 500，因此本轮创建动作通过 `X-User-Id: 101` 直连服务完成，账号归属未变。

| 账号 | 角色 | 观察位置 | 价格字段 / 显示值 | 判断 |
|---|---|---|---|---|
| `13800138000` | 发布者 | 作品创建后详情 `/product/96` | `price=617.17`，`originalPrice=617.17`，`currentPrice=636.00` | 浏览态双口径，需统一 |
| `13800138000` | 运营后台 | 作品列表 `/admin/product/list` | `price=6.36`，`originalPrice=6.1717`，`currentPrice=6.36` | 后台作品列表仍比 H5/订单小 100 倍 |
| `13800138000` | 运营后台 | 作品列表 `/admin/product/list`（修复后复测） | `price=636.00`，`originalPrice=617.17`，`currentPrice=636.00` | 已与 H5 成交后作品详情对齐 |
| `13800138001` | 买家 1 | 直购创建响应 / 订单 `43` | `totalAmount=636.00`，`payAmount=636.00` | 下单按实时价 `636.00` 计算 |
| `13800138001` | 买家 1 | H5 订单详情 / Admin 订单列表 / Admin 订单详情 | 均为 `¥636.00` | 订单链路内部一致 |
| `13800138001` | 买家 1 | 成交后作品详情 `/product/96` | `price=636.00`，`originalPrice=617.17` | 成交后作品价被改写 |
| `13800138002` | 买家 2 | 直购创建响应 / 订单 `42` | `totalAmount=636.00`，`payAmount=636.00` | 下单按实时价 `636.00` 计算 |
| `13800138002` | 买家 2 | H5 订单详情 | 商品金额、实付款、单价、小计均为 `¥636.00` | 订单链路内部一致 |
| `13800138003` | 买家 3 | 直购创建响应 / 订单 `44` | `totalAmount=636.00`，`payAmount=636.00` | 下单按实时价 `636.00` 计算 |
| `13800138003` | 买家 3 | H5 订单详情 | 商品金额、实付款、单价、小计均为 `¥636.00` | 订单链路内部一致 |

## 本轮新增结论

- “作品浏览态”和“订单成交态”仍不是同一套价格口径。
- 作品创建时写入的基础价是 `¥617.17`，但下单实际成交价统一走 `currentPrice=¥636.00`。
- 一旦有买家完成支付，作品表 `artwork.price` 会被直接改写成 `¥636.00`，同时 `original_price` 变成 `¥617.17`，这会让成交前后的作品详情口径发生跳变。
- 运营后台作品列表 `/admin/product/list` 当前仍显示 `¥6.36`，而同一作品在 H5 详情和订单链路里是 `¥636.00`，说明后台作品列表仍残留一次错误的 `/100` 缩放。
- 上述运营后台作品列表 `/admin/product/list` 的 `/100` 缩放问题已在 `2026-06-17` 修复，修复后复测返回 `price=¥636.00`、`originalPrice=¥617.17`、`currentPrice=¥636.00`。
- 订单链路内部目前是统一的：订单服务创建响应、H5 订单详情、Admin 订单列表、Admin 订单详情都显示 `¥636.00`。
- 这说明本轮最主要的问题已经从“订单页 100 倍错误”收敛为“作品页基础价 / 实时价 / 成交价三套口径未统一”。

## 重点回归建议

| 优先级 | 页面 / 文件 | 回归点 |
|---|---|---|
| 高 | `/Users/master/CodeBuddy/art12/frontend/src/pages/order/pay.vue` | 支付页待支付金额、费用明细、支付按钮金额是否一致 |
| 高 | `/Users/master/CodeBuddy/art12/frontend/src/pages/order/confirm.vue` | 提交订单金额与实际创建订单金额是否一致 |
| 高 | `/Users/master/CodeBuddy/art12/frontend/src/pages/resale/publish.vue` | 转售价格、评估价格、卖家收入是否同口径 |
| 高 | `/Users/master/CodeBuddy/art12/admin/src/views/order/list.vue` | 后台订单金额与 H5 订单详情是否一致 |
| 高 | `/Users/master/CodeBuddy/art12/backend/shiyiju-order/src/main/java/com/shiyiju/order/service/OrderService.java` | 下单、支付、订单金额字段是否全链路同单位 |
| 高 | `/Users/master/CodeBuddy/art12/frontend/src/pages/auction/detail.vue` | 保证金、加价幅度、当前价是否有元/分混用 |
| 中 | `/Users/master/CodeBuddy/art12/frontend/src/pages/promoter/earnings.vue` | 收益金额是否仍直接使用原始值 |
| 中 | `/Users/master/CodeBuddy/art12/frontend/src/pages/user/invoice.vue` | 发票金额及示例金额是否统一 |

## 当前结论

- H5 和 Admin 的主要显示层已经基本切到“按元显示”
- 订单、支付、转售、钱包、分销是核心金额链路，已完成第一轮统一
- 剩余最高风险点主要集中在订单服务后端、拍卖模块、个别收益页和示例页
- 后续应继续按“页面展示、页面计算、接口入参、接口出参、数据库字段”五层口径逐项核对
- 对同一商品做跨页面比对很有必要，尤其要对照“确认订单页 -> 支付页 -> H5 订单详情 -> Admin 订单列表 -> Admin 订单详情”
