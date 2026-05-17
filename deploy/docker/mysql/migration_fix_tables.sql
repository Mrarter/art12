-- ========================================
-- 拾艺局数据库迁移脚本
-- 修复表名和结构以匹配 Java 实体定义
-- ========================================
-- 使用方法: mysql -u root -p shiyiju_local < migration_fix_tables.sql

-- ========================================
-- 1. 修复 artwork 表缺失的字段
-- ========================================
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `stock` INT DEFAULT 1 COMMENT '库存' AFTER `price`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `original_price` DECIMAL(10,2) COMMENT '原始价格' AFTER `price`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `holder_id` BIGINT COMMENT '当前持有者ID' AFTER `status`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `holder_since` DATETIME COMMENT '持有起始时间' AFTER `holder_id`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `author_id` BIGINT COMMENT '艺术家ID' AFTER `title`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `sale_count` INT DEFAULT 0 COMMENT '销售数量' AFTER `stock`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `price_rise` DECIMAL(10,4) DEFAULT 0 COMMENT '价格涨幅' AFTER `original_price`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `content_fingerprint` VARCHAR(128) COMMENT '内容指纹' AFTER `update_time`;

-- ========================================
-- 2. 创建 trade_order 表（替代旧的 orders 表）
-- ========================================
CREATE TABLE IF NOT EXISTS `trade_order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `buyer_user_id` BIGINT NOT NULL COMMENT '买家用户ID',
  `buyer_user_uid` VARCHAR(64) DEFAULT NULL COMMENT '买家用户UID',
  `seller_name` VARCHAR(64) DEFAULT NULL COMMENT '卖家名称',
  `seller_avatar` VARCHAR(512) DEFAULT NULL COMMENT '卖家头像',
  `order_type` VARCHAR(32) DEFAULT 'DIRECT' COMMENT '订单来源: DIRECT/CART/AUCTION/RESALE',
  `order_status` VARCHAR(32) DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
  `payment_status` VARCHAR(32) DEFAULT 'UNPAID' COMMENT '支付状态',
  `goods_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '商品总额',
  `freight_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '运费',
  `discount_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '折扣金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '应付金额',
  `address_id` BIGINT DEFAULT NULL COMMENT '收货地址ID',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  INDEX `idx_order_no` (`order_no`),
  INDEX `idx_buyer_user_id` (`buyer_user_id`),
  INDEX `idx_order_status` (`order_status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ========================================
-- 3. 创建 trade_order_item 表
-- ========================================
CREATE TABLE IF NOT EXISTS `trade_order_item` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `artwork_id` BIGINT NOT NULL COMMENT '艺术品ID',
  `artist_id` BIGINT DEFAULT NULL COMMENT '艺术家ID',
  `item_type` VARCHAR(32) DEFAULT 'ARTWORK' COMMENT '物品类型',
  `sku_no` VARCHAR(64) DEFAULT NULL COMMENT 'SKU编码',
  `item_title` VARCHAR(128) DEFAULT NULL COMMENT '物品标题',
  `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `quantity` INT DEFAULT 1 COMMENT '数量',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计',
  `promoter_id` BIGINT DEFAULT NULL COMMENT '艺荐官ID',
  `commission_status` INT DEFAULT 0 COMMENT '佣金状态: 0-未结算 1-已结算',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_artwork_id` (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- ========================================
-- 4. 创建 order_fail_record 表
-- ========================================
CREATE TABLE IF NOT EXISTS `order_fail_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '失败时已生成的订单号',
  `user_id` BIGINT NOT NULL COMMENT '买家用户ID',
  `artwork_id` BIGINT DEFAULT NULL COMMENT '作品ID',
  `resale_id` BIGINT DEFAULT NULL COMMENT '转售记录ID',
  `cart_ids` VARCHAR(256) DEFAULT NULL COMMENT '购物车ID列表（逗号分隔）',
  `source` VARCHAR(32) NOT NULL COMMENT '订单来源',
  `fail_reason` VARCHAR(64) NOT NULL COMMENT '失败原因枚举',
  `fail_message` VARCHAR(1024) DEFAULT NULL COMMENT '详细错误信息',
  `request_params` TEXT COMMENT '请求参数JSON',
  `retry_count` INT DEFAULT 0 COMMENT '已重试次数',
  `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',
  `retry_status` TINYINT DEFAULT 0 COMMENT '重试状态: 0-未重试 1-重试中 2-重试成功 3-重试失败',
  `compensated` TINYINT DEFAULT 0 COMMENT '是否已补偿回滚: 0-未补偿 1-已补偿',
  `compensate_at` DATETIME DEFAULT NULL COMMENT '补偿回滚时间',
  `next_retry_at` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_order_no` (`order_no`),
  INDEX `idx_retry_status` (`retry_status`),
  INDEX `idx_next_retry_at` (`next_retry_at`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单失败记录表';
