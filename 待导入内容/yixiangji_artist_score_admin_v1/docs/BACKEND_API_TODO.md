# 后台管理端需要补充的后端接口

前面第 1 包已经有基础接口。后台管理系统还需要补充以下 Admin 接口。

## 1. 艺术家评分列表

```http
GET /api/admin/artist/scores
```

参数：

```json
{
  "keyword": "艺术家名称或ID",
  "level": "A"
}
```

返回：

```json
{
  "records": [
    {
      "artistId": 1,
      "artistName": "张三",
      "totalScore": 780,
      "level": "A",
      "academicScore": 100,
      "internetScore": 50,
      "updatedAt": "2026-04-30 10:00:00"
    }
  ]
}
```

---

## 2. 人工调分

```http
POST /api/admin/artist/score/manual-adjust
```

请求：

```json
{
  "artistId": 1,
  "adjustScore": 20,
  "reason": "艺术家获得重要展览，运营临时加权"
}
```

要求：

- 必须记录操作人
- 必须记录原因
- 必须写入调分日志
- 调整后重新计算 total_score

---

## 3. 资质审核列表

```http
GET /api/admin/artist/identity/audit-list
```

---

## 4. 资质审核

```http
POST /api/admin/artist/identity/audit
```

请求：

```json
{
  "artistId": 1,
  "auditStatus": "PASS",
  "auditRemark": "资料真实，通过审核"
}
```

要求：

- 审核通过后 verified = 1
- 审核驳回后 verified = 0
- 审核通过后触发评分重算

---

## 5. 作品价格列表

```http
GET /api/admin/artwork/price/list
```

---

## 6. 人工调价

```http
POST /api/admin/artwork/price/manual-adjust
```

请求：

```json
{
  "artworkId": 1,
  "oldPrice": 12000,
  "newPrice": 12800,
  "reason": "平台活动后恢复价格"
}
```

要求：

- 更新 artwork.current_price
- 写入 artwork_price_log
- change_reason = MANUAL
- 必须保存操作人和原因

---

## 7. 价格日志

```http
GET /api/admin/artwork/price/logs
```
