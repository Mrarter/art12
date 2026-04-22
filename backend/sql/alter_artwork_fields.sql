-- ============================================
-- 拾艺局·数据库增量脚本 v1.1
-- 日期: 2026-04-22
-- 说明: 添加艺术家简介和电话字段
-- ============================================

USE shiyiju;

-- artwork表添加艺术家简介和电话字段
ALTER TABLE `artwork` 
ADD COLUMN IF NOT EXISTS `author_bio` VARCHAR(500) DEFAULT NULL COMMENT '艺术家简介',
ADD COLUMN IF NOT EXISTS `author_phone` VARCHAR(20) DEFAULT NULL COMMENT '艺术家联系电话';

-- 注意: freight_amount 字段已存在于 order_info 表
