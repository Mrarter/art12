-- ============================================
-- Phase 3: 艺术品二级流通生态系统
-- 表：转售记录、交易链路、价格历史
-- ============================================

-- 转售记录表
CREATE TABLE IF NOT EXISTS `resale_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `seller_user_id` BIGINT NOT NULL COMMENT '卖家用户ID',
    `buyer_user_id` BIGINT DEFAULT NULL COMMENT '买家用户ID',
    `source_order_id` BIGINT DEFAULT NULL COMMENT '来源订单ID',
    `resale_price` DECIMAL(12,2) NOT NULL COMMENT '转售价格',
    `artist_income` DECIMAL(12,2) DEFAULT 0.00 COMMENT '艺术家持续收益',
    `platform_fee` DECIMAL(12,2) DEFAULT 0.00 COMMENT '平台服务费',
    `seller_income` DECIMAL(12,2) DEFAULT 0.00 COMMENT '卖家实际收入',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待售/paid-已支付/completed-已完成/cancel-已取消',
    `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '交易编号（用于幂等控制）',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_artwork` (`artwork_id`),
    KEY `idx_seller` (`seller_user_id`),
    KEY `idx_buyer` (`buyer_user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转售记录表（含乐观锁）';

-- 作品交易链路记录表
CREATE TABLE IF NOT EXISTS `artwork_trade_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `trade_no` VARCHAR(64) NOT NULL COMMENT '交易编号',
    `seller_user_id` BIGINT DEFAULT NULL COMMENT '卖家用户ID',
    `buyer_user_id` BIGINT NOT NULL COMMENT '买家用户ID',
    `trade_price` DECIMAL(12,2) NOT NULL COMMENT '成交价格',
    `trade_type` VARCHAR(20) NOT NULL COMMENT '交易类型: first_sale-首次出售/resale-转售',
    `trade_round` INT DEFAULT 1 COMMENT '交易轮次（从1递增）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_artwork` (`artwork_id`),
    KEY `idx_trade_no` (`trade_no`),
    KEY `idx_buyer` (`buyer_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品交易链路记录表';

-- 作品价格历史记录表
CREATE TABLE IF NOT EXISTS `artwork_price_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `before_price` DECIMAL(12,2) DEFAULT NULL COMMENT '变动前价格',
    `after_price` DECIMAL(12,2) NOT NULL COMMENT '变动后价格（成交价）',
    `growth_rate` DECIMAL(8,2) DEFAULT 0.00 COMMENT '涨幅(%)',
    `reason` VARCHAR(100) DEFAULT NULL COMMENT '变动原因: first_sale/resale/admin_adjust',
    `related_resale_id` BIGINT DEFAULT NULL COMMENT '关联转售记录ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_artwork` (`artwork_id`),
    KEY `idx_resale` (`related_resale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品价格历史记录表';

-- 已存在表的 ALTER 迁移（如果表已创建）
ALTER TABLE `resale_record`
    ADD COLUMN IF NOT EXISTS `trade_no` VARCHAR(64) DEFAULT NULL AFTER `status`,
    ADD COLUMN IF NOT EXISTS `version` INT DEFAULT 0 AFTER `trade_no`,
    ADD INDEX IF NOT EXISTS `idx_trade_no` (`trade_no`);
