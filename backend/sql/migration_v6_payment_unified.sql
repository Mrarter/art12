-- ============================================
-- v6: unified payment/refund foundation
-- ============================================

CREATE TABLE IF NOT EXISTS `payment_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `pay_no` VARCHAR(40) NOT NULL COMMENT '平台支付单号',
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型: ORDER/AUCTION_DEPOSIT/RESALE/RECHARGE',
    `biz_id` BIGINT NOT NULL COMMENT '业务主键',
    `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
    `user_id` BIGINT NOT NULL COMMENT '付款用户ID',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '支付金额，沿用业务订单金额单位',
    `channel` VARCHAR(20) NOT NULL COMMENT 'WECHAT/ALIPAY',
    `trade_type` VARCHAR(32) NOT NULL COMMENT '渠道交易类型',
    `status` VARCHAR(20) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/PAYING/SUCCESS/CLOSED/REFUNDING/REFUNDED',
    `channel_trade_no` VARCHAR(80) DEFAULT NULL COMMENT '渠道交易号',
    `expire_time` DATETIME DEFAULT NULL,
    `pay_time` DATETIME DEFAULT NULL,
    `request_payload` TEXT DEFAULT NULL,
    `response_payload` TEXT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_channel_trade_no` (`channel_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一支付单';

CREATE TABLE IF NOT EXISTS `payment_notify_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `channel` VARCHAR(20) NOT NULL COMMENT 'WECHAT/ALIPAY',
    `pay_no` VARCHAR(40) DEFAULT NULL,
    `biz_no` VARCHAR(64) DEFAULT NULL,
    `channel_trade_no` VARCHAR(80) DEFAULT NULL,
    `notify_type` VARCHAR(20) NOT NULL DEFAULT 'PAY',
    `raw_payload` MEDIUMTEXT DEFAULT NULL,
    `verified` TINYINT NOT NULL DEFAULT 0,
    `process_status` VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    `fail_reason` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_pay_no` (`pay_no`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_channel_time` (`channel`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道通知日志';

CREATE TABLE IF NOT EXISTS `refund_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `refund_no` VARCHAR(40) NOT NULL COMMENT '平台退款单号',
    `pay_no` VARCHAR(40) NOT NULL COMMENT '平台支付单号',
    `biz_type` VARCHAR(32) NOT NULL,
    `biz_id` BIGINT NOT NULL,
    `biz_no` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `total_amount` DECIMAL(12,2) NOT NULL,
    `refund_amount` DECIMAL(12,2) NOT NULL,
    `channel` VARCHAR(20) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'INIT',
    `channel_refund_no` VARCHAR(80) DEFAULT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `refund_time` DATETIME DEFAULT NULL,
    `request_payload` TEXT DEFAULT NULL,
    `response_payload` TEXT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_pay_no` (`pay_no`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一退款单';

CREATE TABLE IF NOT EXISTS `refund_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) DEFAULT NULL,
    `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
    `refund_amount` DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    `refund_type` TINYINT DEFAULT 1 COMMENT '1-仅退款 2-退货退款',
    `reason` VARCHAR(500) NOT NULL COMMENT '退款原因',
    `images` TEXT DEFAULT NULL COMMENT '凭证图片，逗号分隔',
    `status` TINYINT DEFAULT 0 COMMENT '0-待处理 1-同意 2-拒绝',
    `handle_remark` VARCHAR(255) DEFAULT NULL,
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `handle_time` DATETIME DEFAULT NULL,
    `complete_time` DATETIME DEFAULT NULL,
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

CREATE TABLE IF NOT EXISTS `user_oauth_account` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '平台用户ID',
    `provider` VARCHAR(20) NOT NULL COMMENT 'wechat/alipay',
    `app_type` VARCHAR(20) NOT NULL COMMENT 'mini/official/app/h5',
    `openid` VARCHAR(128) NOT NULL COMMENT '第三方 OpenID',
    `unionid` VARCHAR(128) DEFAULT NULL COMMENT '微信 UnionID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常 0-禁用',
    `bind_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_provider_app_openid` (`provider`, `app_type`, `openid`),
    KEY `idx_user_provider` (`user_id`, `provider`),
    KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方身份绑定表';
