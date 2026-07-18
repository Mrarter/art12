CREATE TABLE IF NOT EXISTS `private_messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sender_id` BIGINT NOT NULL,
    `recipient_id` BIGINT NOT NULL,
    `message_type` VARCHAR(20) NOT NULL DEFAULT 'text',
    `content` TEXT NULL,
    `extra_data` JSON NULL,
    `is_read` TINYINT NOT NULL DEFAULT 0,
    `read_time` DATETIME NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_private_sender_recipient_id` (`sender_id`, `recipient_id`, `id`),
    KEY `idx_private_recipient_read_id` (`recipient_id`, `is_read`, `id`),
    KEY `idx_private_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户私信';
