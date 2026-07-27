-- 测试环境补全表
-- 为 Java 实体类 @TableName 创建缺失的表

USE shiyiju_test;

-- 1. 用户关注关系表
CREATE TABLE IF NOT EXISTS `user_follows` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `follow_user_id` BIGINT NOT NULL COMMENT '被关注用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_follow` (`user_id`, `follow_user_id`),
    KEY `idx_follow_user_id` (`follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- 2. 作品收藏表（复数命名，匹配 @TableName("artwork_favorites")）
CREATE TABLE IF NOT EXISTS `artwork_favorites` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_favorite` (`user_id`, `artwork_id`),
    KEY `idx_artwork_id` (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品收藏表';

-- 3. 订单主表（匹配 @TableName("trade_order")）
CREATE TABLE IF NOT EXISTS `trade_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `buyer_id` BIGINT COMMENT '买家ID',
    `seller_id` BIGINT COMMENT '卖家ID',
    `artwork_id` BIGINT COMMENT '作品ID',
    `total_amount` BIGINT DEFAULT 0 COMMENT '总金额(分)',
    `pay_amount` BIGINT DEFAULT 0 COMMENT '实付金额(分)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
    `order_type` VARCHAR(32) DEFAULT 'PURCHASE' COMMENT '订单类型',
    `remark` VARCHAR(500) COMMENT '备注',
    `paid_at` DATETIME COMMENT '支付时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_artwork_id` (`artwork_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 4. 订单明细表（匹配 @TableName("trade_order_item")）
CREATE TABLE IF NOT EXISTS `trade_order_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `title` VARCHAR(255) COMMENT '作品标题',
    `price` BIGINT DEFAULT 0 COMMENT '单价(分)',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `subtotal` BIGINT DEFAULT 0 COMMENT '小计(分)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_order_id` (`order_id`),
    KEY `idx_artwork_id` (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 5. 购物车表（匹配 @TableName("user_cart")）
CREATE TABLE IF NOT EXISTS `user_cart` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_user_artwork` (`user_id`, `artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 6. 作品表补充字段（author_uid, author_name 等）
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `author_uid` VARCHAR(64) COMMENT '作者UID' AFTER `author_id`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `stock` INT DEFAULT 1 COMMENT '库存' AFTER `price`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `status` TINYINT DEFAULT 1 COMMENT '状态 0下架 1上架 2售罄' AFTER `stock`;
ALTER TABLE `artworks` ADD COLUMN IF NOT EXISTS `current_price` BIGINT DEFAULT 0 COMMENT '当前价格(分)' AFTER `price`;

-- 7. artists 表补充字段
ALTER TABLE `artists` ADD COLUMN IF NOT EXISTS `author_uid` VARCHAR(64) COMMENT '作者UID' AFTER `author_id`;
ALTER TABLE `artists` ADD COLUMN IF NOT EXISTS `stock` INT DEFAULT 1 COMMENT '库存' AFTER `price`;
