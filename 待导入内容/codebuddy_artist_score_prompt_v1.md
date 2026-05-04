# CodeBuddy 开发总提示词 V1.0
# 项目：艺享集 / 拾艺局 艺术家评分与价格上涨系统

你现在是我的全栈开发工程师，负责在现有项目仓库中开发「艺术家评分系统 + 价格上涨引擎 + 小程序展示 + 后台审核调控」模块。

本项目是一个纯艺术品流通服务平台，不卖装饰画，核心业务包括：
- 艺术家入驻
- 艺术品上架
- 藏家收藏 / 购买
- 艺荐官分销
- 艺术家等级评分
- 作品价格随艺术家成长和市场热度动态上涨

请严格按照以下要求开发。

---

## 一、开发目标

本次需要完成三个模块：

### 1. Spring Boot 后端模块

完成：
- 艺术家评分系统
- 身份资质评分
- 学术资质评分
- 互联网博主资质评分
- 艺术家等级计算
- 作品每日涨价
- 成交触发涨价
- 收藏触发涨价
- 价格日志记录
- 后台人工调分
- 后台人工调价

### 2. 微信小程序前端模块

完成：
- 艺术家主页评分展示
- 艺术家等级徽章
- 艺术家资质标签
- 艺术家评分详情页
- 作品详情页价格成长卡片
- 收藏触发价格变化
- 成交后价格变化展示

### 3. Vue3 后台管理系统模块

完成：
- 艺术家评分列表
- 艺术家资质审核
- 艺术家评分详情
- 人工调分
- 作品价格调控
- 价格日志
- 菜单权限配置

---

## 二、核心业务规则

### 艺术家总评分

总分 1000 分：

| 维度 | 分值 |
|---|---:|
| 销售表现 | 300 |
| 市场影响力 | 200 |
| 活跃度 | 100 |
| 作品质量 | 150 |
| 藏家评价 | 100 |
| 学术资质 | 100 |
| 互联网资质 | 50 |

### 等级规则

| 等级 | 分数 |
|---|---:|
| A+ | 900-1000 |
| A | 750-899 |
| B | 500-749 |
| C | 250-499 |
| D | 0-249 |

---

## 三、学术资质评分规则

学术资质最高 100 分。

加分项包括：
- 中央美术学院 / 中国美术学院 / 清华大学美术学院：+40
- 其他美术学院：+30
- 普通艺术类本科：+15
- 硕士 / 博士：+10
- 教授：+40
- 副教授：+30
- 讲师：+20
- 美协会员：+20
- 国家级展览 / 重要获奖：后续可扩展

要求：
- 必须后台审核通过后才计分
- 艺术家自己填写的数据不能直接生效
- 前台只展示审核通过的标签

---

## 四、互联网博主资质评分规则

互联网资质最高 50 分。

### 粉丝规模

| 粉丝量 | 加分 |
|---|---:|
| 1万-5万 | +5 |
| 5万-10万 | +10 |
| 10万-50万 | +15 |
| 50万-100万 | +20 |
| 100万+ | +25 |

### 内容质量

- 艺术创作内容为主：最高 +10
- 原创内容质量：最高 +10
- 内容转化能力：最高 +10

要求：
- 只认可艺术相关账号
- 娱乐、颜值、泛流量账号不计入互联网资质
- 必须后台审核
- 需要保存平台、账号链接、粉丝数、内容类型、审核备注

---

## 五、价格上涨规则

### 每日涨价

根据艺术家评分决定基础日涨幅：

| 艺术家分数 | 日涨幅 |
|---|---:|
| 900+ | 0.5% |
| 750-899 | 0.4% |
| 500-749 | 0.3% |
| 250-499 | 0.2% |
| 0-249 | 0.1% |

要求：
- 每日涨幅最高不超过 0.5%
- 后续可接入定时任务
- 小程序前端不直接触发每日涨价，应由后端定时任务或后台触发

### 成交触发涨价

作品成交后：
- 默认涨价 2%
- 必须写入价格日志
- change_reason = SALE

### 收藏触发涨价

作品被收藏后：
- 默认涨价 0.5%
- 生产环境建议改成“每新增 10 个收藏触发一次”
- 必须写入价格日志
- change_reason = COLLECT

### 人工调价

后台可人工调整价格：
- 必须填写原因
- 必须记录操作人
- 必须写入价格日志
- change_reason = MANUAL

---

## 六、数据库表要求

需要创建或补充以下表：

### artist_score

字段包括：
- id
- artist_id
- total_score
- sales_score
- influence_score
- activity_score
- quality_score
- review_score
- academic_score
- internet_score
- level
- created_at
- updated_at

### artist_identity

字段包括：
- id
- artist_id
- school_name
- degree
- academic_title
- association_name
- exhibitions
- awards
- social_platform
- social_account_url
- follower_count
- content_type
- content_quality_score
- conversion_score
- verified
- audit_status
- audit_remark
- created_at
- updated_at

### artwork_price_log

字段包括：
- id
- artwork_id
- artist_id
- old_price
- new_price
- change_rate
- change_reason
- remark
- operator_id
- created_at

建议额外增加：

### artist_score_adjust_log

用于记录人工调分：
- id
- artist_id
- old_score
- adjust_score
- new_score
- reason
- operator_id
- created_at

---

## 七、后端接口要求

### 艺术家评分

```http
GET /api/artist/score/{artistId}
POST /api/artist/score/recalculate/{artistId}
```

### 后台评分

```http
GET /api/admin/artist/scores
POST /api/admin/artist/score/manual-adjust
```

### 资质审核

```http
GET /api/admin/artist/identity/audit-list
GET /api/admin/artist/identity/{artistId}
POST /api/admin/artist/identity/audit
```

### 作品价格

```http
POST /api/artwork/price/daily/{artworkId}
POST /api/artwork/price/sale/{artworkId}
POST /api/artwork/price/collect/{artworkId}
```

### 后台价格调控

```http
GET /api/admin/artwork/price/list
POST /api/admin/artwork/price/manual-adjust
GET /api/admin/artwork/price/logs
```

---

## 八、后端代码结构要求

请按照以下结构开发：

```text
src/main/java/com/yixiangji/
├── artist/
│   ├── controller/
│   ├── entity/
│   ├── mapper/
│   ├── service/
│   └── service/impl/
├── artwork/
│   ├── controller/
│   ├── entity/
│   ├── mapper/
│   ├── service/
│   └── service/impl/
└── admin/
    ├── controller/
    ├── dto/
    └── vo/
```

要求：
- 使用 Spring Boot
- 使用 MyBatis / MyBatis Plus 均可
- 所有金额使用 BigDecimal
- 不允许使用 double 处理价格
- 所有人工操作必须写日志
- 接口返回统一结果结构，例如 Result<T>
- 参数校验要完整
- 查询接口支持分页
- 删除操作暂不需要

---

## 九、小程序端开发要求

小程序页面放在：

```text
miniprogram/
├── pages/
│   ├── artist-profile/
│   ├── artist-score/
│   └── artwork-detail/
├── components/
│   ├── artist-level-badge/
│   ├── artist-cert-tags/
│   └── price-growth-card/
├── services/
│   ├── artist-score-api.js
│   └── artwork-price-api.js
└── utils/
    └── request.js
```

页面要求：

### 艺术家主页

展示：
- 艺术家头像
- 艺术家名称
- 艺术家等级
- 总评分
- 认证标签
- 成长指数
- 查看评分详情按钮

### 艺术家评分详情页

展示：
- 总分
- 等级
- 销售表现
- 市场影响力
- 活跃度
- 作品质量
- 藏家评价
- 学术资质
- 互联网资质

### 作品详情页

展示：
- 作品图片
- 当前价格
- 今日涨幅
- 收藏人数
- 下一次涨价条件
- 收藏按钮
- 立即购买按钮

要求：
- 前端不计算最终评分
- 前端不直接计算价格
- 所有数据以后端接口返回为准
- 收藏、购买后刷新作品价格

---

## 十、Vue3 后台开发要求

后台页面放在：

```text
admin-web/
├── src/
│   ├── api/
│   ├── router/modules/
│   ├── views/artist-score/
│   └── components/
```

需要页面：

```text
views/artist-score/
├── ArtistScoreList.vue
├── ArtistIdentityAudit.vue
├── ArtistScoreDetail.vue
├── ArtworkPriceControl.vue
└── ArtworkPriceLog.vue
```

后台菜单：

```text
艺术家管理
├── 艺术家评分
├── 资质审核
└── 评分详情

交易调控
├── 价格调控
└── 价格日志
```

权限点：

```text
artist:score:list
artist:score:detail
artist:score:recalculate
artist:score:manual-adjust
artist:identity:list
artist:identity:audit
artist:identity:detail
artwork:price:list
artwork:price:manual-adjust
artwork:price:log
```

---

## 十一、开发顺序

请严格按照以下顺序开发：

### 第一步：数据库

1. 新增 SQL 文件
2. 创建 artist_score
3. 创建 artist_identity
4. 创建 artwork_price_log
5. 创建 artist_score_adjust_log

### 第二步：后端

1. Entity
2. Mapper
3. Service
4. ServiceImpl
5. Controller
6. Admin Controller
7. 参数 DTO
8. 返回 VO
9. 统一异常处理
10. 接口测试

### 第三步：小程序

1. request 封装
2. API 文件
3. 组件
4. 页面
5. app.json 注册
6. 联调后端接口

### 第四步：后台管理

1. API 文件
2. 路由
3. 菜单
4. 页面
5. 权限点
6. 联调接口

---

## 十二、必须注意的风险

1. 不要把“互联网粉丝数”做成唯一核心评分
2. 不要让未审核资质直接加分
3. 不要让价格无限上涨
4. 所有价格变动都必须记录日志
5. 所有人工操作都必须填写原因
6. 小程序前端不能决定评分和价格
7. 后台人工调价不能绕过日志
8. 艺术品价格上涨要表现为“成长型慢牛”，不能像投机炒作

---

## 十三、最终交付要求

请输出：
1. 完整代码文件
2. SQL 文件
3. 接口文档
4. 页面路由
5. 菜单权限配置
6. 本地启动说明
7. 联调说明
8. 需要我手动补充的环境变量清单

请先检查当前仓库结构，再决定文件应该放在哪里。  
不要覆盖已有业务文件，除非明确需要。  
如果发现已有同名模块，请在原模块基础上增量开发。
