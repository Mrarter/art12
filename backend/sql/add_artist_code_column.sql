-- 艺术家认证表添加 artist_code 字段
-- 日期: 2026-04-25
-- 说明: 为艺术家认证表添加标准化艺术家ID字段

-- 添加 artist_code 字段
ALTER TABLE `artist_certifications` 
ADD COLUMN `artist_code` VARCHAR(19) DEFAULT NULL COMMENT '艺术家认证编号(新), 如: ART202604200001K9M3' 
AFTER `id`;

-- 为已有认证的艺术家生成编号（示例）
-- 需要根据实际业务逻辑生成编号
-- UPDATE artist_certifications SET artist_code = CONCAT('ART', DATE_FORMAT(create_time, '%Y%m%d'), LPAD(id, 4, '0'), UPPER(SUBSTRING(MD5(RAND()), 1, 4))) WHERE artist_code IS NULL AND status = 1;
