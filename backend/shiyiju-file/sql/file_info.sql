-- 文件信息表
CREATE TABLE IF NOT EXISTS `file_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `original_name` VARCHAR(255) COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件URL',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) NOT NULL COMMENT '文件类型（image/video/document）',
    `mime_type` VARCHAR(100) COMMENT 'MIME类型',
    `extension` VARCHAR(20) COMMENT '文件扩展名',
    `user_id` BIGINT COMMENT '上传用户ID',
    `biz_type` VARCHAR(50) COMMENT '业务类型（artwork/avatar/post等）',
    `biz_id` BIGINT COMMENT '关联业务ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用 1-正常）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_file_type` (`file_type`),
    INDEX `idx_biz_type_id` (`biz_type`, `biz_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- 文件存储配置表（可选，用于存储不同存储方式的配置）
CREATE TABLE IF NOT EXISTS `file_storage_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `storage_type` VARCHAR(50) NOT NULL COMMENT '存储类型（local/cos/oss）',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) COMMENT '配置值',
    `description` VARCHAR(255) COMMENT '配置描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用 1-正常）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_storage_type_key` (`storage_type`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件存储配置表';
