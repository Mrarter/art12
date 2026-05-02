# 小程序 API 对接说明

## 1. 获取艺术家评分

```http
GET /api/artist/score/{artistId}
```

### 返回示例

```json
{
  "artistId": 1,
  "totalScore": 780,
  "salesScore": 180,
  "influenceScore": 120,
  "activityScore": 70,
  "qualityScore": 130,
  "reviewScore": 80,
  "academicScore": 100,
  "internetScore": 50,
  "level": "A"
}
```

---

## 2. 重新计算艺术家评分

```http
POST /api/artist/score/recalculate/{artistId}
```

使用场景：

- 后台审核资质后
- 艺术家新增作品后
- 成交后
- 平台定时任务

---

## 3. 收藏触发涨价

```http
POST /api/artwork/price/collect/{artworkId}
```

使用场景：

- 用户点击收藏
- 每新增收藏触发价格引擎
- 实际生产中建议后端判断是否达到涨价条件，不要每次收藏都涨价

---

## 4. 成交触发涨价

```http
POST /api/artwork/price/sale/{artworkId}
```

使用场景：

- 作品成交后
- 订单支付成功后
- 二次流通价格调整

---

## 5. 每日自动涨价

```http
POST /api/artwork/price/daily/{artworkId}
```

使用场景：

- 后台定时任务触发
- 不建议由小程序前端直接调用
