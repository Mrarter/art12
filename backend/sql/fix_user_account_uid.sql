-- ============================================
-- 修复脚本：为 user_account 表添加 user_uid 字段
-- 日期: 2026-04-27
-- ============================================

-- 检查 user_account 表是否存在，不存在则创建
CREATE TABLE IF NOT EXISTS `user_account` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status` VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态',
    `register_source` VARCHAR(20) DEFAULT NULL COMMENT '注册来源',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID',
    `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账户表';

-- 如果 user_uid 字段不存在，添加它
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'user_account' 
    AND COLUMN_NAME = 'user_uid'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE user_account ADD COLUMN user_uid VARCHAR(19) DEFAULT NULL COMMENT "用户UID(新)" AFTER uid',
    'SELECT "user_uid column already exists"');
    
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 如果 uid 字段不存在，添加它
SET @uid_column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'user_account' 
    AND COLUMN_NAME = 'uid'
);

SET @sql2 = IF(@uid_column_exists = 0, 
    'ALTER TABLE user_account ADD COLUMN uid VARCHAR(19) DEFAULT NULL COMMENT "用户UID"',
    'SELECT "uid column already exists"');
    
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPATE stmt2;

SELECT '修复完成：user_account 表已添加 user_uid 字段' AS result;
