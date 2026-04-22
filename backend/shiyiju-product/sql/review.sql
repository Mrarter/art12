-- 作品评价表
CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_item_id` BIGINT NOT NULL COMMENT '订单项ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_nickname` VARCHAR(100) COMMENT '用户昵称',
    `user_avatar` VARCHAR(500) COMMENT '用户头像',
    `rating` TINYINT NOT NULL COMMENT '评分（1-5星）',
    `content` TEXT COMMENT '评价内容',
    `images` VARCHAR(2000) COMMENT '评价图片（多个用逗号分隔）',
    `quality_rating` TINYINT COMMENT '作品状态评分',
    `logistics_rating` TINYINT COMMENT '物流服务评分',
    `service_rating` TINYINT COMMENT '服务态度评分',
    `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名（0-否 1-是）',
    `status` TINYINT DEFAULT 1 COMMENT '审核状态（0-待审核 1-通过 2-拒绝）',
    `audit_remark` VARCHAR(500) COMMENT '审核备注',
    `audit_time` DATETIME COMMENT '审核时间',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_order_item_user` (`order_item_id`, `user_id`),
    INDEX `idx_artwork_id` (`artwork_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品评价表';

-- 评价回复表
CREATE TABLE IF NOT EXISTS `review_reply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `review_id` BIGINT NOT NULL COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '回复用户ID',
    `user_nickname` VARCHAR(100) COMMENT '回复用户昵称',
    `user_avatar` VARCHAR(500) COMMENT '回复用户头像',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `reply_type` VARCHAR(20) NOT NULL COMMENT '回复类型（buyer-买家回复/seller-卖家回复/admin-平台回复）',
    `reply_to_user_id` BIGINT COMMENT '被回复的用户ID',
    `reply_to_nickname` VARCHAR(100) COMMENT '被回复的用户昵称',
    `status` TINYINT DEFAULT 1 COMMENT '审核状态（0-待审核 1-通过 2-拒绝）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_review_id` (`review_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价回复表';

-- 评价点赞表
CREATE TABLE IF NOT EXISTS `review_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `review_id` BIGINT NOT NULL COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_review_user` (`review_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价点赞表';
