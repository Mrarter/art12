# 拾艺局 - 运营后台数据库表结构与关系

## 一、核心表关系图（ER Diagram）

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
                           拾艺局运营后台 - 核心表关系
└─────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────────┐
                                    │   users 表       │
                                    │  (用户主表)       │
                                    │  - id (主键)     │
                                    │  - uid (标准UID)  │
                                    │  - nickname      │
                                    │  - identities    │
                                    └────────┬─────────┘
                                             │
                        ┌────────────────────┼────────────────────┐
                        │                    │                    │
                        ▼                    ▼                    ▼
           ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
           │artist_certifi-   │   │ artist_profile   │   │ artist_identity  │
           │cations 表       │   │      表          │   │      表          │
           │  (艺术家认证)    │   │  (艺术家档案)    │   │  (艺术家身份)    │
           └────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘
                    │                    │                    │
                    └────────────────────┴────────────────────┘
                                             │
                                    ┌────────┴────────┐
                                    │   user_id       │
                                    │   user_uid      │
                                    └────────┬────────┘
                                             │
                                             ▼
                               ┌─────────────────────────┐
                               │    artwork 表           │
                               │    (作品表)             │
                               │  - id (主键)            │
                               │  - author_id (作者ID)   │
                               │  - author_uid (作者UID)  │
                               │  - author_name (作者名)  │
                               └─────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────┐
                              艺荐官体系
└─────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐         ┌──────────────────┐         ┌─────────────────────────┐
│   users 表       │────────▶│ promoter_record  │◀────────│ distributor_profile    │
│   (用户主表)     │         │   表 (艺荐官)     │         │      表 (分销)         │
└─────────────────┘         └────────┬─────────┘         └─────────────────────────┘
                                     │
                                     ▼
                          ┌──────────────────┐
                          │commission_record │
                          │   表 (佣金记录)   │
                          └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────┐
                              订单体系
└─────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐         ┌──────────────────┐         ┌─────────────────────────┐
│   users 表       │────────▶│  trade_order     │────────▶│  trade_order_item     │
│   (用户主表)     │         │    表 (订单)      │         │    表 (订单明细)       │
└─────────────────┘         └────────┬─────────┘         └─────────────────────────┘
                                     │
                                     ▼
                          ┌──────────────────┐
                          │ payment_record   │
                          │   表 (支付记录)  │
                          └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────┐
                              钱包体系
└─────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐         ┌──────────────────┐
│   users 表       │────────▶│ wallet_account  │
│   (用户主表)     │         │    表 (钱包)     │
└─────────────────┘         └────────┬─────────┘
                                     │
                                     ▼
                          ┌──────────────────┐
                          │  wallet_flow     │
                          │   表 (钱包流水)   │
                          └──────────────────┘
```

---

## 二、表结构详细说明

### 1. 用户相关表

#### 1.1 users 表（用户主表）
> shiyiju-user 服务使用的主力用户表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键，自增 |
| uid | varchar(50) | 标准用户UID，格式：USR+日期+序列+随机码 |
| nickname | varchar(50) | 用户昵称 |
| avatar | varchar(512) | 头像URL |
| phone | varchar(20) | 手机号 |
| identities | varchar(100) | 身份标识，多个用逗号分隔（artist,collector,promoter） |
| status | int | 状态：1-正常，0-禁用 |
| artist_level | varchar(20) | 艺术家等级 |
| promoter_level | varchar(20) | 艺荐官等级 |
| total_commission | decimal(12,2) | 累计佣金 |
| available_commission | decimal(12,2) | 可用佣金 |
| register_time | datetime | 注册时间 |
| last_login_time | datetime | 最后登录时间 |
| deleted | int | 逻辑删除：0-未删除，1-已删除 |

#### 1.2 user_account 表（用户账户表）
> 旧版用户表，uid 字段可能为空

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键，自增 |
| uid | varchar(50) | 用户UID（可能为空） |
| nickname | varchar(50) | 用户昵称 |
| phone | varchar(20) | 手机号 |
| avatar_url | varchar(512) | 头像URL |
| ... | ... | 其他字段 |

#### 1.3 sys_user 表（系统用户表）
> 后台管理员账户

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 |
| username | varchar(50) | 用户名 |
| password | varchar(100) | 密码（加密） |
| role_id | int | 角色ID |

---

### 2. 艺术家相关表

#### 2.1 artist_certifications 表（艺术家认证表）
> 记录艺术家认证申请

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键，自增 |
| artist_code | varchar(50) | 艺术家编号，格式：ART+日期+序列+随机码 |
| user_id | bigint | 用户ID |
| real_name | varchar(100) | 真实姓名 |
| id_card | varchar(50) | 身份证号 |
| resume | text | 个人履历 |
| artworks | text | 代表作图片URLs，逗号分隔 |
| exhibits | text | 参展证明URLs，逗号分隔 |
| status | int | 认证状态：0-待审核，1-已通过，2-已拒绝 |
| reject_reason | varchar(500) | 拒绝原因 |
| review_time | datetime | 审核时间 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 2.2 artist_profile 表（艺术家档案表）
> 艺术家详细档案

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 用户ID |
| user_uid | varchar(50) | 用户UID |
| artist_name | varchar(100) | 艺术家名称 |
| artist_code | varchar(50) | 艺术家编号 |
| avatar_url | varchar(512) | 头像 |
| bio | text | 个人简介 |
| style_tags | varchar(100) | 风格标签 |
| slogan | varchar(200) | 标语 |
| status | int | 状态 |

#### 2.3 artist_identity 表（艺术家身份表）
> 艺术家身份信息

#### 2.4 artist_level 表（艺术家等级表）
> 艺术家等级配置

#### 2.5 artist_score 表（艺术家评分表）
> 艺术家评分记录

---

### 3. 艺荐官相关表

#### 3.1 promoter_record 表（艺荐官记录表）
> 记录艺荐官信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 用户ID |
| distributor_code | varchar(50) | 分销编号 |
| level | int | 等级 |
| status | int | 状态 |
| invite_code | varchar(20) | 邀请码 |
| parent_id | bigint | 上级ID |
| team_size | int | 团队人数 |

#### 3.2 distributor_profile 表（分销档案表）
> 分销商详细档案

#### 3.3 commission_record 表（佣金记录表）
> 艺荐官佣金记录

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 |
| promoter_id | bigint | 艺荐官ID |
| order_id | bigint | 订单ID |
| commission_amount | decimal(12,2) | 佣金金额 |
| commission_type | varchar(20) | 佣金类型 |
| create_time | datetime | 创建时间 |

---

### 4. 作品相关表

#### 4.1 artwork 表（作品表）
> 用户/艺术家发布的作品

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 |
| author_id | bigint | 作者ID（关联 users.id） |
| author_uid | varchar(50) | 作者UID |
| author_name | varchar(100) | 作者名称 |
| title | varchar(200) | 作品标题 |
| cover_image | varchar(512) | 封面图 |
| category_id | bigint | 分类ID |
| art_type | varchar(50) | 艺术品类型 |
| size | varchar(100) | 尺寸 |
| year | varchar(10) | 年份 |
| price | decimal(12,2) | 价格 |
| original_price | decimal(12,2) | 原价 |
| ownership_type | varchar(20) | 所有权类型 |
| stock | int | 库存 |
| status | int | 状态 |
| view_count | int | 浏览量 |
| favorite_count | int | 收藏量 |

---

### 5. 订单相关表

#### 5.1 trade_order 表（交易订单表）
> 订单主表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 |
| order_no | varchar(50) | 订单编号 |
| user_id | bigint | 用户ID |
| order_amount | decimal(12,2) | 订单金额 |
| promoter_id | bigint | 艺荐官ID（订单来源） |
| status | int | 订单状态 |
| create_time | datetime | 创建时间 |

#### 5.2 trade_order_item 表（订单明细表）
> 订单商品明细

#### 5.3 payment_record 表（支付记录表）
> 支付流水记录

---

### 6. 钱包相关表

#### 6.1 wallet_account 表（钱包账户表）
> 用户钱包

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 用户ID |
| balance | decimal(12,2) | 余额 |
| frozen_amount | decimal(12,2) | 冻结金额 |

#### 6.2 wallet_flow 表（钱包流水表）
> 钱包资金变动记录

---

## 三、表关联关系总结

### 核心关联

| 关系 | 表A | 关联字段 | 表B | 说明 |
|------|-----|----------|-----|------|
| 用户-艺术家认证 | users.id | user_id | artist_certifications.user_id | 一个用户可以有多个认证申请 |
| 用户-艺术家档案 | users.id | user_id | artist_profile.user_id | 一对一关系 |
| 用户-艺荐官 | users.id | user_id | promoter_record.user_id | 一个用户只能是一个艺荐官 |
| 艺术家-作品 | artist_certifications.user_id | user_id | artwork.author_id | 艺术家发布作品 |
| 订单-艺荐官 | trade_order.promoter_id | promoter_id | promoter_record.id | 订单来源追踪 |
| 艺荐官-佣金 | promoter_record.id | promoter_id | commission_record.promoter_id | 佣金记录 |
| 用户-钱包 | users.id | user_id | wallet_account.user_id | 一对一关系 |

---

## 四、表名动态解析逻辑

系统使用 `SchemaInspector` 动态检测实际表名：

```java
// 用户表优先级
userTable() → resolveTable("user", "user_account", "users")

// 艺术家表优先级
artistTable() → resolveTable("artist", "artist_profile", "artist_certifications", "artist_certification")

// 艺荐官表优先级
promoterTable() → resolveTable("promoter", "distributor_profile", "promoter_record")
```

---

## 五、身份体系

系统支持多身份用户，通过 `users.identities` 字段标识：

| 身份 | 标识 | 说明 |
|------|------|------|
| 收藏家 | collector | 默认身份，所有用户都有 |
| 艺术家 | artist | 需要通过认证 |
| 艺荐官 | promoter | 需要申请开通 |

---

*文档生成时间：2026-05-03*
*生成依据：/backend/shiyiju-admin/src/main/java/com/shiyiju/admin/service/UserAdminPersistenceService.java*
