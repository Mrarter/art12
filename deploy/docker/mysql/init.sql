-- 拾艺局数据库初始化脚本
-- 用于 Docker 环境首次启动

CREATE DATABASE IF NOT EXISTS shiyiju DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shiyiju;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `openid` VARCHAR(128) UNIQUE COMMENT '微信OpenID',
  `unionid` VARCHAR(128) COMMENT '微信UnionID',
  `nickname` VARCHAR(64) COMMENT '昵称',
  `avatar` VARCHAR(512) COMMENT '头像URL',
  `phone` VARCHAR(32) COMMENT '手机号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员表
CREATE TABLE IF NOT EXISTS `admins` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(64) UNIQUE NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` VARCHAR(64) COMMENT '真实姓名',
  `role` VARCHAR(32) DEFAULT 'admin' COMMENT '角色',
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员 (密码: admin123)
INSERT INTO `admins` (`username`, `password`, `real_name`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'super_admin')
ON DUPLICATE KEY UPDATE username=username;

-- 用户身份表
CREATE TABLE IF NOT EXISTS `user_identities` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `identity_type` VARCHAR(32) NOT NULL COMMENT '身份类型: artist/agent/collector/promoter',
  `status` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核 1-通过 2-拒绝',
  `certification_id` BIGINT COMMENT '认证ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 艺术门类表
CREATE TABLE IF NOT EXISTS `categories` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL COMMENT '名称',
  `icon` VARCHAR(256) COMMENT '图标',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认分类
INSERT INTO `categories` (`name`, `sort`) VALUES
('书法', 1), ('绘画', 2), ('雕塑', 3), ('摄影', 4), ('当代艺术', 5), ('其他', 6)
ON DUPLICATE KEY UPDATE name=name;

-- 作品表
CREATE TABLE IF NOT EXISTS `artworks` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '艺术家ID',
  `title` VARCHAR(128) NOT NULL COMMENT '作品标题',
  `description` TEXT COMMENT '作品描述',
  `category_id` BIGINT COMMENT '分类ID',
  `price` DECIMAL(10,2) COMMENT '价格',
  `stock` INT DEFAULT 1 COMMENT '库存',
  `images` TEXT COMMENT '图片JSON',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 购物车表
CREATE TABLE IF NOT EXISTS `carts` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `artwork_id` BIGINT NOT NULL,
  `quantity` INT DEFAULT 1,
  `locked` TINYINT DEFAULT 0 COMMENT '是否锁定',
  `locked_by` BIGINT COMMENT '锁定人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '买家ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '优惠金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `pay_status` TINYINT DEFAULT 0 COMMENT '支付状态: 0-未支付 1-已支付 2-已退款',
  `pay_time` DATETIME COMMENT '支付时间',
  `order_status` TINYINT DEFAULT 0 COMMENT '订单状态: 0-待付款 1-待发货 2-待收货 3-已完成 4-已取消',
  `receiver_name` VARCHAR(64) COMMENT '收货人',
  `receiver_phone` VARCHAR(32) COMMENT '联系电话',
  `receiver_address` VARCHAR(512) COMMENT '收货地址',
  `remark` VARCHAR(256) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_items` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `artwork_id` BIGINT NOT NULL,
  `artwork_title` VARCHAR(128) COMMENT '作品标题快照',
  `artwork_image` VARCHAR(512) COMMENT '作品图片快照',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `quantity` INT DEFAULT 1,
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收货地址表
CREATE TABLE IF NOT EXISTS `addresses` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `consignee` VARCHAR(64) NOT NULL COMMENT '收货人',
  `phone` VARCHAR(32) NOT NULL,
  `province` VARCHAR(32) COMMENT '省',
  `city` VARCHAR(32) COMMENT '市',
  `district` VARCHAR(32) COMMENT '区',
  `address` VARCHAR(256) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 艺荐官表
CREATE TABLE IF NOT EXISTS `promoter_records` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL UNIQUE,
  `inviter_id` BIGINT COMMENT '邀请人ID',
  `invite_code` VARCHAR(32) UNIQUE COMMENT '邀请码',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-失效 1-有效',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 佣金记录表
CREATE TABLE IF NOT EXISTS `commission_logs` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '艺荐官ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL,
  `buyer_id` BIGINT NOT NULL COMMENT '买家ID',
  `order_amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
  `commission_rate` DECIMAL(5,4) NOT NULL COMMENT '佣金比例',
  `commission_amount` DECIMAL(10,2) NOT NULL COMMENT '佣金金额',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待确认 1-已确认 2-已提现',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 拍卖专场表
CREATE TABLE IF NOT EXISTS `auction_sessions` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT '专场名称',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-预展 1-进行中 2-已结束',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 拍卖拍品表
CREATE TABLE IF NOT EXISTS `auction_lots` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL COMMENT '专场ID',
  `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
  `start_price` DECIMAL(10,2) NOT NULL COMMENT '起拍价',
  `current_price` DECIMAL(10,2) COMMENT '当前价',
  `reserve_price` DECIMAL(10,2) COMMENT '保留价',
  `bid_count` INT DEFAULT 0 COMMENT '出价次数',
  `winner_id` BIGINT COMMENT '竞得者ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-预展 1-竞拍中 2-已成交 3-流拍',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 拍卖出价记录
CREATE TABLE IF NOT EXISTS `auction_bids` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `lot_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `bid_price` DECIMAL(10,2) NOT NULL COMMENT '出价',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 艺术圈帖子表
CREATE TABLE IF NOT EXISTS `posts` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL COMMENT '内容',
  `images` TEXT COMMENT '图片JSON',
  `like_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `topic_id` BIGINT COMMENT '话题ID',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-隐藏 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 帖子评论表
CREATE TABLE IF NOT EXISTS `post_comments` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` VARCHAR(512) NOT NULL,
  `reply_id` BIGINT COMMENT '回复ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 帖子点赞表
CREATE TABLE IF NOT EXISTS `post_likes` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 话题表
CREATE TABLE IF NOT EXISTS `topics` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL UNIQUE,
  `description` VARCHAR(256) COMMENT '描述',
  `post_count` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS `messages` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT '消息类型',
  `title` VARCHAR(128) COMMENT '标题',
  `content` TEXT,
  `related_id` BIGINT COMMENT '关联ID',
  `is_read` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 艺术家认证表
CREATE TABLE IF NOT EXISTS `artist_certifications` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `real_name` VARCHAR(64) NOT NULL,
  `id_card` VARCHAR(32) COMMENT '身份证号',
  `id_card_front` VARCHAR(256) COMMENT '身份证正面',
  `id_card_back` VARCHAR(256) COMMENT '身份证背面',
  `art_field` VARCHAR(64) COMMENT '艺术领域',
  `portfolio` TEXT COMMENT '作品集URL',
  `status` TINYINT DEFAULT 0 COMMENT '审核状态',
  `reject_reason` VARCHAR(256) COMMENT '拒绝原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 艺术家认证表
CREATE TABLE IF NOT EXISTS `artist_certifications` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `real_name` VARCHAR(64) NOT NULL,
  `id_card` VARCHAR(32) COMMENT '身份证号',
  `id_card_front` VARCHAR(256) COMMENT '身份证正面',
  `id_card_back` VARCHAR(256) COMMENT '身份证背面',
  `art_field` VARCHAR(64) COMMENT '艺术领域',
  `portfolio` TEXT COMMENT '作品集URL',
  `status` TINYINT DEFAULT 0 COMMENT '审核状态',
  `reject_reason` VARCHAR(256) COMMENT '拒绝原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文件上传记录表
CREATE TABLE IF NOT EXISTS `file_records` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT COMMENT '上传用户',
  `file_name` VARCHAR(256) NOT NULL COMMENT '文件名',
  `file_path` VARCHAR(512) NOT NULL COMMENT '存储路径',
  `file_url` VARCHAR(512) COMMENT '访问URL',
  `file_size` BIGINT COMMENT '文件大小',
  `file_type` VARCHAR(32) COMMENT '文件类型',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;