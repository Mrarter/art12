-- ============================================
-- 分类管理权重排序升级脚本
-- 执行时间：2026-04-20
-- ============================================

-- 1. 将 sort 字段改为 weight（如果字段已存在）
-- 注意：如果表已存在且有数据，请先备份

-- 如果是新库，直接执行 init_database.sql 即可

-- 如果是已存在的库，执行以下语句进行字段重命名：
ALTER TABLE `artwork_category` 
CHANGE COLUMN `sort` `weight` INT DEFAULT 0 COMMENT '权重，数值越大排序越靠前';

-- 更新索引
ALTER TABLE `artwork_category` DROP INDEX `idx_sort`;
ALTER TABLE `artwork_category` ADD INDEX `idx_weight` (`weight`);

-- 2. 更新现有分类的权重值（可选，保持原有排序逻辑）
-- 将原有 sort 值乘以一定系数，使热门分类权重更高
UPDATE `artwork_category` SET `weight` = `weight` * 10;

-- 3. 验证更新
SELECT id, name, weight FROM `artwork_category` ORDER BY weight DESC;
