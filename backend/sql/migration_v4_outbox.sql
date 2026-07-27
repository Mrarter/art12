-- ============================================
-- Phase 4: 金融级可靠事件系统
-- Outbox + DLQ 表
-- ============================================

-- Outbox: 可靠事件存储
CREATE TABLE IF NOT EXISTS `finance_event_outbox` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型(对应FinanceEventType枚举)',
    `event_body` TEXT NOT NULL COMMENT '事件体(JSON序列化的FinanceEvent)',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    `retry_count` INT DEFAULT 0 COMMENT '已重试次数',
    `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '最近一次错误信息',
    `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_time` DATETIME DEFAULT NULL COMMENT '完成/失败时间',
    KEY `idx_status` (`status`),
    KEY `idx_next_retry` (`next_retry_time`),
    KEY `idx_created` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金融事件Outbox表（可靠事件存储）';

-- 死信队列：永久失败的事件
CREATE TABLE IF NOT EXISTS `finance_event_dlq` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `outbox_id` BIGINT DEFAULT NULL COMMENT '原Outbox ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型',
    `event_body` TEXT NOT NULL COMMENT '事件体',
    `retry_count` INT DEFAULT NULL COMMENT '已重试次数',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `failed_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '进入DLQ时间',
    `is_replayed` TINYINT DEFAULT 0 COMMENT '是否已重放: 0-未重放, 1-已重放',
    `replayed_time` DATETIME DEFAULT NULL COMMENT '重放时间',
    KEY `idx_replayed` (`is_replayed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金融事件死信队列（待人工处理）';
