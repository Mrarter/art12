# 艺享集 / 拾艺局 小程序端：艺术家评分与价格上涨模块 V1.0

本包用于微信小程序前端开发，对接后端模块：

- 艺术家评分系统
- 身份资质展示
- 互联网博主资质展示
- 作品价格上涨展示
- 作品收藏触发涨价
- 成交后涨价展示

## 推荐放置位置

```text
miniprogram/
├── pages/
│   ├── artist-score/
│   ├── artwork-detail/
│   └── artist-profile/
├── components/
│   ├── artist-level-badge/
│   ├── artist-cert-tags/
│   └── price-growth-card/
├── services/
│   ├── artist-score-api.js
│   └── artwork-price-api.js
└── utils/
    └── request.js
```

## 页面说明

### 1. artist-profile
艺术家主页，展示艺术家等级、资质标签、成长指数、代表作品。

### 2. artist-score
艺术家评分详情页，展示评分构成，不建议普通用户看到过细算法，可用于艺术家本人或后台预览。

### 3. artwork-detail
作品详情页，展示当前价格、今日涨幅、收藏人数、下一次涨价条件。

## 开发原则

前端不要直接计算核心评分，核心评分以后端为准。  
小程序前端只负责展示、触发收藏、拉取价格日志。
