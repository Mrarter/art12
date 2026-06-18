CREATE TABLE IF NOT EXISTS `user_cart` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    UNIQUE KEY `uk_user_cart_artwork_active` (`user_id`, `artwork_id`, `deleted`),
    KEY `idx_user_cart_user_id` (`user_id`),
    KEY `idx_user_cart_artwork_id` (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户购物车';
