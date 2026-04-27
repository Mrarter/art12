-- ============================================
-- ID迁移脚本
-- 版本: 1.0
-- 日期: 2026-04-25
-- 说明: 为各表添加新ID字段，采用19位标准化ID格式
-- 格式: [种类前缀(3位)][日期YYYYMMDD(8位)][4位序列号][4位随机码]
-- ============================================

-- 1. 创建ID映射表（用于新旧ID对照）
CREATE TABLE IF NOT EXISTS `id_mapping` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '映射ID',
    `entity_type` VARCHAR(20) NOT NULL COMMENT '实体类型: user/session/lot/post/comment/topic/withdraw/commission/bid/aftersale/artwork',
    `old_id` BIGINT NOT NULL COMMENT '旧ID（数字）',
    `new_id` VARCHAR(19) NOT NULL COMMENT '新ID（字符串）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_entity_old_id` (`entity_type`, `old_id`),
    KEY `idx_new_id` (`new_id`),
    KEY `idx_old_id` (`old_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID映射表';

-- 2. 为用户表添加新ID字段
ALTER TABLE `sys_user` ADD COLUMN `uid` VARCHAR(19) DEFAULT NULL COMMENT '用户ID(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_uid` ON `sys_user`(`uid`);

-- 3. 为拍卖专场表添加新ID字段
ALTER TABLE `auction_session` ADD COLUMN `session_code` VARCHAR(19) DEFAULT NULL COMMENT '专场编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_session_code` ON `auction_session`(`session_code`);

-- 4. 为拍卖拍品表添加新ID字段
ALTER TABLE `auction_lot` ADD COLUMN `lot_code` VARCHAR(19) DEFAULT NULL COMMENT '拍品编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_lot_code` ON `auction_lot`(`lot_code`);

-- 5. 为社区帖子表添加新ID字段
ALTER TABLE `community_post` ADD COLUMN `post_code` VARCHAR(19) DEFAULT NULL COMMENT '帖子编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_post_code` ON `community_post`(`post_code`);

-- 6. 为评论表添加新ID字段
ALTER TABLE `post_comment` ADD COLUMN `comment_code` VARCHAR(19) DEFAULT NULL COMMENT '评论编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_comment_code` ON `post_comment`(`comment_code`);

-- 7. 为话题表添加新ID字段
ALTER TABLE `topic` ADD COLUMN `topic_code` VARCHAR(19) DEFAULT NULL COMMENT '话题编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_topic_code` ON `topic`(`topic_code`);

-- 8. 为提现记录表添加新ID字段
ALTER TABLE `withdraw_record` ADD COLUMN `withdraw_code` VARCHAR(19) DEFAULT NULL COMMENT '提现编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_withdraw_code` ON `withdraw_record`(`withdraw_code`);

-- 9. 为佣金记录表添加新ID字段
ALTER TABLE `commission_log` ADD COLUMN `commission_code` VARCHAR(19) DEFAULT NULL COMMENT '佣金编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_commission_code` ON `commission_log`(`commission_code`);

-- 10. 为竞拍记录表添加新ID字段
ALTER TABLE `auction_bid` ADD COLUMN `bid_code` VARCHAR(19) DEFAULT NULL COMMENT '竞拍编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_bid_code` ON `auction_bid`(`bid_code`);

-- 11. 为售后管理表添加新ID字段
ALTER TABLE `refund_record` ADD COLUMN `refund_code` VARCHAR(19) DEFAULT NULL COMMENT '售后编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_refund_code` ON `refund_record`(`refund_code`);

-- 12. 为作品表添加新ID字段
ALTER TABLE `artwork` ADD COLUMN `artwork_code` VARCHAR(19) DEFAULT NULL COMMENT '作品编号(新)' AFTER `id`;
CREATE UNIQUE INDEX `uk_artwork_code` ON `artwork`(`artwork_code`);

-- ============================================
-- 数据填充脚本（示例：需要根据实际数据执行）
-- ============================================

-- 存储过程：生成用户新ID
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `generate_user_ids`()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_old_id BIGINT;
    DECLARE v_new_id VARCHAR(19);
    DECLARE v_date_str VARCHAR(8);
    DECLARE v_seq INT DEFAULT 1;
    
    DECLARE cur CURSOR FOR SELECT id FROM sys_user WHERE uid IS NULL ORDER BY id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SET v_date_str = DATE_FORMAT(CURDATE(), '%Y%m%d');
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_old_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET v_new_id = CONCAT('USR', v_date_str, LPAD(v_seq, 4, '0'), UPPER(SUBSTRING(MD5(RAND()), 1, 4)));
        SET v_seq = v_seq + 1;
        
        UPDATE sys_user SET uid = v_new_id WHERE id = v_old_id;
        INSERT INTO id_mapping (entity_type, old_id, new_id) VALUES ('user', v_old_id, v_new_id);
    END LOOP;
    
    CLOSE cur;
END //
DELIMITER ;

-- 执行存储过程（按需执行）
-- CALL generate_user_ids();

-- ============================================
-- 字段重命名脚本（确认迁移完成后执行）
-- ============================================

-- 以下脚本在确认数据迁移完成后执行
-- 执行前请务必备份数据库！

/*
-- 1. 重命名sys_user表的id字段
ALTER TABLE `sys_user` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `sys_user` CHANGE COLUMN `uid` `id` VARCHAR(19);
ALTER TABLE `sys_user` DROP INDEX `uk_openid`;
ALTER TABLE `sys_user` ADD UNIQUE INDEX `uk_openid` (`openid`);

-- 2. 重命名auction_session表的id字段
ALTER TABLE `auction_session` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `auction_session` CHANGE COLUMN `session_code` `id` VARCHAR(19);

-- 3. 重命名auction_lot表的id字段
ALTER TABLE `auction_lot` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `auction_lot` CHANGE COLUMN `lot_code` `id` VARCHAR(19);

-- 4. 重命名community_post表的id字段
ALTER TABLE `community_post` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `community_post` CHANGE COLUMN `post_code` `id` VARCHAR(19);

-- 5. 重命名post_comment表的id字段
ALTER TABLE `post_comment` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `post_comment` CHANGE COLUMN `comment_code` `id` VARCHAR(19);

-- 6. 重命名topic表的id字段
ALTER TABLE `topic` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `topic` CHANGE COLUMN `topic_code` `id` VARCHAR(19);

-- 7. 重命名withdraw_record表的id字段
ALTER TABLE `withdraw_record` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `withdraw_record` CHANGE COLUMN `withdraw_code` `id` VARCHAR(19);

-- 8. 重命名commission_log表的id字段
ALTER TABLE `commission_log` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `commission_log` CHANGE COLUMN `commission_code` `id` VARCHAR(19);

-- 9. 重命名auction_bid表的id字段
ALTER TABLE `auction_bid` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `auction_bid` CHANGE COLUMN `bid_code` `id` VARCHAR(19);

-- 10. 重命名refund_record表的id字段
ALTER TABLE `refund_record` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `refund_record` CHANGE COLUMN `refund_code` `id` VARCHAR(19);

-- 11. 重命名artwork表的id字段
ALTER TABLE `artwork` CHANGE COLUMN `id` `old_id` BIGINT;
ALTER TABLE `artwork` CHANGE COLUMN `artwork_code` `id` VARCHAR(19);
*/
