-- ============================================
-- 用户ID统一迁移脚本 (统一使用19位格式)
-- 版本: 2.0
-- 日期: 2026-04-26
-- 说明: 将所有表中的 user_id (BIGINT) 迁移为 user_uid (VARCHAR 19位)
-- 格式: [种类前缀(3位)][日期YYYYMMDD(8位)][4位序列号][4位随机码]
-- ============================================

-- 0. 设置事务隔离级别
SET SESSION transaction_isolation = 'READ-COMMITTED';

-- ============================================
-- 1. 创建用户ID映射表（用于新旧ID对照）
-- ============================================
CREATE TABLE IF NOT EXISTS `user_id_mapping` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '映射ID',
    `old_user_id` BIGINT NOT NULL COMMENT '旧用户ID（数字）',
    `uid` VARCHAR(19) NOT NULL COMMENT '新用户UID（19位标准化格式）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_old_user_id` (`old_user_id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户ID映射表';

-- ============================================
-- 2. 为已有用户生成19位UID并填充映射表
-- ============================================

-- 如果 sys_user 表存在 uid 字段，为没有 uid 的用户生成
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `generate_missing_user_uids`()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_old_id BIGINT;
    DECLARE v_new_id VARCHAR(19);
    DECLARE v_date_str VARCHAR(8);
    DECLARE v_seq INT DEFAULT 1;
    DECLARE v_random VARCHAR(4);
    
    DECLARE cur CURSOR FOR 
        SELECT id FROM sys_user WHERE uid IS NULL OR uid = '' OR uid = 'null' ORDER BY id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SET v_date_str = DATE_FORMAT(CURDATE(), '%Y%m%d');
    
    -- 获取当前最大序列号
    SELECT COALESCE(MAX(CAST(SUBSTRING(uid, 12, 4) AS UNSIGNED)), 0) INTO v_seq
    FROM user_id_mapping 
    WHERE SUBSTRING(uid, 4, 8) = v_date_str;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_old_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET v_seq = v_seq + 1;
        SET v_random = UPPER(SUBSTRING(MD5(CONCAT(v_old_id, RAND(), NOW())), 1, 4));
        SET v_new_id = CONCAT('USR', v_date_str, LPAD(v_seq, 4, '0'), v_random);
        
        -- 更新用户表的 uid 字段
        UPDATE sys_user SET uid = v_new_id WHERE id = v_old_id;
        
        -- 插入映射表
        INSERT INTO user_id_mapping (old_user_id, uid) VALUES (v_old_id, v_new_id)
        ON DUPLICATE KEY UPDATE uid = v_new_id;
    END LOOP;
    
    CLOSE cur;
    
    SELECT CONCAT('Generated UIDs for users created on ', v_date_str) AS result;
END //
DELIMITER ;

-- 执行存储过程
CALL generate_missing_user_uids();

-- 如果 users 表（业务主表）需要处理
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `generate_missing_users_uids`()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_old_id BIGINT;
    DECLARE v_new_id VARCHAR(19);
    DECLARE v_date_str VARCHAR(8);
    DECLARE v_seq INT DEFAULT 1;
    DECLARE v_random VARCHAR(4);
    
    -- 检查 users 表是否存在 uid 列
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'uid') THEN
    
        DECLARE cur CURSOR FOR 
            SELECT id FROM users WHERE uid IS NULL OR uid = '' OR uid = 'null' ORDER BY id;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        
        SET v_date_str = DATE_FORMAT(CURDATE(), '%Y%m%d');
        
        SELECT COALESCE(MAX(CAST(SUBSTRING(uid, 12, 4) AS UNSIGNED)), 0) INTO v_seq
        FROM user_id_mapping 
        WHERE SUBSTRING(uid, 4, 8) = v_date_str;
        
        OPEN cur;
        
        read_loop: LOOP
            FETCH cur INTO v_old_id;
            IF done THEN
                LEAVE read_loop;
            END IF;
            
            SET v_seq = v_seq + 1;
            SET v_random = UPPER(SUBSTRING(MD5(CONCAT(v_old_id, RAND(), NOW())), 1, 4));
            SET v_new_id = CONCAT('USR', v_date_str, LPAD(v_seq, 4, '0'), v_random);
            
            UPDATE users SET uid = v_new_id WHERE id = v_old_id;
            
            INSERT INTO user_id_mapping (old_user_id, uid) VALUES (v_old_id, v_new_id)
            ON DUPLICATE KEY UPDATE uid = v_new_id;
        END LOOP;
        
        CLOSE cur;
    END IF;
    
    SELECT 'Users table UID generation completed' AS result;
END //
DELIMITER ;

CALL generate_missing_users_uids();

-- ============================================
-- 3. 为各表添加 user_uid 字段
-- ============================================

-- 艺术家认证表
ALTER TABLE `artist_certifications` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- 艺荐官记录表
ALTER TABLE `promoter_record` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- 作品表
ALTER TABLE `artwork` 
ADD COLUMN `author_uid` VARCHAR(19) DEFAULT NULL COMMENT '作者用户UID(新)' AFTER `author_id`,
ADD COLUMN `holder_uid` VARCHAR(19) DEFAULT NULL COMMENT '持有者用户UID(新)' AFTER `holder_id`;

-- 订单表
ALTER TABLE `order_info` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`,
ADD COLUMN `promoter_uid` VARCHAR(19) DEFAULT NULL COMMENT '艺荐官UID(新)' AFTER `promoter_id`;

-- 购物车表
ALTER TABLE `cart` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`,
ADD COLUMN `promoter_uid` VARCHAR(19) DEFAULT NULL COMMENT '艺荐官UID(新)' AFTER `promoter_id`;

-- 作品收藏表
ALTER TABLE `artwork_favorite` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- 社区帖子表
ALTER TABLE `community_post` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '发布用户UID(新)' AFTER `user_id`;

-- 评论表
ALTER TABLE `post_comment` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '评论用户UID(新)' AFTER `user_id`,
ADD COLUMN `reply_to_uid` VARCHAR(19) DEFAULT NULL COMMENT '被回复用户UID(新)' AFTER `reply_to_user_id`;

-- 评价/晒单表
ALTER TABLE `review` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`,
ADD COLUMN `reply_to_uid` VARCHAR(19) DEFAULT NULL COMMENT '被回复用户UID(新)' AFTER `reply_to_user_id`;

-- 收货地址表
ALTER TABLE `user_address` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- 竞拍记录表
ALTER TABLE `auction_bid` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- 提现记录表
ALTER TABLE `withdraw_record` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- 佣金记录表
ALTER TABLE `commission_log` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`,
ADD COLUMN `promoter_uid` VARCHAR(19) DEFAULT NULL COMMENT '艺荐官UID(新)' AFTER `promoter_id`;

-- 退款记录表
ALTER TABLE `refund_record` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- 用户浏览足迹表
ALTER TABLE `user_browse_history` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '用户UID(新)' AFTER `user_id`;

-- Banner关联用户表
ALTER TABLE `banner` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '关联用户UID(新)' AFTER `user_id`;

-- 文件信息表
ALTER TABLE `file_info` 
ADD COLUMN `user_uid` VARCHAR(19) DEFAULT NULL COMMENT '上传用户UID(新)' AFTER `user_id`;

-- ============================================
-- 4. 为艺术家认证表和艺荐官表添加标准化编号字段
-- ============================================

-- 艺术家认证表 - 添加 artist_code
ALTER TABLE `artist_certifications` 
ADD COLUMN `artist_code` VARCHAR(19) DEFAULT NULL COMMENT '艺术家编号(新), 如: ART20260420001K9M3' AFTER `update_time`;

-- 艺荐官记录表 - 添加 promoter_code
ALTER TABLE `promoter_record` 
ADD COLUMN `promoter_code` VARCHAR(19) DEFAULT NULL COMMENT '艺荐官编号(新), 如: PRM20260420001K9M3' AFTER `update_time`;

-- ============================================
-- 5. 填充各表的 user_uid 字段
-- ============================================

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `populate_user_uids`()
BEGIN
    -- 艺术家认证表
    UPDATE artist_certifications a
    INNER JOIN user_id_mapping m ON a.user_id = m.old_user_id
    SET a.user_uid = m.uid
    WHERE a.user_uid IS NULL;
    
    -- 艺荐官记录表
    UPDATE promoter_record p
    INNER JOIN user_id_mapping m ON p.user_id = m.old_user_id
    SET p.user_uid = m.uid
    WHERE p.user_uid IS NULL;
    
    -- 作品表 - 作者
    UPDATE artwork a
    INNER JOIN user_id_mapping m ON a.author_id = m.old_user_id
    SET a.author_uid = m.uid
    WHERE a.author_uid IS NULL AND a.author_id IS NOT NULL;
    
    -- 作品表 - 持有者
    UPDATE artwork a
    INNER JOIN user_id_mapping m ON a.holder_id = m.old_user_id
    SET a.holder_uid = m.uid
    WHERE a.holder_uid IS NULL AND a.holder_id IS NOT NULL;
    
    -- 订单表
    UPDATE order_info o
    INNER JOIN user_id_mapping m ON o.user_id = m.old_user_id
    SET o.user_uid = m.uid
    WHERE o.user_uid IS NULL;
    
    -- 购物车表
    UPDATE cart c
    INNER JOIN user_id_mapping m ON c.user_id = m.old_user_id
    SET c.user_uid = m.uid
    WHERE c.user_uid IS NULL;
    
    -- 作品收藏表
    UPDATE artwork_favorite f
    INNER JOIN user_id_mapping m ON f.user_id = m.old_user_id
    SET f.user_uid = m.uid
    WHERE f.user_uid IS NULL;
    
    -- 社区帖子表
    UPDATE community_post p
    INNER JOIN user_id_mapping m ON p.user_id = m.old_user_id
    SET p.user_uid = m.uid
    WHERE p.user_uid IS NULL;
    
    -- 评论表
    UPDATE post_comment c
    INNER JOIN user_id_mapping m ON c.user_id = m.old_user_id
    SET c.user_uid = m.uid
    WHERE c.user_uid IS NULL;
    
    -- 评论表 - 回复
    UPDATE post_comment c
    INNER JOIN user_id_mapping m ON c.reply_to_user_id = m.old_user_id
    SET c.reply_to_uid = m.uid
    WHERE c.reply_to_uid IS NULL AND c.reply_to_user_id IS NOT NULL;
    
    -- 评价表
    UPDATE review r
    INNER JOIN user_id_mapping m ON r.user_id = m.old_user_id
    SET r.user_uid = m.uid
    WHERE r.user_uid IS NULL;
    
    -- 收货地址表
    UPDATE user_address a
    INNER JOIN user_id_mapping m ON a.user_id = m.old_user_id
    SET a.user_uid = m.uid
    WHERE a.user_uid IS NULL;
    
    -- 竞拍记录表
    UPDATE auction_bid b
    INNER JOIN user_id_mapping m ON b.user_id = m.old_user_id
    SET b.user_uid = m.uid
    WHERE b.user_uid IS NULL;
    
    -- 提现记录表
    UPDATE withdraw_record w
    INNER JOIN user_id_mapping m ON w.user_id = m.old_user_id
    SET w.user_uid = m.uid
    WHERE w.user_uid IS NULL;
    
    -- 佣金记录表
    UPDATE commission_log l
    INNER JOIN user_id_mapping m ON l.user_id = m.old_user_id
    SET l.user_uid = m.uid
    WHERE l.user_uid IS NULL;
    
    -- 退款记录表
    UPDATE refund_record r
    INNER JOIN user_id_mapping m ON r.user_id = m.old_user_id
    SET r.user_uid = m.uid
    WHERE r.user_uid IS NULL;
    
    -- 用户浏览足迹表
    UPDATE user_browse_history h
    INNER JOIN user_id_mapping m ON h.user_id = m.old_user_id
    SET h.user_uid = m.uid
    WHERE h.user_uid IS NULL;
    
    -- 文件信息表
    UPDATE file_info f
    INNER JOIN user_id_mapping m ON f.user_id = m.old_user_id
    SET f.user_uid = m.uid
    WHERE f.user_uid IS NULL;
    
    SELECT 'All user_uid fields populated successfully' AS result;
END //
DELIMITER ;

CALL populate_user_uids();

-- ============================================
-- 6. 填充艺术家编号和艺荐官编号
-- ============================================

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `populate_entity_codes`()
BEGIN
    DECLARE v_date_str VARCHAR(8);
    DECLARE v_seq INT DEFAULT 1;
    DECLARE v_random VARCHAR(4);
    DECLARE v_max_seq INT;
    
    SET v_date_str = DATE_FORMAT(CURDATE(), '%Y%m%d');
    
    -- 艺术家编号
    SELECT COALESCE(MAX(CAST(SUBSTRING(artist_code, 12, 4) AS UNSIGNED)), 0) INTO v_max_seq
    FROM artist_certifications 
    WHERE SUBSTRING(artist_code, 4, 8) = v_date_str AND artist_code IS NOT NULL;
    
    UPDATE artist_certifications 
    SET artist_code = CONCAT(
        'ART',
        v_date_str,
        LPAD(ROW_NUMBER() OVER (ORDER BY id) + v_max_seq, 4, '0'),
        UPPER(SUBSTRING(MD5(CONCAT(id, RAND(), NOW())), 1, 4))
    )
    WHERE artist_code IS NULL OR artist_code = '' OR artist_code = 'null';
    
    -- 艺荐官编号
    SELECT COALESCE(MAX(CAST(SUBSTRING(promoter_code, 12, 4) AS UNSIGNED)), 0) INTO v_max_seq
    FROM promoter_record 
    WHERE SUBSTRING(promoter_code, 4, 8) = v_date_str AND promoter_code IS NOT NULL;
    
    UPDATE promoter_record 
    SET promoter_code = CONCAT(
        'PRM',
        v_date_str,
        LPAD(ROW_NUMBER() OVER (ORDER BY id) + v_max_seq, 4, '0'),
        UPPER(SUBSTRING(MD5(CONCAT(id, RAND(), NOW())), 1, 4))
    )
    WHERE promoter_code IS NULL OR promoter_code = '' OR promoter_code = 'null';
    
    SELECT 'Entity codes populated successfully' AS result;
END //
DELIMITER ;

CALL populate_entity_codes();

-- ============================================
-- 7. 创建索引优化查询性能
-- ============================================

-- 用户ID映射表索引（已创建）

-- 各表 user_uid 索引
CREATE INDEX idx_artist_user_uid ON artist_certifications(user_uid);
CREATE INDEX idx_promoter_user_uid ON promoter_record(user_uid);
CREATE INDEX idx_artwork_author_uid ON artwork(author_uid);
CREATE INDEX idx_artwork_holder_uid ON artwork(holder_uid);
CREATE INDEX idx_order_user_uid ON order_info(user_uid);
CREATE INDEX idx_order_promoter_uid ON order_info(promoter_uid);
CREATE INDEX idx_cart_user_uid ON cart(user_uid);
CREATE INDEX idx_cart_promoter_uid ON cart(promoter_uid);
CREATE INDEX idx_favorite_user_uid ON artwork_favorite(user_uid);
CREATE INDEX idx_post_user_uid ON community_post(user_uid);
CREATE INDEX idx_comment_user_uid ON post_comment(user_uid);
CREATE INDEX idx_comment_reply_uid ON post_comment(reply_to_uid);
CREATE INDEX idx_review_user_uid ON review(user_uid);
CREATE INDEX idx_address_user_uid ON user_address(user_uid);
CREATE INDEX idx_bid_user_uid ON auction_bid(user_uid);
CREATE INDEX idx_withdraw_user_uid ON withdraw_record(user_uid);
CREATE INDEX idx_commission_user_uid ON commission_log(user_uid);
CREATE INDEX idx_commission_promoter_uid ON commission_log(promoter_uid);
CREATE INDEX idx_refund_user_uid ON refund_record(user_uid);
CREATE INDEX idx_browse_user_uid ON user_browse_history(user_uid);
CREATE INDEX idx_file_user_uid ON file_info(user_uid);

-- 艺术家编号和艺荐官编号索引
CREATE UNIQUE INDEX uk_artist_code ON artist_certifications(artist_code);
CREATE UNIQUE INDEX uk_promoter_code ON promoter_record(promoter_code);

-- ============================================
-- 8. 清理临时存储过程
-- ============================================
DROP PROCEDURE IF EXISTS generate_missing_user_uids;
DROP PROCEDURE IF EXISTS generate_missing_users_uids;
DROP PROCEDURE IF EXISTS populate_user_uids;
DROP PROCEDURE IF EXISTS populate_entity_codes;

-- ============================================
-- 9. 验证迁移结果
-- ============================================
SELECT '=== 迁移验证 ===' AS info;

-- 检查用户ID映射表
SELECT COUNT(*) AS '映射记录数' FROM user_id_mapping;

-- 检查各表 user_uid 填充情况
SELECT 'artist_certifications' AS tbl, COUNT(*) AS total, 
       SUM(CASE WHEN user_uid IS NOT NULL THEN 1 ELSE 0 END) AS filled
FROM artist_certifications
UNION ALL
SELECT 'promoter_record', COUNT(*), SUM(CASE WHEN user_uid IS NOT NULL THEN 1 ELSE 0 END)
FROM promoter_record
UNION ALL
SELECT 'artwork', COUNT(*), SUM(CASE WHEN author_uid IS NOT NULL THEN 1 ELSE 0 END)
FROM artwork
UNION ALL
SELECT 'order_info', COUNT(*), SUM(CASE WHEN user_uid IS NOT NULL THEN 1 ELSE 0 END)
FROM order_info
UNION ALL
SELECT 'cart', COUNT(*), SUM(CASE WHEN user_uid IS NOT NULL THEN 1 ELSE 0 END)
FROM cart;

-- 示例：查看用户刘亦菲的映射关系
SELECT '示例：查找同名用户' AS info;
SELECT u.id AS old_id, u.uid, u.nickname, u.phone
FROM sys_user u
INNER JOIN user_id_mapping m ON u.id = m.old_user_id
WHERE u.nickname LIKE '%刘亦菲%' OR u.phone = '13800138000'
LIMIT 10;

SELECT '=== 迁移完成 ===' AS info;
