# ID迁移API接口文档

> 更新时间：2026-04-25
> 版本：v1.2

## 基础信息

- **User Service Base URL**: `http://localhost:8090/api/user` (用户服务)
- **Admin Service Base URL**: `http://localhost:8090/api/admin` (管理服务)
- **认证**: 需要管理员Token（Header: `Authorization: Bearer {token}`）

---

## 一、用户UID迁移接口

### 1. 批量更新用户UID

```http
POST /api/user/admin/batch-update-uids
```

**请求示例**:
```json
{
  "userIds": [1, 2, 3, 4, 5],
  "uids": [
    "USR202604250001VKO5",
    "USR202604250001XSEU",
    "USR202604250002WWVT",
    "USR202604250003MZ9L",
    "USR202604250004YZ7S"
  ]
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "批量更新成功"
}
```

---

### 2. 更新单个用户UID

```http
POST /api/user/admin/update-uid
```

**请求示例**:
```json
{
  "userId": 1,
  "uid": "USR202604250001VKO5"
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "更新成功"
}
```

---

## 二、ID格式验证接口

### 3. 验证UID格式

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

### 4. 获取迁移状态

```http
GET /api/admin/id-migration/status
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "users": 50,
    "artworks": 0,
    "sessions": 0,
    "lots": 0,
    "posts": 0,
    "comments": 0,
    "topics": 0,
    "withdraws": 0,
    "commissions": 0,
    "bids": 0,
    "aftersales": 0
  }
}
```

---

## 三、Postman测试脚本

```javascript
// Postman Collection - 用户UID迁移

// 批量更新用户UID
pm.test("批量更新UID成功", function() {
    pm.sendRequest({
        url: 'http://localhost:8090/api/user/admin/batch-update-uids',
        method: 'POST',
        header: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + pm.collectionVariables.get("adminToken")
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                userIds: [1, 2, 3],
                uids: [
                    "USR202604250001VKO5",
                    "USR202604250001XSEU",
                    "USR202604250002WWVT"
                ]
            })
        }
    }, function(err, res) {
        var jsonData = res.json();
        pm.expect(jsonData.success).to.eq(true);
    });
});

// 更新单个用户UID
pm.test("更新单个UID成功", function() {
    pm.sendRequest({
        url: 'http://localhost:8090/api/user/admin/update-uid',
        method: 'POST',
        header: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + pm.collectionVariables.get("adminToken")
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                userId: 10,
                uid: "USR202604250010A7KM"
            })
        }
    }, function(err, res) {
        var jsonData = res.json();
        pm.expect(jsonData.success).to.eq(true);
    });
});

// 验证ID格式
pm.test("ID格式验证", function() {
    var testId = "USR202604250001A7KM";
    pm.sendRequest({
        url: 'http://localhost:8090/api/admin/id-migration/validate/' + testId,
        method: 'GET',
        header: {
            'Authorization': 'Bearer ' + pm.collectionVariables.get("adminToken")
        }
    }, function(err, res) {
        var jsonData = res.json();
        pm.expect(jsonData.valid).to.eq(true);
        pm.expect(jsonData.type).to.eq("用户");
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
```

---

## 四、UID格式说明

标准UID格式：`USR + YYYYMMDD + NNNN + XXXX`

| 字段 | 位置 | 说明 | 示例 |
|------|------|------|------|
| 类型前缀 | 1-3 | USR=用户 | USR |
| 日期 | 4-11 | 创建日期 | 20260425 |
| 序号 | 12-15 | 当日序号 | 0001 |
| 随机码 | 16-19 | 4位随机字符 | VKO5 |

**示例**: `USR202604250001VKO5` = 用户 + 2026年4月25日 + 第1个 + 随机码VKO5

---

## 五、日志查看

迁移过程中查看日志：

```bash
# User Service日志
tail -f logs/shiyiju-user.log | grep "batchUpdateUids"

# Admin API日志
tail -f logs/shiyiju-admin.log | grep "ID迁移"
```
