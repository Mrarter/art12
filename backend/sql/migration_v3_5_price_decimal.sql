-- ============================================
-- Phase 3.5: 资金安全修复
-- 1. Artwork.price BIGINT(分) → DECIMAL(12,2)(元)
-- 2. trade_order_item.price/subtotal BIGINT(分) → DECIMAL(12,2)(元)
-- 3. artwork.original_price 同上
-- ============================================

-- 注意：执行前请备份数据库
-- 这些迁移将修改现有数据类型并转换数据

-- ========== artwork 表 ==========
-- 新增 DECIMAL 列
ALTER TABLE `artwork`
    ADD COLUMN `price_decimal` DECIMAL(12,2) DEFAULT NULL COMMENT '当前价格（元）' AFTER `holder_since`,
    ADD COLUMN `original_price_decimal` DECIMAL(12,2) DEFAULT NULL COMMENT '原始价格（元）' AFTER `price_decimal`;

-- 迁移数据：BIGINT(分) → DECIMAL(12,2)(元)，除以100
UPDATE `artwork` SET `price_decimal` = `price` / 100.00 WHERE `price` IS NOT NULL;
UPDATE `artwork` SET `original_price_decimal` = `original_price` / 100.00 WHERE `original_price` IS NOT NULL;

-- 验证数据完整性
-- SELECT COUNT(*) FROM artwork WHERE price IS NOT NULL AND price_decimal IS NULL;

-- 删除旧列并重命名新列
ALTER TABLE `artwork`
    DROP COLUMN `price`,
    DROP COLUMN `original_price`,
    CHANGE COLUMN `price_decimal` `price` DECIMAL(12,2) DEFAULT NULL COMMENT '当前价格（元）',
    CHANGE COLUMN `original_price_decimal` `original_price` DECIMAL(12,2) DEFAULT NULL COMMENT '原始价格（元）';

-- ========== trade_order_item 表 ==========
ALTER TABLE `trade_order_item`
    ADD COLUMN `price_decimal` DECIMAL(12,2) DEFAULT NULL COMMENT '单价（元）' AFTER `cover_image`,
    ADD COLUMN `subtotal_decimal` DECIMAL(12,2) DEFAULT NULL COMMENT '小计（元）' AFTER `quantity`;

-- 迁移数据
UPDATE `trade_order_item` SET `price_decimal` = `price` / 100.00 WHERE `price` IS NOT NULL;
UPDATE `trade_order_item` SET `subtotal_decimal` = `subtotal` / 100.00 WHERE `subtotal` IS NOT NULL;

ALTER TABLE `trade_order_item`
    DROP COLUMN `price`,
    DROP COLUMN `subtotal`,
    CHANGE COLUMN `price_decimal` `price` DECIMAL(12,2) DEFAULT NULL COMMENT '单价（元）',
    CHANGE COLUMN `subtotal_decimal` `subtotal` DECIMAL(12,2) DEFAULT NULL COMMENT '小计（元）';
