# 艺享集 / 拾艺局 后台管理系统：艺术家评分审核与价格调控模块 V1.0

本包用于 Vue3 后台管理系统开发，配合前面两个包：

1. 后端模块：艺术家评分 + 价格上涨引擎
2. 小程序端：评分展示 + 价格成长展示
3. 本包：后台审核 + 调控 + 日志管理

## 推荐放置位置

```text
admin-web/
├── src/
│   ├── api/
│   │   ├── artistScore.js
│   │   ├── artistIdentity.js
│   │   └── artworkPrice.js
│   ├── router/
│   │   └── modules/artist-score.js
│   ├── views/
│   │   └── artist-score/
│   │       ├── ArtistScoreList.vue
│   │       ├── ArtistIdentityAudit.vue
│   │       ├── ArtistScoreDetail.vue
│   │       ├── ArtworkPriceControl.vue
│   │       └── ArtworkPriceLog.vue
│   └── components/
│       └── ScoreLevelTag.vue
```

## 核心功能

- 艺术家评分列表
- 艺术家资质审核
- 学术资质审核
- 互联网博主资质审核
- 人工调分
- 艺术家等级查看
- 作品价格调控
- 价格上涨/回调日志
- 审核通过后重新计算评分

## 开发原则

后台拥有最终审核权。  
前台展示的资质标签必须来自后台审核通过的数据。  
价格调控必须保留日志，不能直接静默改价。
