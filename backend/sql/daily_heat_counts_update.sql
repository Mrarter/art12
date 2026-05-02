-- 为作品增加每日展示热度配置
-- 展示浏览量 = 真实浏览量 + daily_view_count * 上架天数
-- 展示点赞量 = 真实收藏/喜欢量 + daily_like_count * 上架天数
ALTER TABLE artwork
  ADD COLUMN daily_view_count INT DEFAULT 0 COMMENT '每日展示浏览增量' AFTER view_count,
  ADD COLUMN daily_like_count INT DEFAULT 0 COMMENT '每日展示点赞增量' AFTER daily_view_count;
