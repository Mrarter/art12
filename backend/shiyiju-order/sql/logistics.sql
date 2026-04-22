-- 物流信息表
CREATE TABLE IF NOT EXISTS `logistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `company_code` VARCHAR(50) COMMENT '物流公司编码',
    `company_name` VARCHAR(100) COMMENT '物流公司名称',
    `tracking_no` VARCHAR(100) COMMENT '运单号',
    `ship_time` DATETIME COMMENT '发货时间',
    `receive_time` DATETIME COMMENT '收货时间',
    `status` TINYINT DEFAULT 0 COMMENT '物流状态（0-暂无物流信息 1-已发货 2-运输中 3-派送中 4-已签收 5-拒收 6-退件）',
    `receiver_name` VARCHAR(100) COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) COMMENT '收货人电话',
    `receiver_address` VARCHAR(500) COMMENT '收货地址',
    `sender_name` VARCHAR(100) COMMENT '发货人姓名',
    `sender_phone` VARCHAR(20) COMMENT '发货人电话',
    `sender_address` VARCHAR(500) COMMENT '发货地址',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_tracking_no` (`tracking_no`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流信息表';

-- 物流轨迹表
CREATE TABLE IF NOT EXISTS `logistics_track` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `logistics_id` BIGINT NOT NULL COMMENT '物流ID',
    `tracking_no` VARCHAR(100) COMMENT '运单号',
    `track_time` DATETIME COMMENT '物流时间',
    `status` VARCHAR(50) COMMENT '物流状态',
    `description` VARCHAR(500) COMMENT '物流描述',
    `location` VARCHAR(200) COMMENT '当前地点',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_logistics_id` (`logistics_id`),
    INDEX `idx_tracking_no` (`tracking_no`),
    INDEX `idx_track_time` (`track_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流轨迹表';

-- 物流公司配置表
CREATE TABLE IF NOT EXISTS `logistics_company` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(50) NOT NULL COMMENT '物流公司编码',
    `name` VARCHAR(100) NOT NULL COMMENT '物流公司名称',
    `logo` VARCHAR(500) COMMENT 'Logo URL',
    `website` VARCHAR(200) COMMENT '官网',
    `phone` VARCHAR(50) COMMENT '客服电话',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用 1-正常）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流公司配置表';

-- 插入默认物流公司
INSERT INTO `logistics_company` (`code`, `name`, `sort`, `status`) VALUES
('SF', '顺丰速运', 1, 1),
('YTO', '圆通速递', 2, 1),
('ZTO', '中通快递', 3, 1),
('STO', '申通快递', 4, 1),
('YD', '韵达快递', 5, 1),
('JTSD', '极兔速递', 6, 1),
('EMS', 'EMS', 7, 1),
('YZPY', '邮政快递包裹', 8, 1);
