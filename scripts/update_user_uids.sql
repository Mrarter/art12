-- 更新所有现有用户的UID
-- 格式: ART + 日期(YYYYMMDD) + 6位序列号 + 2位随机码

-- 使用游标更新每个用户（MySQL需要使用存储过程）
DELIMITER //

DROP PROCEDURE IF EXISTS update_user_uids //

CREATE PROCEDURE update_user_uids()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE user_id BIGINT;
    DECLARE seq_num INT DEFAULT 0;
    DECLARE user_date VARCHAR(8);
    DECLARE new_uid VARCHAR(18);
    DECLARE rand_char VARCHAR(2);
    
    DECLARE cur CURSOR FOR SELECT id FROM users WHERE uid IS NULL OR uid = '' OR uid = 'N/A';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SET user_date = DATE_FORMAT(CURDATE(), '%Y%m%d');
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO user_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET seq_num = seq_num + 1;
        
        -- 生成随机码
        SET rand_char = CONCAT(
            SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1),
            SUBSTRING('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', FLOOR(1 + RAND() * 36), 1)
        );
        
        -- 生成UID: ART + 日期 + 6位序列号 + 2位随机码
        SET new_uid = CONCAT('ART', user_date, LPAD(seq_num, 6, '0'), rand_char);
        
        UPDATE users SET uid = new_uid WHERE id = user_id;
        
    END LOOP;
    
    CLOSE cur;
    
    SELECT CONCAT('已更新 ', seq_num, ' 个用户UID') AS result;
END //

DELIMITER ;

-- 执行存储过程
CALL update_user_uids();

-- 删除存储过程
DROP PROCEDURE IF EXISTS update_user_uids;
