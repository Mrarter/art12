# 拾艺局 API 接口文档

> **文档版本**: 1.0.0  
> **更新时间**: 2026-04-22  
> **基础路径**: 
> - 小程序API: `/api`
> - 管理后台API: `/api/admin`

---

## 目录

1. [通用说明](#通用说明)
2. [用户模块](#1-用户模块-shiyiju-user)
3. [商品/作品模块](#2-商品作品模块-shiyiju-product)
4. [订单模块](#3-订单模块-shiyiju-order)
5. [拍卖模块](#4-拍卖模块-shiyiju-auction)
6. [社区模块](#5-社区模块-shiyiju-community)
7. [消息模块](#6-消息模块-shiyiju-message)
8. [文件上传模块](#7-文件上传模块-shiyiju-file)
9. [艺荐官/推广模块](#8-艺荐官推广模块-shiyiju-promotion)
10. [管理后台API](#9-管理后台api)

---

## 通用说明

### 响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1704067200000
}
```

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 分页响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

### 认证方式

- Header: `X-User-Id` - 用户ID（部分接口需要）
- Header: `Authorization: Bearer <token>` - 管理员Token

---

## 1. 用户模块 (shiyiju-user)

### 1.1 用户控制器 (UserController)

**基础路径**: `/api/user`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/user/auth/wx-login` | body: WxLoginDTO | 微信登录 |
| POST | `/user/wxlogin` | body: WxLoginDTO | 微信登录(兼容路径) |
| GET | `/user/info` | Header: X-User-Id | 获取用户信息 |
| PUT | `/user/user/update` | Header: X-User-Id, body | 更新用户信息 |
| POST | `/user/user/bind-phone` | Header: X-User-Id, phone, verifyCode | 绑定手机号 |
| POST | `/user/user/artist/cert` | Header: X-User-Id, body | 艺术家认证申请 |
| GET | `/user/artist/cert/status` | Header: X-User-Id | 获取艺术家认证状态 |
| POST | `/user/user/promoter/open` | Header: X-User-Id | 开通艺荐官 |
| GET | `/user/user/promoter/invite-code` | Header: X-User-Id | 获取艺荐官邀请码 |
| POST | `/user/artist/{artistId}/follow` | Header: X-User-Id, Path: artistId | 关注艺术家 |
| DELETE | `/user/artist/{artistId}/follow` | Header: X-User-Id, Path: artistId | 取消关注艺术家 |
| GET | `/user/artist/{artistId}` | Path: artistId | 获取艺术家主页信息 |

#### WxLoginDTO

```json
{
  "code": "string",          // 微信授权码
  "encryptedData": "string", // 用户信息加密数据
  "iv": "string",            // 加密算法的初始向量
  "nickname": "string",      // 昵称
  "avatar": "string",        // 头像URL
  "gender": 0,               // 性别: 0-未知, 1-男, 2-女
  "birthday": "string",      // 生日
  "region": "string",        // 地区
  "inviteCode": "string"     // 邀请码
}
```

#### ArtistCertDTO

```json
{
  "realName": "string",      // 真实姓名
  "idCard": "string",        // 身份证号
  "resume": "string",        // 个人履历
  "artworks": ["string"],    // 代表作图片URLs
  "exhibits": ["string"]      // 参展证明图片URLs
}
```

### 1.2 个人中心控制器 (UserCenterController)

**基础路径**: `/api/user`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/user/center` | Header: X-User-Id | 获取个人中心聚合数据 |
| GET | `/user/addresses` | Header: X-User-Id | 获取收货地址列表 |
| POST | `/user/addresses` | Header: X-User-Id, body: Address | 添加收货地址 |
| PUT | `/user/addresses/{id}` | Header: X-User-Id, Path: id, body | 更新收货地址 |
| DELETE | `/user/addresses/{id}` | Header: X-User-Id, Path: id | 删除收货地址 |
| GET | `/user/favorites` | Header: X-User-Id, page, pageSize | 我的收藏列表 |
| GET | `/user/history` | Header: X-User-Id, page, pageSize | 浏览足迹 |

### 1.3 艺术家主页控制器 (ArtistController)

**基础路径**: `/api/artist`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/artist/{userId}` | Path: userId, Header: X-User-Id(可选) | 获取艺术家主页信息 |
| GET | `/artist/{userId}/works` | Path: userId, page, pageSize | 获取艺术家作品列表 |
| GET | `/artist/{userId}/moments` | Path: userId, page, pageSize | 获取艺术家动态 |
| POST | `/artist/{userId}/follow` | Header: X-User-Id, Path: userId | 关注艺术家 |
| DELETE | `/artist/{userId}/follow` | Header: X-User-Id, Path: userId | 取消关注艺术家 |

---

## 2. 商品/作品模块 (shiyiju-product)

### 2.1 商品控制器 (ProductController)

**基础路径**: `/api/product`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/product/upload` | file | 上传作品图片 |
| GET | `/product/list` | page, pageSize, categoryId, sort, minPrice, maxPrice, yearFrom, yearTo, region, holdDuration, Header: X-User-Id | 获取作品列表 |
| GET | `/product/{id}` | Path: id, Header: X-User-Id(可选) | 获取作品详情 |
| GET | `/product/search` | keyword, page, pageSize, Header: X-User-Id | 搜索作品 |
| GET | `/product/banners` | - | 获取Banner列表 |
| GET | `/product/categories` | - | 获取分类列表 |
| POST | `/product/favorite` | Header: X-User-Id, body: artworkId | 收藏作品 |
| DELETE | `/product/favorite/{id}` | Header: X-User-Id, Path: id | 取消收藏 |
| PUT | `/product/update` | body: ArtworkUpdateDTO | 更新作品 |
| PUT | `/product/batch/status` | body: {ids, status} | 批量更新作品状态 |
| POST | `/product/create` | body: ArtworkUpdateDTO | 创建作品 |
| DELETE | `/product/{id}` | Path: id | 删除作品 |
| GET | `/product/homepage/banners` | - | 获取首页Banner(旧) |
| GET | `/product/artwork/detail/{id}` | Path: id | 获取作品详情(旧) |

#### ArtworkQueryDTO (查询参数)

```json
{
  "page": 1,
  "pageSize": 20,
  "categoryId": 1,
  "keyword": "string",
  "sort": "newest|price_asc|price_desc|popular",
  "minPrice": 0,
  "maxPrice": 10000,
  "yearFrom": 2020,
  "yearTo": 2024,
  "region": "string",
  "holdDuration": 0
}
```

#### ArtworkUpdateDTO (创建/更新参数)

```json
{
  "id": 1,
  "title": "string",
  "authorName": "string",
  "categoryId": 1,
  "cover": "http://...",
  "price": 5000.00,
  "originalPrice": 8000.00,
  "stock": 10,
  "description": "string",
  "status": 1,
  "weight": 0,
  "ownershipType": 1,
  "artworkCode": "string",
  "artType": "string",
  "size": "100*80",
  "year": 2024,
  "distributionEnabled": false,
  "commissionRate": 10
}
```

### 2.2 评价控制器 (ReviewController)

**基础路径**: `/api/review`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/review/list/{artworkId}` | Path: artworkId, page, pageSize | 获取作品评价列表 |
| POST | `/review/add` | Header: X-User-Id, body | 发表评论 |

### 2.3 系统配置控制器 (SystemConfigController)

**基础路径**: `/api/config`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/config/banner/list` | position | 获取Banner列表 |
| GET | `/config/agreement/{type}` | Path: type(privacy/user) | 获取协议 |

---

## 3. 订单模块 (shiyiju-order)

### 3.1 订单控制器 (OrderController)

**基础路径**: `/api/order`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/order/create` | Header: X-User-Id, body: CreateOrderDTO | 创建订单 |
| GET | `/order/list` | Header: X-User-Id, status, page, pageSize | 订单列表 |
| GET | `/order/{orderId}` | Header: X-User-Id, Path: orderId | 订单详情 |
| POST | `/order/cancel/{orderId}` | Header: X-User-Id, Path: orderId | 取消订单 |
| POST | `/order/confirm/{orderId}` | Header: X-User-Id, Path: orderId | 确认收货 |
| POST | `/order/refund/{orderId}` | Header: X-User-Id, Path: orderId, reason | 申请退款 |
| GET | `/order/stats` | Header: X-User-Id | 获取订单统计 |

#### CreateOrderDTO

```json
{
  "artworkId": 1,
  "addressId": 1,
  "remark": "string",
  "quantity": 1
}
```

### 3.2 物流控制器 (LogisticsController)

**基础路径**: `/api/logistics`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/logistics/{orderId}` | Path: orderId | 获取物流信息 |

### 3.3 微信支付回调 (WxPayCallbackController)

**基础路径**: `/api/pay`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/pay/callback` | XML body | 微信支付回调 |

---

## 4. 拍卖模块 (shiyiju-auction)

### 拍卖控制器 (AuctionController)

**基础路径**: `/api/auction`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/auction/list` | page, pageSize, status | 拍卖列表 |
| GET | `/auction/{id}` | Path: id, Header: X-User-Id | 拍卖详情 |
| POST | `/auction/{id}/bid` | Path: id, Header: X-User-Id, amount | 出价 |
| POST | `/auction/{id}/favorite` | Header: X-User-Id, Path: id | 收藏拍卖 |
| GET | `/auction/{id}/bids` | Path: id, page, pageSize | 出价记录 |

---

## 5. 社区模块 (shiyiju-community)

### 社区控制器 (CommunityController)

**基础路径**: `/api/community`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/community/moments` | page, pageSize, type | 动态列表 |
| GET | `/community/moment/{id}` | Path: id | 动态详情 |
| POST | `/community/moment` | Header: X-User-Id, body | 发布动态 |
| POST | `/community/moment/{id}/like` | Header: X-User-Id, Path: id | 点赞 |
| DELETE | `/community/moment/{id}/like` | Header: X-User-Id, Path: id | 取消点赞 |
| POST | `/community/moment/{id}/comment` | Header: X-User-Id, Path: id, body | 评论 |
| DELETE | `/community/comment/{id}` | Header: X-User-Id, Path: id | 删除评论 |
| GET | `/community/follow/moments` | Header: X-User-Id, page, pageSize | 关注动态 |

---

## 6. 消息模块 (shiyiju-message)

### 消息控制器 (MessageController)

**基础路径**: `/api/message`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/message/list` | Header: X-User-Id, type, page, pageSize | 消息列表 |
| GET | `/message/unread-count` | Header: X-User-Id | 未读消息数量 |
| POST | `/message/mark-read` | Header: X-User-Id, ids | 标记已读 |
| POST | `/message/mark-all-read` | Header: X-User-Id | 全部标记已读 |

---

## 7. 文件上传模块 (shiyiju-file)

### 文件控制器 (FileController)

**基础路径**: `/api/file`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/file/upload` | file | 上传文件(图片/视频) |
| POST | `/file/upload/base64` | body: {data, type} | Base64上传 |
| DELETE | `/file/delete` | url | 删除文件 |
| GET | `/upload/{path}` | Path: path | 访问上传的文件 |

---

## 8. 艺荐官/推广模块 (shiyiju-promotion)

### 推广控制器 (PromotionController)

**基础路径**: `/api/promotion`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/promotion/center` | Header: X-User-Id | 艺荐官中心数据 |
| GET | `/promotion/team` | Header: X-User-Id, page, pageSize | 我的团队 |
| GET | `/promotion/earnings` | Header: X-User-Id, page, pageSize | 收益明细 |
| POST | `/promotion/withdraw` | Header: X-User-Id, body | 申请提现 |
| GET | `/promotion/withdraw/history` | Header: X-User-Id, page, pageSize | 提现记录 |
| GET | `/promotion/invite/{code}` | Path: code | 通过邀请码获取邀请人信息 |
| GET | `/promotion/stats/daily` | Header: X-User-Id, date | 日统计数据 |
| GET | `/promotion/stats/monthly` | Header: X-User-Id, year, month | 月统计数据 |
| GET | `/promotion/earnings/trend` | Header: X-User-Id, days | 收益趋势 |
| GET | `/promotion/leaderboard` | Header: X-User-Id, type | 排行榜 |
| GET | `/promotion/ranking/list` | Header: X-User-Id, type | 排行列表 |
| GET | `/promotion/qrcode` | Header: X-User-Id | 获取推广二维码 |

---

## 9. 管理后台API

> **基础路径**: `/api/admin`  
> **认证方式**: Header: `Authorization: Bearer <token>`

### 9.1 管理员控制器 (AdminController)

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/admin/login` | body: {username, password} | 管理员登录 |
| GET | `/admin/info` | - | 获取管理员信息 |

### 9.2 用户管理 (UserManagementController)

**基础路径**: `/api/admin/user`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/user/list` | page, size, identity, keyword | 用户列表 |
| POST | `/admin/user/create` | body | 创建用户 |
| POST | `/admin/user/batch-create` | body: {identity, count} | 批量创建用户 |
| GET | `/admin/user/stats` | - | 用户身份统计 |
| GET | `/admin/user/artist/list` | page, size, status | 艺术家认证列表 |
| POST | `/admin/user/artist/approve` | body: {id, badge} | 通过艺术家认证 |
| POST | `/admin/user/artist/reject` | body: {id, reason} | 拒绝艺术家认证 |
| POST | `/admin/user/artist/revoke` | body: {id} | 取消艺术家认证 |
| POST | `/admin/user/artist/reapply` | body: {id} | 重新发起认证 |
| POST | `/admin/user/artist/add` | body | 手动添加艺术家 |
| POST | `/admin/user/artist/badge` | body: {id, badge} | 设置艺术家等级 |
| GET | `/admin/user/artist/{id}` | Path: id | 艺术家详情 |
| GET | `/admin/user/{userId}` | Path: userId | 用户详情 |
| POST | `/admin/user/{userId}/identity` | body: {identity} | 更新用户身份 |
| POST | `/admin/user/updateStatus` | body: {userId, status} | 更新用户状态 |
| POST | `/admin/user/delete` | body: {userId} | 删除用户 |
| GET | `/admin/user/promoter/list` | page, size, userId, level | 艺荐官列表 |
| POST | `/admin/user/promoter/add` | body: {userId, level, remark} | 添加艺荐官 |
| POST | `/admin/user/promoter/freeze` | body: {userId, status} | 冻结/解冻艺荐官 |
| GET | `/admin/user/promoter/{userId}` | Path: userId | 艺荐官详情 |
| GET | `/admin/user/promoter/team/{userId}` | Path: userId, page, size | 艺荐官团队成员 |
| GET | `/admin/user/promoter/commission/{userId}` | Path: userId, page, size | 艺荐官佣金记录 |

### 9.3 作品管理 (ProductAdminController)

**基础路径**: `/api/admin/product`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/product/list` | artworkId, title, artistName, categoryId, status, pageNum, pageSize | 作品列表 |
| GET | `/admin/product/categories` | - | 作品分类列表 |
| GET | `/admin/product/audit/list` | status, page, size | 作品审核列表 |
| POST | `/admin/product/upload` | file | 上传作品图片 |
| POST | `/admin/product/create` | body | 创建作品 |
| PUT | `/admin/product/update` | body | 更新作品 |
| DELETE | `/admin/product/{id}` | Path: id | 删除作品 |

### 9.4 订单管理 (OrderAdminController)

**基础路径**: `/api/admin/order`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/order/list` | orderNo, userName, status, startDate, endDate, pageNum, pageSize | 订单列表 |
| GET | `/admin/order/aftersale/list` | status, page, size | 售后/退款列表 |
| GET | `/admin/order/detail/{orderId}` | Path: orderId | 订单详情 |
| POST | `/admin/order/aftersale/handle` | body | 处理售后 |

### 9.5 Banner管理 (BannerController)

**基础路径**: `/api/admin/banner`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/banner/list` | position | Banner列表 |
| POST | `/admin/banner/create` | body | 创建Banner |
| PUT | `/admin/banner/update` | body | 更新Banner |
| DELETE | `/admin/banner/{id}` | Path: id | 删除Banner |

### 9.6 拍卖管理 (AuctionAdminController)

**基础路径**: `/api/admin/auction`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/auction/list` | page, size, status | 拍卖列表 |
| POST | `/admin/auction/create` | body | 创建拍卖 |
| PUT | `/admin/auction/update` | body | 更新拍卖 |
| POST | `/admin/auction/publish/{id}` | Path: id | 发布拍卖 |
| DELETE | `/admin/auction/{id}` | Path: id | 删除拍卖 |

### 9.7 拍卖统计 (AuctionStatsController)

**基础路径**: `/api/admin/auction/stats`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/auction/stats/overview` | - | 拍卖统计概览 |
| GET | `/admin/auction/stats/daily` | date | 日统计 |
| GET | `/admin/auction/stats/monthly` | year, month | 月统计 |

### 9.8 社区管理 (CommunityAdminController)

**基础路径**: `/api/admin/community`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/community/moment/list` | page, size, status | 动态列表 |
| POST | `/admin/community/moment/{id}/hide` | Path: id | 隐藏动态 |
| POST | `/admin/community/moment/{id}/show` | Path: id | 显示动态 |
| DELETE | `/admin/community/comment/{id}` | Path: id | 删除评论 |

### 9.9 消息管理 (MessageAdminController)

**基础路径**: `/api/admin/message`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/message/list` | page, size, type | 消息列表 |
| POST | `/admin/message/send` | body: {userIds, title, content, type} | 发送消息 |
| DELETE | `/admin/message/{id}` | Path: id | 删除消息 |

### 9.10 操作日志 (OperationLogController)

**基础路径**: `/api/admin/log`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/log/list` | page, size, adminId, action | 操作日志列表 |

### 9.11 权限管理 (PermissionController)

**基础路径**: `/api/admin/permission`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/permission/list` | - | 权限列表 |
| GET | `/admin/permission/roles` | - | 角色列表 |
| POST | `/admin/permission/role` | body | 创建角色 |
| PUT | `/admin/permission/role/{id}` | Path: id, body | 更新角色 |
| DELETE | `/admin/permission/role/{id}` | Path: id | 删除角色 |
| GET | `/admin/permission/admins` | page, size | 管理员列表 |
| POST | `/admin/permission/admin` | body | 创建管理员 |
| PUT | `/admin/permission/admin/{id}` | Path: id, body | 更新管理员 |
| DELETE | `/admin/permission/admin/{id}` | Path: id | 删除管理员 |

### 9.12 推广统计 (PromotionStatsController)

**基础路径**: `/api/admin/promotion/stats`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/promotion/stats/overview` | - | 推广统计概览 |
| GET | `/admin/promotion/stats/daily` | date | 日统计 |
| GET | `/admin/promotion/stats/monthly` | year, month | 月统计 |

### 9.13 系统配置 (SystemConfigController)

**基础路径**: `/api/admin/system`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/admin/system/config` | key | 获取配置 |
| PUT | `/admin/system/config` | body: {key, value} | 更新配置 |

---

## 附录

### 状态码说明

#### 作品状态 (status)
| 值 | 说明 |
|---|------|
| 0 | 下架 |
| 1 | 上架 |
| 2 | 审核中 |

#### 订单状态 (status)
| 值 | 说明 |
|---|------|
| 0 | 待支付 |
| 1 | 已支付 |
| 2 | 已发货 |
| 3 | 已完成 |
| 4 | 已取消 |
| 5 | 退款中 |
| 6 | 已退款 |

#### 用户身份 (identity)
| 值 | 说明 |
|---|------|
| artist | 艺术家 |
| agent | 经纪人 |
| collector | 收藏家 |
| promoter | 艺荐官 |

#### 艺术家认证状态 (status)
| 值 | 说明 |
|---|------|
| 0 | 待审核 (pending) |
| 1 | 已认证 (approved) |
| 2 | 已拒绝 (rejected) |

#### 艺术家等级 (badge)
| 值 | 说明 |
|---|------|
| master | 大师 |
| popular | 人气 |
| verified | 认证 |
| new | 新锐 |
