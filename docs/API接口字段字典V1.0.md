# 拾艺局 API 接口字段字典 V1.0

> 文档版本：V1.2  
> 更新时间：2026-04-24 13:07  
> 项目地址：https://github.com/Mrarter/art12

---

## 修复记录

### V1.2 (2026-04-24)
1. **修复运营后台 Banner 列表显示 0 条问题**
   - 问题：代理配置 `/api/admin/system` 错误映射为 `/admin/system/banner`
   - 修复：调整为 `/admin` 前缀，正确映射到 `/admin/banner`
   - 文件：`admin/vite.config.js`

2. **修复小程序前端无法显示真实数据**
   - 问题：缺少 `api/product.js` 文件，首页 API 调用失败
   - 修复：创建完整的 API 定义文件
   - 问题：Banner API 路径错误 `/banner/list` → `/product/homepage/banners`
   - 文件：`test-miniprogram/api/product.js`, `test-miniprogram/services/home/home.js`

3. **艺术家列表说明**
   - API 正常工作，数据存在于数据库
   - 默认显示 `pending`（待审核）tab，需切换到 `approved` 查看已认证艺术家

---

## 服务地址

---

## 一、服务地址

| 服务 | 端口 | 说明 |
|------|------|------|
| 后台管理 (admin) | 8090 | 运营后台 API |
| 商品服务 (product) | 8082 | 作品/商品 API |
| API 网关 | 8080 | 统一入口（未启用）|

---

## 二、已验证可用接口

### 2.1 商品/作品服务 (localhost:8082)

#### GET /product/list - 作品列表

请求参数：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认10 |
| status | int | 否 | 状态：1-上架，0-下架 |

响应示例：
```json
{
  "code": 200,
  "data": {
    "total": 22,
    "records": [{
      "id": 19,
      "title": "远航",
      "authorName": "王强",
      "coverImage": "http://localhost:8087/upload/images/...",
      "price": 0,
      "originalPrice": 0,
      "status": 1,
      "isHot": false,
      "isNew": true,
      "artType": null,
      "size": "100*80",
      "year": 2024,
      "createTime": "2026-04-20T18:22:57"
    }]
  }
}
```

#### GET /product/banners - Banner轮播图

响应示例：
```json
{
  "code": 200,
  "data": [{
    "id": 5,
    "title": "124",
    "imageUrl": "http://localhost:8087/upload/images/...",
    "linkType": "OTHER",
    "linkValue": "2",
    "sort": 1,
    "status": 1
  }]
}
```

#### GET /product/{id} - 作品详情

#### GET /product/recommend - 推荐作品

#### GET /product/following - 关注作品

#### GET /product/categories - 作品分类

#### GET /product/search - 搜索作品

#### POST /product/artwork/favorite/{id} - 收藏作品

#### POST /product/artwork/unfavorite/{id} - 取消收藏

#### POST /product/create - 创建作品

### 2.2 后台管理服务 (localhost:8090)

#### GET /admin/system/admin/list - 管理员列表

响应示例：
```json
{
  "code": 200,
  "data": [{
    "id": 1,
    "username": "admin",
    "email": "admin@shiyiju.com",
    "phone": "13800000000",
    "role": "super",
    "status": 1,
    "lastLoginTime": "2026-04-24T10:45:16",
    "createTime": "2026-04-24T10:26:26"
  }]
}
```

#### GET /admin/permission/admins - 管理员列表（分页）

#### POST /admin/system/admin - 添加管理员

#### PUT /admin/system/admin/{id} - 更新管理员

#### DELETE /admin/system/admin/{id} - 删除管理员

#### GET /admin/banner/list - Banner列表

#### POST /admin/banner - 添加Banner

#### PUT /admin/banner/{id} - 更新Banner

#### GET /admin/product/list - 后台作品列表

#### PUT /admin/product/{id} - 更新作品

---

## 三、前端配置

### 3.1 小程序配置 (test-miniprogram/config/index.js)

```javascript
export const config = {
  useMock: false,  // 已关闭 Mock，使用真实 API
  apiBase: 'http://localhost:8082',
};
```

### 3.2 H5前端配置 (frontend/src/api/request.js)

```javascript
const BASE_URL = 'http://localhost:8090/api'
```

---

*最后更新：2026-04-24*

---

## 一、通用规范

### 1.1 统一响应格式

所有接口统一返回 `Result<T>` 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-04-23T00:00:00"
}
```

### 1.2 分页响应格式

```json
{
  "total": 100,
  "page": 1,
  "pageSize": 20,
  "totalPages": 5,
  "records": []
}
```

### 1.3 通用状态码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录/Token无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 1.4 请求头

| Header | 必填 | 说明 |
|--------|------|------|
| X-User-Id | 否 | 用户ID（部分接口需要） |
| Authorization | 否 | Bearer Token |

---

## 二、用户模块 (user)

### 2.1 认证相关

#### POST /user/auth/wx-login 微信登录

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | String | 是 | 微信授权码 |
| encryptedData | String | 否 | 微信加密数据 |
| iv | String | 否 | 加密向量 |
| nickname | String | 否 | 昵称 |
| avatar | String | 否 | 头像URL |
| gender | Integer | 否 | 性别 0-未知 1-男 2-女 |
| birthday | String | 否 | 生日 YYYY-MM-DD |
| region | String | 否 | 地区 |
| inviteCode | String | 否 | 邀请码 |

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| token | String | JWT Token |
| isNewUser | Boolean | 是否新用户 |
| userId | Long | 用户ID |
| nickname | String | 昵称 |
| avatar | String | 头像 |
| identities | String | 身份列表，逗号分隔 |

#### GET /user/info 获取用户信息

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |
| nickname | String | 昵称 |
| avatar | String | 头像 |
| phone | String | 手机号 |
| gender | Integer | 性别 |
| birthday | String | 生日 |
| region | String | 地区 |
| identities | String | 身份列表 |
| isArtist | Boolean | 是否艺术家 |
| isPromoter | Boolean | 是否艺荐官 |
| isVerified | Boolean | 是否已认证 |
| followerCount | Integer | 粉丝数 |
| followingCount | Integer | 关注数 |
| createTime | String | 注册时间 |

#### PUT /user/user/update 更新用户信息

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nickname | String | 否 | 昵称 |
| avatar | String | 否 | 头像 |
| gender | Integer | 否 | 性别 |
| birthday | String | 否 | 生日 |
| region | String | 否 | 地区 |

#### POST /user/user/bind-phone 绑定手机号

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | String | 是 | 手机号 |
| verifyCode | String | 是 | 验证码 |

### 2.2 艺术家认证

#### POST /user/user/artist/cert 申请艺术家认证

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| realName | String | 是 | 真实姓名 |
| idCard | String | 是 | 身份证号 |
| idCardFront | String | 是 | 身份证正面照 |
| idCardBack | String | 是 | 身份证反面照 |
| artType | String | 是 | 艺术类型 |
| bio | String | 否 | 个人简介 |
| portfolio | String | 否 | 作品集链接 |

#### GET /user/artist/cert/status 获取认证状态

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| status | Integer | 状态 0-未申请 1-审核中 2-已通过 3-已拒绝 |
| statusText | String | 状态文本 |
| rejectReason | String | 拒绝原因 |

### 2.3 关注功能

#### POST /user/artist/{artistId}/follow 关注艺术家

#### DELETE /user/artist/{artistId}/follow 取消关注

#### GET /user/artist/{userId} 获取艺术家主页

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |
| nickname | String | 昵称 |
| avatar | String | 头像 |
| bio | String | 简介 |
| isVerified | Boolean | 是否认证 |
| badge | String | 认证标签 |
| followerCount | Integer | 粉丝数 |
| followingCount | Integer | 关注数 |
| artworkCount | Integer | 作品数 |
| isFollowing | Boolean | 当前用户是否关注 |

### 2.4 个人中心

#### GET /user/center 获取个人中心数据

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| orderCount | Integer | 订单数 |
| favoriteCount | Integer | 收藏数 |
| followCount | Integer | 关注数 |
| balance | Decimal | 余额 |

#### GET /user/addresses 获取收货地址列表

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 地址ID |
| receiverName | String | 收货人 |
| phone | String | 手机号 |
| province | String | 省份 |
| city | String | 城市 |
| district | String | 区县 |
| detailAddress | String | 详细地址 |
| isDefault | Boolean | 是否默认 |

#### POST /user/addresses 添加地址

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| receiverName | String | 是 | 收货人 |
| phone | String | 是 | 手机号 |
| province | String | 是 | 省份 |
| city | String | 是 | 城市 |
| district | String | 是 | 区县 |
| detailAddress | String | 是 | 详细地址 |
| isDefault | Boolean | 否 | 是否默认 |

#### PUT /user/addresses/{id} 更新地址

#### DELETE /user/addresses/{id} 删除地址

#### GET /user/favorites 获取收藏列表

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| total | Integer | 总数 |
| page | Integer | 页码 |
| pageSize | Integer | 每页数量 |
| records | Array | 收藏列表 |

#### GET /user/history 获取浏览足迹

---

## 三、作品模块 (product)

### 3.1 作品列表

#### GET /product/list 或 GET /product/artwork/list

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 默认1 |
| pageSize | Integer | 否 | 每页数量 默认20 |
| categoryId | Long | 否 | 分类ID |
| sort | String | 否 | 排序: price_asc/price_desc/time |
| minPrice | Long | 否 | 最低价格 |
| maxPrice | Long | 否 | 最高价格 |
| yearFrom | Integer | 否 | 创作年代起始 |
| yearTo | Integer | 否 | 创作年代结束 |
| artistId | Long | 否 | 艺术家ID |
| keyword | String | 否 | 搜索关键词 |

**响应字段（ArtworkVO）：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 作品ID |
| title | String | 标题 |
| authorId | Long | 作者ID |
| authorName | String | 作者名称 |
| authorAvatar | String | 作者头像 |
| authorBadge | String | 作者标签 |
| categoryId | Long | 分类ID |
| category | String | 分类名称 |
| artType | String | 艺术品类 |
| size | String | 尺寸 |
| material | String | 材质 |
| createYear | Integer | 创作年份 |
| edition | String | 版数 |
| description | String | 描述 |
| cover | String | 封面图 |
| images | Array | 图片列表 |
| source | Integer | 来源 1-艺术家 2-画廊 3-藏家 |
| sourceText | String | 来源文本 |
| price | Long | 当前价格（分） |
| originalPrice | Long | 原价（分） |
| currentPrice | Long | 当前价格（分） |
| stock | Integer | 库存 |
| status | Integer | 状态 0-下架 1-上架 |
| statusText | String | 状态文本 |
| viewCount | Integer | 浏览数 |
| favoriteCount | Integer | 收藏数 |
| saleCount | Integer | 销量 |
| rating | Double | 评分 |
| reviewCount | Integer | 评价数 |
| isFavorite | Boolean | 是否收藏 |
| isFollowing | Boolean | 是否关注作者 |
| priceRise | Double | 价格涨幅 |
| holdDuration | Integer | 持有天数 |
| ownershipType | Integer | 所有权类型 1-原创 2-二手 |
| ownershipTypeText | String | 所有权类型文本 |
| artworkCode | String | 作品编号 |
| distributionEnabled | Boolean | 是否开启分销 |
| commissionRate | Integer | 佣金比例 |
| createTime | String | 创建时间 |

### 3.2 作品详情

#### GET /product/{id} 获取作品详情

**响应字段：** 同 ArtworkVO

### 3.3 作品操作

#### POST /product/favorite 收藏作品

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| artworkId | Long | 是 | 作品ID |

#### DELETE /product/favorite/{id} 取消收藏

#### GET /product/favorites 获取收藏列表

#### GET /product/recommend 获取推荐作品

#### GET /product/following 获取关注艺术家作品

### 3.4 分类与Banner

#### GET /product/banners 或 GET /product/homepage/banners

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | Banner ID |
| title | String | 标题 |
| imageUrl | String | 图片URL |
| linkType | Integer | 链接类型 1-作品 2-艺术家 3-H5 |
| linkValue | String | 链接值 |
| sort | Integer | 排序 |
| status | Integer | 状态 |

#### GET /product/categories

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 分类ID |
| name | String | 分类名称 |
| icon | String | 图标 |
| parentId | Long | 父级ID |
| sort | Integer | 排序 |

### 3.5 评价模块

#### POST /review 创建评价

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| artworkId | Long | 是 | 作品ID |
| orderId | Long | 是 | 订单ID |
| orderItemId | Long | 是 | 订单项ID |
| rating | Integer | 是 | 总体评分 1-5 |
| content | String | 是 | 评价内容 |
| images | String | 否 | 图片，多个用逗号分隔 |
| qualityRating | Integer | 否 | 质量评分 |
| logisticsRating | Integer | 否 | 物流评分 |
| serviceRating | Integer | 否 | 服务评分 |
| isAnonymous | Integer | 否 | 是否匿名 0-否 1-是 |

#### GET /review/artwork/{artworkId} 获取作品评价列表

**响应字段（ReviewVO）：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 评价ID |
| orderId | Long | 订单ID |
| userId | Long | 用户ID |
| userNickname | String | 用户昵称 |
| userAvatar | String | 用户头像 |
| rating | Integer | 评分 |
| content | String | 内容 |
| images | String | 图片列表 |
| qualityRating | Integer | 质量评分 |
| logisticsRating | Integer | 物流评分 |
| serviceRating | Integer | 服务评分 |
| isAnonymous | Integer | 是否匿名 |
| reply | String | 商家回复 |
| createTime | String | 创建时间 |

#### GET /review/artwork/{artworkId}/stats 获取评价统计

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| totalCount | Integer | 总数 |
| averageRating | Double | 平均评分 |
| ratingDistribution | Object | 评分分布 {5: 10, 4: 5, ...} |

#### POST /review/{id}/reply 商家回复

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | String | 是 | 回复内容 |

---

## 四、订单模块 (order)

### 4.1 订单操作

#### POST /order/create 创建订单

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| items | Array | 是 | 订单项列表 |
| items[].artworkId | Long | 是 | 作品ID |
| items[].quantity | Integer | 是 | 数量 |
| addressId | Long | 是 | 地址ID |
| remark | String | 否 | 备注 |
| couponId | Long | 否 | 优惠券ID |

**响应字段（OrderVO）：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| orderId | String | 订单ID |
| orderNo | String | 订单编号 |
| totalAmount | Long | 总金额（分） |
| discountAmount | Long | 优惠金额（分） |
| freightAmount | Long | 运费（分） |
| actualAmount | Long | 实付金额（分） |
| status | Integer | 状态 |
| statusText | String | 状态文本 |
| createTime | String | 创建时间 |
| payTime | String | 支付时间 |
| shipTime | String | 发货时间 |
| receiveTime | String | 收货时间 |
| items | Array | 订单项列表 |

#### GET /order/{orderId} 获取订单详情

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| orderId | String | 订单ID |
| orderNo | String | 订单编号 |
| totalAmount | Long | 总金额 |
| discountAmount | Long | 优惠 |
| freightAmount | Long | 运费 |
| actualAmount | Long | 实付 |
| status | Integer | 状态 |
| statusText | String | 状态文本 |
| address | Object | 收货地址 |
| items | Array | 订单项 |

**订单状态说明：**

| status | statusText | 说明 |
|--------|------------|------|
| 10 | pending_pay | 待支付 |
| 20 | paid | 已支付 |
| 30 | shipped | 已发货 |
| 40 | received | 已收货 |
| 50 | completed | 已完成 |
| -10 | cancelled | 已取消 |
| -20 | refunded | 已退款 |

#### GET /order/list 获取订单列表

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | Integer | 否 | 订单状态筛选 |
| page | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |

#### PUT /order/{orderId}/cancel 取消订单

#### PUT /order/{orderId}/pay 支付订单

#### PUT /order/{orderId}/confirm 确认收货

### 4.2 购物车

#### GET /cart 获取购物车列表

**响应字段（CartVO）：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 购物车ID |
| artworkId | Long | 作品ID |
| artworkTitle | String | 作品标题 |
| artworkCover | String | 作品封面 |
| authorName | String | 作者 |
| price | Long | 价格（分） |
| stock | Integer | 库存 |
| quantity | Integer | 数量 |
| selected | Boolean | 是否选中 |

#### POST /cart/add 加入购物车

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| artworkId | Long | 是 | 作品ID |
| quantity | Integer | 是 | 数量 |

#### PUT /cart/{id} 更新数量

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| quantity | Integer | 是 | 数量 |

#### DELETE /cart/{id} 删除

#### PUT /cart/select 批量选择

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | Array | 是 | 购物车ID列表 |
| selected | Boolean | 是 | 是否选中 |

### 4.3 物流

#### GET /logistics/{orderId} 获取物流信息

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| logisticsNo | String | 物流单号 |
| company | String | 物流公司 |
| companyCode | String | 物流公司编码 |
| status | Integer | 物流状态 |
| statusText | String | 状态文本 |
| tracks | Array | 物流轨迹 |

#### GET /logistics/track 获取物流轨迹

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| time | String | 时间 |
| content | String | 内容 |
| location | String | 地点 |

---

## 五、拍卖模块 (auction)

### 5.1 拍卖场次

#### GET /auction/sessions 获取拍卖场次列表

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 场次ID |
| name | String | 场次名称 |
| coverImage | String | 封面图 |
| startTime | String | 开始时间 |
| endTime | String | 结束时间 |
| status | Integer | 状态 |
| statusText | String | 状态文本 |
| lotCount | Integer | 拍品数量 |
| currentSession | Boolean | 是否当前场次 |

### 5.2 拍品

#### GET /auction/lots/{sessionId} 获取场次拍品

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 拍品ID |
| artworkId | Long | 作品ID |
| title | String | 标题 |
| coverImage | String | 封面 |
| authorName | String | 作者 |
| startPrice | Long | 起拍价（分） |
| currentPrice | Long | 当前价（分） |
| bidCount | Integer | 出价次数 |
| status | Integer | 状态 |
| endTime | String | 结束时间 |

#### GET /auction/lot/{id} 获取拍品详情

### 5.3 出价

#### POST /auction/bid 出价

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lotId | Long | 是 | 拍品ID |
| price | Long | 是 | 出价金额（分） |

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| bidId | Long | 出价ID |
| currentPrice | Long | 当前价 |
| myBid | Boolean | 是否最高 |
| isWon | Boolean | 是否中标 |

### 5.4 保证金

#### POST /auction/deposit 缴纳保证金

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lotId | Long | 是 | 拍品ID |

#### GET /auction/my-deposits 获取我的保证金

---

## 六、推广分销模块 (promotion)

### 6.1 艺荐官

#### POST /user/user/promoter/open 开通艺荐官

#### GET /user/user/promoter/invite-code 获取邀请码

### 6.2 收益

#### GET /promotion/earnings 获取收益概览

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| totalEarnings | Long | 累计收益（分） |
| pendingEarnings | Long | 待确认收益（分） |
| withdrawnEarnings | Long | 已提现收益（分） |
| todayEarnings | Long | 今日收益（分） |
| orderCount | Integer | 成交订单数 |
| followerCount | Integer | 粉丝数 |

#### GET /promotion/earnings/trend 获取收益趋势

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| period | String | 否 | 周期 week/month |

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| date | String | 日期 |
| amount | Long | 金额 |

#### GET /promotion/earnings/detail 获取收益明细

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 记录ID |
| orderId | String | 订单ID |
| artworkTitle | String | 作品标题 |
| artworkCover | String | 作品封面 |
| buyerNickname | String | 买家昵称 |
| orderAmount | Long | 订单金额 |
| commission | Long | 佣金 |
| rate | Double | 佣金比例 |
| status | Integer | 状态 0-待确认 1-已确认 2-已结算 |
| createTime | String | 创建时间 |
| confirmTime | String | 确认时间 |

### 6.3 提现

#### GET /promotion/withdraw/rate 获取提现费率

#### POST /promotion/withdraw 申请提现

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| amount | Long | 是 | 提现金额（分） |
| type | Integer | 是 | 类型 1-微信 2-银行卡 |

#### GET /promotion/withdraw/logs 获取提现记录

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 记录ID |
| amount | Long | 金额 |
| type | Integer | 类型 |
| status | Integer | 状态 |
| statusText | String | 状态文本 |
| createTime | String | 申请时间 |
| handleTime | String | 处理时间 |
| reason | String | 失败原因 |

### 6.4 排行榜

#### GET /promotion/ranking 获取收益排行

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| period | String | 否 | week/month/all |

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| rank | Integer | 排名 |
| userId | Long | 用户ID |
| nickname | String | 昵称 |
| avatar | String | 头像 |
| earnings | Long | 收益 |

---

## 七、社区模块 (community)

### 7.1 帖子

#### POST /community/post 发布帖子

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | String | 是 | 内容 |
| images | String | 否 | 图片，多个用逗号分隔 |
| topicId | Long | 否 | 话题ID |
| artworkId | Long | 否 | 关联作品ID |

#### GET /community/post/{id} 获取帖子详情

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 帖子ID |
| userId | Long | 用户ID |
| userNickname | String | 用户昵称 |
| userAvatar | String | 用户头像 |
| identityType | String | 身份类型 |
| content | String | 内容 |
| images | String | 图片列表 |
| topicId | Long | 话题ID |
| topicName | String | 话题名称 |
| artworkId | Long | 关联作品ID |
| artworkTitle | String | 作品标题 |
| artworkCover | String | 作品封面 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| shareCount | Integer | 分享数 |
| isLiked | Boolean | 是否点赞 |
| isFollowed | Boolean | 是否关注 |
| createTime | String | 创建时间 |

#### DELETE /community/post/{id} 删除帖子

#### GET /community/posts 获取帖子列表

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 否 | 类型 following/recommend |
| page | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |

### 7.2 点赞

#### POST /community/post/{id}/like 点赞

#### DELETE /community/post/{id}/like 取消点赞

### 7.3 评论

#### GET /community/post/{id}/comments 获取评论列表

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 评论ID |
| userId | Long | 用户ID |
| userNickname | String | 昵称 |
| userAvatar | String | 头像 |
| content | String | 内容 |
| parentId | Long | 父评论ID |
| replyTo | String | 回复谁 |
| likeCount | Integer | 点赞数 |
| isLiked | Boolean | 是否点赞 |
| createTime | String | 创建时间 |
| replies | Array | 回复列表 |

#### POST /community/post/{id}/comment 评论

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | String | 是 | 内容 |
| parentId | Long | 否 | 父评论ID |

### 7.4 话题

#### GET /community/topics 获取话题列表

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 话题ID |
| name | String | 话题名称 |
| description | String | 描述 |
| coverImage | String | 封面 |
| postCount | Integer | 帖子数 |
| followCount | Integer | 关注数 |
| isFollowed | Boolean | 是否关注 |

#### GET /community/topic/{id}/posts 获取话题帖子

---

## 八、消息模块 (message)

### 8.1 消息列表

#### GET /message/list 获取消息列表

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 否 | 类型 system/order/activity |
| page | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页数量 |

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 消息ID |
| type | String | 类型 |
| title | String | 标题 |
| content | String | 内容 |
| data | Object | 扩展数据 |
| isRead | Boolean | 是否已读 |
| createTime | String | 创建时间 |

### 8.2 消息操作

#### PUT /message/read/{messageId} 标记已读

#### PUT /message/read-all 全部已读

#### DELETE /message/{messageId} 删除消息

#### GET /message/unread-count 获取未读数

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| total | Integer | 总未读数 |
| system | Integer | 系统消息未读数 |
| order | Integer | 订单消息未读数 |
| activity | Integer | 活动消息未读数 |

---

## 九、文件模块 (file)

### 9.1 文件上传

#### POST /file/upload 上传文件

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 文件 |
| type | String | 否 | 类型 image/video/avatar |

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| url | String | 文件URL |
| filename | String | 文件名 |
| size | Long | 文件大小 |
| mimeType | String | MIME类型 |

#### POST /file/upload/base64 Base64上传

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | String | 是 | Base64字符串 |
| filename | String | 是 | 文件名 |
| type | String | 否 | 类型 |

---

## 十、系统配置模块

### 10.1 配置获取

#### GET /admin/config/all 获取所有配置

**响应字段：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| priceGrowth | Object | 价格增长配置 |
| promotion | Object | 推广配置 |
| trade | Object | 交易配置 |

#### GET /admin/config/priceGrowth 获取价格增长配置

#### GET /admin/config/promotion 获取推广配置

| 字段名 | 类型 | 说明 |
|--------|------|------|
| enabled | Boolean | 是否开启 |
| minWithdraw | Long | 最低提现金额（分） |
| feeRate | Double | 提现费率 |
| commissionRates | Object | 佣金比例配置 |

---

## 附录：字段类型说明

| 类型 | 说明 |
|------|------|
| Long | 长整数，金额单位通常为分 |
| Integer | 整数 |
| String | 字符串 |
| Boolean | 布尔值 |
| Double | 浮点数 |
| Array | 数组 |
| Object | 对象 |
| Date/String | 日期时间，格式 ISO8601 |

---

*文档结束*
