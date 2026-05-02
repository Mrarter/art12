# 艺享集/拾艺局 - 艺术家评分与价格上涨后端模块 V1.0

本包用于交给 Codex / CodeBuddy 继续开发。

## 模块目标

实现艺术品交易平台中的核心交易引擎：

1. 艺术家评分系统
2. 学术资质评分
3. 互联网博主资质评分
4. 艺术家等级计算
5. 作品每日自动涨价
6. 成交触发涨价
7. 收藏触发涨价
8. 价格变动日志

## 推荐技术栈

- Spring Boot 3.x
- Java 17+
- MyBatis / MyBatis-Plus
- MySQL 8.x
- Maven

## 放置位置

如已有后端项目，将本包的 `src/main/java/com/yixiangji` 目录合并到现有后端项目。

SQL 文件放入：

```text
sql/artist_score_price_engine.sql
```

开发说明放入：

```text
docs/ARTIST_SCORE_BACKEND_DEV_GUIDE.md
```

## 核心接口

```http
POST /api/artist/score/recalculate/{artistId}
GET  /api/artist/score/{artistId}
POST /api/artwork/price/daily/{artworkId}
POST /api/artwork/price/sale/{artworkId}
POST /api/artwork/price/collect/{artworkId}
```

## Codex 开发任务

请基于本包继续补齐：

1. Mapper XML 或 MyBatis-Plus 实现
2. 统一返回结构 ApiResponse
3. 异常处理 GlobalExceptionHandler
4. 定时任务：每日自动执行作品调价
5. 后台管理接口：人工审核身份资质、人工调价
6. 单元测试

