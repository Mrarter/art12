# Codex 开发提示词：艺术家评分与价格上涨后端模块

你现在是本项目的后端开发工程师。

## 项目背景

这是一个纯艺术品交易与分销小程序，项目名暂定为“艺享集/拾艺局”。平台只售卖纯艺术品，不卖装饰画。艺术家可以发布作品，藏家可以购买或收藏，作品可随着艺术家评分、成交、收藏而涨价。

## 本次开发目标

请基于当前目录中的代码，完成一个可运行的 Spring Boot 后端模块：

1. 艺术家评分系统
2. 学术资质评分
3. 互联网博主资质评分
4. 艺术家等级计算
5. 作品每日价格上涨
6. 成交触发涨价
7. 收藏触发涨价
8. 价格日志记录

## 已提供内容

- Java Entity
- Controller
- Service
- Mapper Interface
- SQL 建表文件
- 开发说明文档

## 请继续完成

1. 建立 Spring Boot 项目基础配置。
2. 使用 MyBatis 或 MyBatis-Plus 完成 Mapper 实现。
3. 创建 mapper XML 文件。
4. 增加统一返回结构 ApiResponse。
5. 增加 GlobalExceptionHandler。
6. 增加定时任务 DailyArtworkPriceJob，每天凌晨执行在售作品涨价。
7. 增加后台接口：审核艺术家资质、人工调价、查看价格日志。
8. 增加测试数据 SQL。
9. 保证项目可以启动、接口可以访问。

## 编码要求

- Java 17+
- Spring Boot 3.x
- MySQL 8.x
- 所有金额使用 BigDecimal
- 所有接口必须保留清晰注释
- 所有价格变化必须写入 artwork_price_log
- 不允许直接删除价格日志
- 评分计算要保留可配置扩展空间

