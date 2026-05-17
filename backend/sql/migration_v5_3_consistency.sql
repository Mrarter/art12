-- ============================================
-- Phase 5.3: 资金闭环终极一致性
-- 1. refund_event_log 表
-- 2. wallet_bill 增加 event_id / direction
-- ============================================

-- 退款事件日志表（追踪每笔退款链路）
CREATE TABLE IF NOT EXISTS `refund_event_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `order_type` VARCHAR(20) DEFAULT NULL COMMENT '订单类型: NORMAL/RESALE',
    `refund_type` VARCHAR(50) NOT NULL COMMENT '退款类型: REFUND_ARTIST/RESALE_ROLLBACK',
    `refund_status` VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态: PROCESSING/COMPLETED/FAILED',
    `total_refund_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '退款总金额',
    `artist_reversed` DECIMAL(12,2) DEFAULT NULL COMMENT '已扣回艺术家收益',
    `seller_reversed` DECIMAL(12,2) DEFAULT NULL COMMENT '已扣回卖家收入',
    `platform_reversed` DECIMAL(12,2) DEFAULT NULL COMMENT '已扣回平台费用',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    KEY `idx_order_no` (`order_no`),
    KEY `idx_status` (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款事件日志（资金回滚追踪）';

-- wallet_bill 增加审计字段（ALTER TABLE，兼容已存在的表）
ALTER TABLE `wallet_bill`
    ADD COLUMN IF NOT EXISTS `event_id` VARCHAR(64) DEFAULT NULL COMMENT '事件ID（幂等标识）' AFTER `remark`,
    ADD COLUMN IF NOT EXISTS `direction` VARCHAR(20) DEFAULT 'IN' COMMENT '方向: IN(入账)/OUT(出账)/REVERSAL(反向回滚)' AFTER `event_id`,
    ADD INDEX IF NOT EXISTS `idx_event_id` (`event_id`);
