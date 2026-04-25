# ID命名规范

## 一、ID格式标准

### 1.1 格式定义

```
[分类前缀(3位)][日期YYYYMMDD(8位)][4位序号][4位随机码]
```

| 组成部分 | 长度 | 说明 |
|---------|------|------|
| 分类前缀 | 3位 | 标识实体类型 |
| 日期 | 8位 | 生成日期，格式YYYYMMDD |
| 序号 | 4位 | 每日从0001开始递增 |
| 随机码 | 4位 | 大写字母+数字组合 |
| **总计** | **19位** | |

### 1.2 示例

```
USR202604250001A7KM  (用户)
SES202604250001X9N2  (专场)
LOT202604250001P3QR  (拍品)
ART202604250001B3CD  (作品)
POST202604250001W5ST (帖子)
CMT202604250001K8MN  (评论)
TOP202604250001J2LP  (话题)
WTH202604250001H7UV  (提现记录)
COM202604250001F4WX  (佣金记录)
BID202604250001D1YZ  (竞拍记录)
ASM202604250001C6AB  (售后记录)
ORD202604250001E5GH  (订单)
PAY202604250001R8JK  (支付记录)
```

---

## 二、分类前缀定义

| 前缀 | 实体类型 | 说明 | 所在服务 |
|------|---------|------|---------|
| USR | 用户 | User | shiyiju-user |
| ART | 作品 | Artwork | shiyiju-product |
| SES | 拍卖专场 | AuctionSession | shiyiju-product |
| LOT | 拍品 | AuctionLot | shiyiju-product |
| POST | 帖子 | CommunityPost | shiyiju-product |
| CMT | 评论 | PostComment | shiyiju-product |
| TOP | 话题 | Topic | shiyiju-product |
| WTH | 提现记录 | WithdrawRecord | shiyiju-product |
| COM | 佣金记录 | CommissionLog | shiyiju-product |
| BID | 竞拍记录 | AuctionBid | shiyiju-product |
| ASM | 售后记录 | RefundRecord/Aftersale | shiyiju-product |
| ORD | 订单 | OrderInfo | shiyiju-order |
| PAY | 支付记录 | PaymentRecord | shiyiju-order |
| PRC | 艺人认证 | ArtistCertification | shiyiju-user |
| PRM | 艺荐官 | PromoterRecord | shiyiju-user |

---

## 三、ID生成工具

### 3.1 核心类：IdGenerator

**文件位置**: `backend/shiyiju-product/src/main/java/com/shiyiju/product/config/IdGenerator.java`

**特性**:
- 线程安全
- 每日序号自动重置
- 支持所有实体类型

**使用方式**:

```java
import com.shiyiju.product.config.IdGenerator;

// 生成各类ID
String userId = IdGenerator.generateUserId();           // USR202604250001A7KM
String sessionId = IdGenerator.generateSessionId();   // SES202604250001X9N2
String lotId = IdGenerator.generateLotId();            // LOT202604250001P3QR
String artworkId = IdGenerator.generateArtworkId();    // ART202604250001B3CD
String postId = IdGenerator.generatePostId();           // POST202604250001W5ST
String commentId = IdGenerator.generateCommentId();    // CMT202604250001K8MN
String topicId = IdGenerator.generateTopicId();         // TOP202604250001J2LP
String withdrawId = IdGenerator.generateWithdrawId();  // WTH202604250001H7UV
String commissionId = IdGenerator.generateCommissionId(); // COM202604250001F4WX
String bidId = IdGenerator.generateBidId();             // BID202604250001D1YZ
String afterSaleId = IdGenerator.generateAfterSaleId(); // ASM202604250001C6AB

// 通用生成方式
String customId = IdGenerator.generateId("USR");        // USR202604250001A7KM
```

### 3.2 工具方法

```java
// 验证ID格式
boolean isValid = IdGenerator.isValid("USR202604250001A7KM"); // true

// 提取日期
String date = IdGenerator.extractDate("USR202604250001A7KM"); // "20260425"

// 提取前缀
String prefix = IdGenerator.extractPrefix("USR202604250001A7KM"); // "USR"

// 获取类型描述
String type = IdGenerator.getTypeDescription("USR202604250001A7KM"); // "用户"
```

---

## 四、数据库字段规范

### 4.1 表结构设计

每个实体表应包含以下字段：

```sql
-- 基础字段（保留）
id              BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 内部自增ID（不暴露给外部）

-- 标准UID字段（对外使用）
uid             VARCHAR(32) UNIQUE,                 -- 标准格式ID，如 USR202604250001A7KM

-- 字段命名规范
-- 关联表外键应使用 xxx_uid 格式，如 user_uid, artist_uid
```

### 4.2 索引规范

```sql
-- UID字段必须建立唯一索引
CREATE UNIQUE INDEX idx_uid ON table_name (uid);

-- 如需按日期查询，建立复合索引
CREATE INDEX idx_uid_date ON table_name (uid, create_time);
```

---

## 五、代码使用规范

### 5.1 新增记录时

```java
// ❌ 不推荐：使用自增ID
Artwork artwork = new Artwork();
artwork.setId(1L);

// ✅ 推荐：使用标准UID
Artwork artwork = new Artwork();
artwork.setUid(IdGenerator.generateArtworkId());
artwork.setTitle("我的作品");
artworkMapper.insert(artwork);
```

### 5.2 查询返回时

```java
// ✅ 返回标准UID给前端
Artwork artwork = artworkMapper.selectById(id);
return artwork.getUid();  // USR202604250001A7KM
```

### 5.3 关联查询时

```java
// ✅ 关联表使用uid字段
@Select("SELECT o.*, u.nickname as artist_name " +
        "FROM order_info o " +
        "LEFT JOIN sys_user u ON o.artist_uid = u.uid " +
        "WHERE o.uid = #{orderUid}")
```

---

## 六、API接口规范

### 6.1 请求参数

| 场景 | 字段名 | 示例 |
|------|--------|------|
| 按ID查询 | uid | USR202604250001A7KM |
| 列表筛选 | uid | USR202604250001A7KM |
| 精确匹配 | uid | USR202604250001A7KM |

### 6.2 响应格式

```json
{
  "code": 200,
  "data": {
    "uid": "USR202604250001A7KM",
    "nickname": "张三",
    "phone": "138****8888"
  }
}
```

---

## 七、历史遗留问题

### 7.1 已迁移的UID

用户表 `sys_user` 字段 `uid` 已统一使用标准格式：
- 旧格式：数值型自增ID
- 新格式：`USR` + 日期 + 序号 + 随机码

### 7.2 待迁移的实体

以下实体的 `uid` 字段需要按相同规范迁移：
- [ ] auction_session (专场)
- [ ] auction_lot (拍品)
- [ ] artwork (作品)
- [ ] community_post (帖子)
- [ ] post_comment (评论)
- [ ] topic (话题)
- [ ] withdraw_record (提现)
- [ ] commission_log (佣金)
- [ ] auction_bid (竞拍)
- [ ] refund_record (退款)

---

## 八、注意事项

1. **唯一性保证**：序号在每日范围内唯一，跨天可重复
2. **大小写敏感**：随机码全大写，与校验码计算一致
3. **不可推测性**：4位随机码确保ID无法被枚举
4. **可读性**：前缀+日期便于日志分析和问题排查
5. **兼容性**：19位长度兼容大多数数据库和前端框架

---

## 九、更新记录

| 日期 | 版本 | 更新内容 |
|------|------|---------|
| 2026-04-25 | v1.0 | 初始文档，创建ID格式规范 |
