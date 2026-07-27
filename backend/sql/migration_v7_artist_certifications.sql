-- 修复 artist_certifications 表结构，使其与 ArtistCertification.java 实体类匹配
-- 执行时间: 2026-05-19

-- 1. 添加缺失的列
ALTER TABLE `artist_certifications`
ADD COLUMN IF NOT EXISTS `artist_code` VARCHAR(20) DEFAULT NULL COMMENT '艺术家认证编号 (如: ART202604200001K9M3)' AFTER `id`,
ADD COLUMN IF NOT EXISTS `resume` TEXT COMMENT '个人履历' AFTER `id_card`,
ADD COLUMN IF NOT EXISTS `artworks` TEXT COMMENT '代表作图片URLs，逗号分隔' AFTER `resume`,
ADD COLUMN IF NOT EXISTS `exhibits` TEXT COMMENT '参展证明URLs，逗号分隔' AFTER `artworks`,
ADD COLUMN IF NOT EXISTS `review_time` DATETIME DEFAULT NULL COMMENT '审核时间' AFTER `reject_reason`;

-- 2. 将 cert_status 重命名为 status（如果需要保持一致性）
-- 注意：MySQL 不支持直接重命名列，需要用 change 语句
-- 如果 cert_status 列存在，将其数据迁移到 status 列
-- 先检查 cert_status 列是否存在
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'artist_certifications'
    AND COLUMN_NAME = 'cert_status'
);

-- 如果 cert_status 存在，将其数据迁移到 status 列
SET @sql = IF(@column_exists > 0,
    'ALTER TABLE `artist_certifications` ADD COLUMN IF NOT EXISTS `status` TINYINT DEFAULT 0 COMMENT ''认证状态：0-待审核，1-已通过，2-已拒绝'' AFTER `exhibits`',
    'SELECT ''cert_status column does not exist, no action needed'' as result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加索引
ALTER TABLE `artist_certifications`
ADD INDEX IF NOT EXISTS `idx_artist_code` (`artist_code`),
ADD INDEX IF NOT EXISTS `idx_status` (`status`);

-- 4. 查看修复后的表结构
DESCRIBE `artist_certifications`;
