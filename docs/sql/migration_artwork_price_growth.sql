-- 添加单个作品价格增长配置字段
-- 执行时间: 2026-04-25

ALTER TABLE artwork ADD COLUMN custom_price_growth_enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用自定义价格增长';
ALTER TABLE artwork ADD COLUMN custom_base_daily_rate DECIMAL(10,6) DEFAULT 0.0002 COMMENT '自定义基础日增长率';
ALTER TABLE artwork ADD COLUMN custom_mature_daily_rate DECIMAL(10,6) DEFAULT 0.0003 COMMENT '自定义成熟期日增长率';
ALTER TABLE artwork ADD COLUMN custom_mature_days INT DEFAULT 30 COMMENT '自定义成熟期天数';
ALTER TABLE artwork ADD COLUMN custom_view_rate DECIMAL(5,2) DEFAULT 1.10 COMMENT '自定义浏览量加成系数';
ALTER TABLE artwork ADD COLUMN custom_favorite_rate DECIMAL(5,2) DEFAULT 1.10 COMMENT '自定义收藏量加成系数';
ALTER TABLE artwork ADD COLUMN custom_max_growth_multiple DECIMAL(5,2) DEFAULT 5.00 COMMENT '自定义最大涨幅倍数';
