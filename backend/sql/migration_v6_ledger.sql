-- ============================================
-- Phase 6: 金融级资金系统终态重构
-- Ledger System + MQ Log + Risk Log
-- ============================================

-- 双记账账本 — 资金操作的唯一真相
CREATE TABLE IF NOT EXISTS `ledger_account` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `account_type` VARCHAR(20) DEFAULT 'USER' COMMENT '账户类型: USER/PLATFORM/ARTIST',
    `balance` DECIMAL(14,2) DEFAULT 0.00 COMMENT '可用余额',
    `frozen_balance` DECIMAL(14,2) DEFAULT 0.00 COMMENT '冻结余额',
    `version` INT DEFAULT 0 COMMENT '乐观锁',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本账户表（金融级单一事实源）';

-- 交易分录 — 每笔资金变动的不可变记录
CREATE TABLE IF NOT EXISTS `ledger_transaction` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `txn_id` VARCHAR(64) NOT NULL COMMENT '业务交易ID（全局唯一，幂等）',
    `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型: PAY/REFUND/RESALE/COMMISSION/WITHDRAW',
    `direction` VARCHAR(10) NOT NULL COMMENT '方向: DEBIT(借=扣款)/CREDIT(贷=入账)',
    `amount` DECIMAL(14,2) NOT NULL COMMENT '金额',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `related_type` VARCHAR(30) DEFAULT NULL COMMENT '关联业务类型',
    `reversal_of_txn_id` VARCHAR(64) DEFAULT NULL COMMENT '反向交易ID（退款时指向原始PAY）',
    `status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_txn_id` (`txn_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_reversal` (`reversal_of_txn_id`),
    KEY `idx_related` (`related_id`, `related_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本交易分录（append-only，不修改只追加）';

-- MQ 消费日志 — at-least-once 幂等消费保障
CREATE TABLE IF NOT EXISTS `mq_consume_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `msg_id` VARCHAR(64) NOT NULL COMMENT '消息ID（全局唯一）',
    `txn_id` VARCHAR(64) DEFAULT NULL COMMENT '业务交易ID',
    `event_type` VARCHAR(50) DEFAULT NULL COMMENT '事件类型',
    `status` VARCHAR(20) DEFAULT 'PRODUCED' COMMENT '状态: PRODUCED/CONSUMED/FAILED',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `consumed_time` DATETIME DEFAULT NULL,
    UNIQUE KEY `uk_msg_id` (`msg_id`),
    KEY `idx_txn` (`txn_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消费日志（幂等消费保障）';
