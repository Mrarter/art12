# ID迁移使用指南

## 一、迁移步骤

### 步骤1: 执行SQL迁移脚本
```bash
# 登录MySQL数据库
mysql -u root -p shiyiju

# 执行迁移脚本
source backend/sql/id_migration.sql
```

### 步骤2: 重启后端服务
确保Product API服务已重启，以加载新的Entity类和Mapper。

### 步骤3: 执行ID迁移（通过API或定时任务）
```bash
# 通过curl调用迁移接口（需要先实现Controller）
curl -X POST http://localhost:8082/api/admin/id-migration/migrate-all
```

## 二、新ID格式说明

```
格式: [种类前缀(3位)][日期YYYYMMDD(8位)][4位序列号][4位随机码]
总长度: 19位

示例:
- USR202604250001A7KM (用户)
- SES202604250001X9N2 (专场)
- LOT202604250001P3QR (拍品)
- POST202604250001W5ST (帖子)
- CMT202604250001K8MN (评论)
- TOP202604250001J2LP (话题)
- WTH202604250001H7UV (提现记录)
- COM202604250001F4WX (佣金记录)
- BID202604250001D1YZ (竞拍记录)
- ASM202604250001C6AB (售后管理)
- ART202604250001B3CD (作品)
```

## 三、代码中使用示例

### 生成新ID
```java
import com.shiyiju.product.config.IdGenerator;

// 生成各类型ID
String userId = IdGenerator.generateUserId();        // USR202604250001A7KM
String sessionId = IdGenerator.generateSessionId();  // SES202604250001X9N2
String lotId = IdGenerator.generateLotId();         // LOT202604250001P3QR
String postId = IdGenerator.generatePostId();        // POST202604250001W5ST
String commentId = IdGenerator.generateCommentId();  // CMT202604250001K8MN
String topicId = IdGenerator.generateTopicId();      // TOP202604250001J2LP
String withdrawId = IdGenerator.generateWithdrawId(); // WTH202604250001H7UV
String commissionId = IdGenerator.generateCommissionId(); // COM202604250001F4WX
String bidId = IdGenerator.generateBidId();         // BID202604250001D1YZ
String afterSaleId = IdGenerator.generateAfterSaleId(); // ASM202604250001C6AB
String artworkId = IdGenerator.generateArtworkId();  // ART202604250001B3CD
```

### 验证ID格式
```java
boolean isValid = IdGenerator.isValid("USR202604250001A7KM"); // true
String type = IdGenerator.getTypeDescription("USR202604250001A7KM"); // "用户"
String date = IdGenerator.extractDate("USR202604250001A7KM"); // "20260425"
String prefix = IdGenerator.extractPrefix("USR202604250001A7KM"); // "USR"
```

### 在Entity中使用
```java
// Artwork实体
Artwork artwork = new Artwork();
artwork.setId(1L); // 旧ID保留用于关联
artwork.setArtworkUid(IdGenerator.generateArtworkId()); // 新ID
artwork.setTitle("我的作品");
artworkMapper.insert(artwork);

// 查询时
Artwork result = artworkMapper.selectById(1L);
String newUid = result.getArtworkUid(); // USR202604250001B3CD
```

## 四、ID映射表使用

迁移过程中，`id_mapping` 表记录了新旧ID的对应关系：

```sql
-- 查看映射记录
SELECT * FROM id_mapping WHERE entity_type = 'user';

-- 根据旧ID查询新ID
SELECT new_id FROM id_mapping WHERE entity_type = 'user' AND old_id = 123;

-- 根据新ID查询旧ID
SELECT old_id FROM id_mapping WHERE entity_type = 'user' AND new_id = 'USR202604250001A7KM';
```

## 五、关联表更新

迁移完成后，需要更新所有关联表的外键引用。参考 `IdMigrationService` 的实现：

```java
// 示例：更新订单中的用户ID引用
// 假设 order_info 表中有 buyer_id 字段引用 sys_user.id
// 需要将 buyer_id 从旧ID更新为新ID

// 1. 获取旧ID到新ID的映射
Map<Long, String> userMapping = idMappingCache.get("user");

// 2. 更新关联表
for (OrderInfo order : orderList) {
    String newBuyerId = userMapping.get(order.getBuyerId());
    if (newBuyerId != null) {
        order.setBuyerIdNew(newBuyerId); // 需要先添加新字段
    }
}
```

## 六、回滚方案

如需回滚到旧ID系统：

```sql
-- 1. 删除新ID字段
ALTER TABLE `sys_user` DROP COLUMN `uid`;

-- 2. 恢复其他表的新ID字段（如果有的话）
ALTER TABLE `auction_session` DROP COLUMN `session_code`;
-- ... 其他表类似

-- 3. 删除ID映射表
DROP TABLE `id_mapping`;
```

## 七、注意事项

1. **数据备份**: 迁移前务必备份数据库
2. **灰度发布**: 建议先在测试环境验证
3. **事务保证**: ID迁移和关联更新应在同一事务中完成
4. **索引创建**: 确保新ID字段有唯一索引
5. **代码更新**: 迁移完成后需要更新所有API的返回格式
