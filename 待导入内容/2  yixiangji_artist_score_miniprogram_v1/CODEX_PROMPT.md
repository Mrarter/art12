# Codex 开发提示词：小程序端评分与价格上涨模块

你现在要开发一个微信小程序模块，项目名称为「艺享集 / 拾艺局」，这是一个纯艺术品交易与分销平台。

## 本次任务

开发小程序端的艺术家评分与作品价格上涨展示模块。

## 技术要求

- 微信小程序原生开发
- 使用 WXML / WXSS / JS / JSON
- API 请求统一放在 services
- request 封装放在 utils/request.js
- 页面结构清晰，可直接接入真实后端
- 不要在前端写死评分算法
- 所有评分、等级、价格变化以后端接口返回为准

## 需要完成

1. 艺术家主页评分展示
2. 艺术家等级徽章组件
3. 艺术家资质标签组件
4. 作品详情页价格成长卡片
5. 评分详情页
6. API 对接文件
7. 收藏触发价格上涨接口调用
8. 价格日志展示

## 后端接口

- GET /api/artist/score/{artistId}
- POST /api/artist/score/recalculate/{artistId}
- POST /api/artwork/price/daily/{artworkId}
- POST /api/artwork/price/sale/{artworkId}
- POST /api/artwork/price/collect/{artworkId}

## 注意

这是交易平台，不是普通画廊展示系统。  
页面必须强调：

- 艺术家成长
- 作品价格上涨
- 收藏热度
- 身份认证
- 成交信任感
