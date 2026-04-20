-- ============================================
-- 作品价格增长机制数据库变更
-- ============================================

-- 添加价格涨幅字段
ALTER TABLE artwork ADD COLUMN price_rise DECIMAL(10,4) DEFAULT 0 COMMENT '价格涨幅百分比，如 0.05 表示涨幅5%' AFTER original_price;

-- 添加艺术家认证标识字段
ALTER TABLE artwork ADD COLUMN author_badge VARCHAR(50) DEFAULT NULL COMMENT '艺术家认证标识：大师级/人气艺术家/认证艺术家' AFTER author_name;

-- 添加艺术家头像字段
ALTER TABLE artwork ADD COLUMN author_avatar VARCHAR(512) DEFAULT NULL COMMENT '艺术家头像' AFTER author_badge;

-- 添加作品类型字段（原创/收藏）
ALTER TABLE artwork ADD COLUMN ownership_type INT DEFAULT 1 COMMENT '作品类型: 1-原创, 2-收藏' AFTER status;

-- 添加作品编号字段
ALTER TABLE artwork ADD COLUMN artwork_code VARCHAR(20) DEFAULT NULL COMMENT '作品编号，如 yh202604200001' AFTER ownership_type;

-- 添加浏览量字段（如果不存在）
ALTER TABLE artwork ADD COLUMN view_count INT DEFAULT 0 COMMENT '浏览量' AFTER favorite_count;

-- ============================================
-- 索引优化（可选）
-- ============================================
-- CREATE INDEX idx_artwork_price_rise ON artwork(price_rise);
-- CREATE INDEX idx_artwork_view_count ON artwork(view_count);
-- CREATE INDEX idx_artwork_favorite_count ON artwork(favorite_count);
