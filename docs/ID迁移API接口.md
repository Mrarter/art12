# ID迁移API接口文档

## 基础信息

- **Base URL**: `http://localhost:8090` (Admin API)
- **认证**: 需要管理员Token

---

## 一、迁移接口

### 1. 执行全量ID迁移

```http
POST /api/admin/id-migration/migrate-all
```

**响应示例**:
```json
{
  "success": true,
  "message": "ID迁移任务执行成功"
}
```

---

### 2. 分项迁移接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/id-migration/migrate/users` | POST | 迁移用户ID |
| `/api/admin/id-migration/migrate/sessions` | POST | 迁移专场ID |
| `/api/admin/id-migration/migrate/lots` | POST | 迁移拍品ID |
| `/api/admin/id-migration/migrate/posts` | POST | 迁移帖子ID |
| `/api/admin/id-migration/migrate/comments` | POST | 迁移评论ID |
| `/api/admin/id-migration/migrate/topics` | POST | 迁移话题ID |
| `/api/admin/id-migration/migrate/withdraws` | POST | 迁移提现记录ID |
| `/api/admin/id-migration/migrate/commissions` | POST | 迁移佣金记录ID |
| `/api/admin/id-migration/migrate/bids` | POST | 迁移竞拍记录ID |
| `/api/admin/id-migration/migrate/aftersales` | POST | 迁移售后记录ID |

---

## 二、查询接口

### 3. 获取迁移状态

```http
GET /api/admin/id-migration/status
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "users": 1234,
    "artworks": 567,
    "sessions": 12,
    "lots": 89,
    "posts": 2345,
    "comments": 6789,
    "topics": 45,
    "withdraws": 123,
    "commissions": 456,
    "bids": 7890,
    "aftersales": 34
  }
}
```

---

### 4. 验证ID格式

```http
GET /api/admin/id-migration/validate/{id}
```

**示例**: `GET /api/admin/id-migration/validate/USR202604250001A7KM`

**响应示例**:
```json
{
  "success": true,
  "valid": true,
  "type": "用户",
  "date": "20260425"
}
```

---

## 三、Postman测试脚本

```javascript
// Postman Collection - ID Migration

// 全量迁移
pm.test("全量迁移成功", function() {
    pm.sendRequest({
        url: 'http://localhost:8090/api/admin/id-migration/migrate-all',
        method: 'POST',
        header: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + pm.collectionVariables.get("adminToken")
        }
    }, function(err, res) {
        var jsonData = res.json();
        pm.expect(jsonData.success).to.eq(true);
    });
});

// 验证迁移状态
pm.test("迁移状态检查", function() {
    pm.sendRequest({
        url: 'http://localhost:8090/api/admin/id-migration/status',
        method: 'GET',
        header: {
            'Authorization': 'Bearer ' + pm.collectionVariables.get("adminToken")
        }
    }, function(err, res) {
        var jsonData = res.json();
        console.log("当前数据统计:", jsonData.data);
    });
});

// 验证ID格式
pm.test("ID格式验证", function() {
    var testId = "USR202604250001A7KM";
    pm.sendRequest({
        url: 'http://localhost:8090/api/admin/id-migration/validate/' + testId,
        method: 'GET'
    }, function(err, res) {
        var jsonData = res.json();
        pm.expect(jsonData.valid).to.eq(true);
        pm.expect(jsonData.type).to.eq("用户");
    });
});
```

---

## 四、迁移建议顺序

如果分步迁移，建议按以下顺序执行：

1. **用户 (users)** - 基础数据，其他表依赖
2. **作品 (artworks)** - 商品数据
3. **专场 (sessions)** - 拍卖专场
4. **拍品 (lots)** - 依赖于专场
5. **话题 (topics)** - 社区基础数据
6. **帖子 (posts)** - 依赖于用户和话题
7. **评论 (comments)** - 依赖于帖子
8. **提现记录 (withdraws)** - 依赖于用户
9. **佣金记录 (commissions)** - 依赖于用户和订单
10. **竞拍记录 (bids)** - 依赖于拍品和用户
11. **售后记录 (aftersales)** - 依赖于订单

---

## 五、日志查看

迁移过程中查看日志：

```bash
# Product API日志
tail -f logs/shiyiju-product.log | grep "ID迁移"

# Admin API日志
tail -f logs/shiyiju-admin.log | grep "ID迁移"
```
