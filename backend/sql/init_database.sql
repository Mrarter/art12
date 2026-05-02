-- ============================================
-- 拾艺局·高端艺术品交易平台 数据库设计
-- 版本: 1.0
-- 日期: 2026-04-18
-- 严格按开发文档第12版设计
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS shiyiju DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shiyiju;

-- ============================================
-- 1. 用户相关表
-- ============================================

-- 用户表
CREATE TABLE `sys_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信OpenID',
    `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `identity` VARCHAR(50) DEFAULT 'collector' COMMENT '当前身份: artist/agent/collector/promoter',
    `identity_json` VARCHAR(500) DEFAULT '["collector"]' COMMENT '身份JSON数组',
    `invite_code` VARCHAR(20) DEFAULT NULL COMMENT '个人邀请码',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上级用户ID',
    `experience_years` INT DEFAULT 0 COMMENT '从业年限(艺术家)',
    `artist_level` VARCHAR(20) DEFAULT NULL COMMENT '艺术家等级',
    `promoter_level` VARCHAR(20) DEFAULT NULL COMMENT '艺荐官等级',
    `total_commission` DECIMAL(12,2) DEFAULT 0 COMMENT '累计佣金',
    `available_commission` DECIMAL(12,2) DEFAULT 0 COMMENT '可提现佣金',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_time` DATETIME DEFAULT NULL,
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_phone` (`phone`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 艺术家认证表
CREATE TABLE `artist_certifications` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
    `id_card_front` VARCHAR(512) DEFAULT NULL COMMENT '身份证正面',
    `id_card_back` VARCHAR(512) DEFAULT NULL COMMENT '身份证反面',
    `bio` TEXT COMMENT '个人简介',
    `portfolio` JSON DEFAULT NULL COMMENT '代表作图片JSON数组',
    `exhibition_proof` JSON DEFAULT NULL COMMENT '参展证明JSON数组',
    `cert_status` TINYINT DEFAULT 0 COMMENT '认证状态: 0-待审核, 1-通过, 2-拒绝',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    `cert_time` DATETIME DEFAULT NULL COMMENT '认证时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_cert_status` (`cert_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='艺术家认证表';

-- 经纪人认证表
CREATE TABLE `agent_certification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `company_name` VARCHAR(100) NOT NULL COMMENT '经纪机构名称',
    `business_license` VARCHAR(512) DEFAULT NULL COMMENT '营业执照',
    `auth_letter` VARCHAR(512) DEFAULT NULL COMMENT '授权委托书',
    `agent_artists` JSON DEFAULT NULL COMMENT '代理艺术家ID数组',
    `cert_status` TINYINT DEFAULT 0 COMMENT '认证状态: 0-待审核, 1-通过, 2-拒绝',
    `reject_reason` VARCHAR(255) DEFAULT NULL,
    `cert_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经纪人认证表';

-- 收货地址表
CREATE TABLE `user_address` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `province` VARCHAR(50) NOT NULL COMMENT '省份',
    `city` VARCHAR(50) NOT NULL COMMENT '城市',
    `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
    `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认: 0-否, 1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 用户足迹表
CREATE TABLE `user_browse_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_artwork_id` (`artwork_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览足迹表';

-- ============================================
-- 2. 作品/商品相关表
-- ============================================

-- 作品分类表
CREATE TABLE `artwork_category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `weight` INT DEFAULT 0 COMMENT '权重，数值越大排序越靠前',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父级ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_weight` (`weight`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品分类表';

-- 作品表
CREATE TABLE `artwork` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '作品名称',
    `author_id` BIGINT NOT NULL COMMENT '作者用户ID',
    `author_name` VARCHAR(100) DEFAULT NULL COMMENT '作者名称',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `category_name` VARCHAR(50) DEFAULT NULL COMMENT '分类名称',
    `cover_image` VARCHAR(512) NOT NULL COMMENT '封面图',
    `images` JSON DEFAULT NULL COMMENT '图片列表JSON',
    `video_url` VARCHAR(512) DEFAULT NULL COMMENT '视频URL',
    `description` TEXT COMMENT '作品描述/故事',
    `art_type` VARCHAR(50) DEFAULT NULL COMMENT '画种(油画/水墨/版画等)',
    `size` VARCHAR(100) DEFAULT NULL COMMENT '尺寸(如 50*70cm)',
    `material` VARCHAR(100) DEFAULT NULL COMMENT '材质',
    `create_year` INT DEFAULT NULL COMMENT '创作年代/年份',
    `edition` VARCHAR(50) DEFAULT NULL COMMENT '版数(如 1/10)',
    `style` VARCHAR(100) DEFAULT NULL COMMENT '风格',
    `hold_duration` INT DEFAULT 0 COMMENT '持有时长(天)',
    `hold_user_id` BIGINT DEFAULT NULL COMMENT '持有者ID(如果是转售)',
    `price` DECIMAL(12,2) NOT NULL COMMENT '售价(分)',
    `original_price` DECIMAL(12,2) DEFAULT NULL COMMENT '原价',
    `stock` INT DEFAULT 1 COMMENT '库存',
    `sales_count` INT DEFAULT 0 COMMENT '销量',
    `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
    `view_count` INT DEFAULT 0 COMMENT '浏览数',
    `daily_view_count` INT DEFAULT 0 COMMENT '每日展示浏览增量',
    `daily_like_count` INT DEFAULT 0 COMMENT '每日展示点赞增量',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核, 1-上架, 2-下架, 3-售罄, 4-审核拒绝',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '审核拒绝原因',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `distributable` TINYINT DEFAULT 1 COMMENT '是否可分销: 0-否, 1-是',
    `commission_rate` DECIMAL(5,4) DEFAULT 0.0500 COMMENT '佣金比例(默认5%)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签,逗号分隔',
    `region` VARCHAR(100) DEFAULT NULL COMMENT '艺术家地区',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_author_id` (`author_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_price` (`price`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品表';

-- 作品收藏表
CREATE TABLE `artwork_favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_artwork` (`user_id`, `artwork_id`),
    KEY `idx_artwork_id` (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品收藏表';

-- Banner表
CREATE TABLE `banner` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `image_url` VARCHAR(512) NOT NULL COMMENT '图片URL',
    `link_type` VARCHAR(20) DEFAULT NULL COMMENT '跳转类型: gallery/auction/article/artist',
    `link_value` VARCHAR(255) DEFAULT NULL COMMENT '跳转值(专题ID/艺术家ID等)',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `start_time` DATETIME DEFAULT NULL COMMENT '展示开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '展示结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_status` (`status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Banner表';

-- ============================================
-- 3. 订单相关表
-- ============================================

-- 购物车表
CREATE TABLE `cart` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `locked` TINYINT DEFAULT 0 COMMENT '是否锁定: 0-未锁, 1-已锁(结算中)',
    `lock_expire_time` DATETIME DEFAULT NULL COMMENT '锁定过期时间',
    `promoter_id` BIGINT DEFAULT NULL COMMENT '锁客艺荐官ID(用于追踪佣金归属)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_artwork` (`user_id`, `artwork_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_locked` (`locked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 订单主表
CREATE TABLE `order_info` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '商品总价',
    `discount_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '优惠金额',
    `pay_amount` DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    `commission_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '佣金金额',
    `address_id` BIGINT DEFAULT NULL COMMENT '地址ID',
    `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `receiver_address` VARCHAR(255) DEFAULT NULL COMMENT '收货地址',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `source` TINYINT DEFAULT 1 COMMENT '来源: 1-直接购买, 2-购物车, 3-拍卖',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-已取消, 1-待付款, 2-已付款, 3-已发货, 4-已收货, 5-已完成, 6-退款中, 7-已退款',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `express_company` VARCHAR(50) DEFAULT NULL COMMENT '快递公司',
    `express_no` VARCHAR(50) DEFAULT NULL COMMENT '快递单号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单明细表
CREATE TABLE `order_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '作品名称',
    `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图',
    `author_name` VARCHAR(100) DEFAULT NULL COMMENT '作者名称',
    `art_type` VARCHAR(50) DEFAULT NULL COMMENT '画种',
    `size` VARCHAR(100) DEFAULT NULL COMMENT '尺寸',
    `price` DECIMAL(12,2) NOT NULL COMMENT '单价',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `subtotal` DECIMAL(12,2) NOT NULL COMMENT '小计',
    `promoter_id` BIGINT DEFAULT NULL COMMENT '归属艺荐官ID',
    `commission_status` TINYINT DEFAULT 0 COMMENT '佣金状态: 0-未结算, 1-已结算',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_order_id` (`order_id`),
    KEY `idx_artwork_id` (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 退款表
CREATE TABLE `refund_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) DEFAULT NULL,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `refund_amount` DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    `refund_type` TINYINT DEFAULT 1 COMMENT '退款方式: 1-原路退回, 2-退回余额',
    `reason` VARCHAR(500) NOT NULL COMMENT '退款原因',
    `images` JSON DEFAULT NULL COMMENT '凭证图片',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-同意, 2-拒绝',
    `handle_remark` VARCHAR(255) DEFAULT NULL COMMENT '处理备注',
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- ============================================
-- 4. 拍卖相关表
-- ============================================

-- 拍卖专场表
CREATE TABLE `auction_session` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '专场名称',
    `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图',
    `description` TEXT COMMENT '专场描述',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-预告, 1-进行中, 2-已结束',
    `total_lots` INT DEFAULT 0 COMMENT '总拍品数',
    `total_bids` INT DEFAULT 0 COMMENT '总出价次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖专场表';

-- 拍卖拍品表
CREATE TABLE `auction_lot` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL COMMENT '专场ID',
    `artwork_id` BIGINT DEFAULT NULL COMMENT '关联作品ID',
    `lot_no` VARCHAR(20) NOT NULL COMMENT '拍品号',
    `title` VARCHAR(200) NOT NULL COMMENT '拍品名称',
    `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图',
    `artist_name` VARCHAR(100) DEFAULT NULL COMMENT '艺术家',
    `start_price` DECIMAL(12,2) NOT NULL COMMENT '起拍价',
    `reserve_price` DECIMAL(12,2) DEFAULT NULL COMMENT '保留价',
    `current_price` DECIMAL(12,2) DEFAULT NULL COMMENT '当前价',
    `estimate_price` VARCHAR(100) DEFAULT NULL COMMENT '估价',
    `increment` DECIMAL(12,2) DEFAULT 100.00 COMMENT '加价幅度',
    `deposit_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '保证金金额',
    `bid_count` INT DEFAULT 0 COMMENT '出价次数',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '当前买家ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-未开始, 1-竞拍中, 2-已成交, 3-流拍, 4-撤拍',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_session_id` (`session_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖拍品表';

-- 拍卖出价记录表
CREATE TABLE `auction_bid` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `lot_id` BIGINT NOT NULL COMMENT '拍品ID',
    `user_id` BIGINT NOT NULL COMMENT '出价用户ID',
    `bid_price` DECIMAL(12,2) NOT NULL COMMENT '出价金额',
    `bid_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-出局, 1-领先',
    KEY `idx_lot_id` (`lot_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_bid_time` (`bid_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖出价记录表';

-- 拍卖保证金表
CREATE TABLE `auction_deposit` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `lot_id` BIGINT NOT NULL COMMENT '拍品ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '保证金金额',
    `pay_status` TINYINT DEFAULT 0 COMMENT '支付状态: 0-未支付, 1-已支付, 2-已退款',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '交易流水号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_lot_id` (`lot_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖保证金表';

-- ============================================
-- 5. 分销/艺荐官相关表
-- ============================================

-- 艺荐官记录表
CREATE TABLE `promoter_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `invite_code` VARCHAR(20) NOT NULL COMMENT '邀请码',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上级艺荐官ID',
    `level` TINYINT DEFAULT 1 COMMENT '等级: 1-普通, 2-白银, 3-黄金, 4-钻石',
    `team_size` INT DEFAULT 0 COMMENT '团队人数',
    `total_orders` INT DEFAULT 0 COMMENT '累计订单数',
    `total_sales` DECIMAL(12,2) DEFAULT 0 COMMENT '累计销售额',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `sign_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '签约时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `uk_invite_code` (`invite_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='艺荐官记录表';

-- 佣金记录表
CREATE TABLE `commission_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '艺荐官ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
    `order_no` VARCHAR(32) DEFAULT NULL COMMENT '订单号',
    `artwork_id` BIGINT DEFAULT NULL COMMENT '作品ID',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '购买用户ID',
    `order_amount` DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    `commission_rate` DECIMAL(5,4) NOT NULL COMMENT '佣金比例',
    `commission_amount` DECIMAL(12,2) NOT NULL COMMENT '佣金金额',
    `commission_type` TINYINT NOT NULL COMMENT '类型: 1-直接推广, 2-团队奖励',
    `level` TINYINT DEFAULT 1 COMMENT '等级: 1-一级, 2-二级',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待结算, 1-已结算, 2-已失效',
    `settle_time` DATETIME DEFAULT NULL COMMENT '结算时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金记录表';

-- 提现记录表
CREATE TABLE `withdraw_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '提现金额',
    `withdraw_type` TINYINT NOT NULL COMMENT '提现方式: 1-微信, 2-支付宝, 3-银行卡',
    `account` VARCHAR(100) NOT NULL COMMENT '收款账户',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '银行名称(银行卡时)',
    `bank_branch` VARCHAR(100) DEFAULT NULL COMMENT '支行名称',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-已通过, 2-已拒绝, 3-已打款',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '交易流水号',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作员ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作员姓名',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_apply_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现记录表';

-- ============================================
-- 6. 社区相关表 (MongoDB推荐)
-- ============================================

-- 帖子表 (建议MongoDB)
CREATE TABLE `community_post` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '发布用户ID',
    `content` TEXT NOT NULL COMMENT '帖子内容',
    `images` JSON DEFAULT NULL COMMENT '图片列表',
    `video_url` VARCHAR(512) DEFAULT NULL COMMENT '视频URL',
    `topic_id` BIGINT DEFAULT NULL COMMENT '话题ID',
    `artwork_id` BIGINT DEFAULT NULL COMMENT '关联作品ID',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `share_count` INT DEFAULT 0 COMMENT '分享数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-删除, 1-正常, 2-审核中, 3-违规',
    `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_time` DATETIME DEFAULT NULL,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子表';

-- 评论表
CREATE TABLE `post_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID(回复)',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-删除, 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_time` DATETIME DEFAULT NULL,
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 点赞表
CREATE TABLE `post_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

-- 话题表
CREATE TABLE `topic` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '话题名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '话题描述',
    `icon` VARCHAR(512) DEFAULT NULL COMMENT '话题图标',
    `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图',
    `post_count` INT DEFAULT 0 COMMENT '帖子数',
    `follow_count` INT DEFAULT 0 COMMENT '关注数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_status` (`status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题表';

-- ============================================
-- 7. 系统配置相关表
-- ============================================

-- 系统配置表
CREATE TABLE `system_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `config_type` VARCHAR(50) DEFAULT 'string' COMMENT '类型: string/text/json/boolean/number',
    `group_name` VARCHAR(50) DEFAULT 'default' COMMENT '配置分组',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 协议表
CREATE TABLE `agreement` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL COMMENT '协议类型: privacy/user/promoter/distribution',
    `title` VARCHAR(200) NOT NULL COMMENT '协议标题',
    `content` TEXT NOT NULL COMMENT '协议内容',
    `version` VARCHAR(20) DEFAULT '1.0' COMMENT '版本号',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-停用, 1-当前有效',
    `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议表';

-- 通知消息表
CREATE TABLE `system_notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(为空则全体)',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT NOT NULL COMMENT '通知内容',
    `type` VARCHAR(50) DEFAULT 'system' COMMENT '类型: system/order/commission/activity',
    `link_type` VARCHAR(50) DEFAULT NULL COMMENT '跳转类型',
    `link_value` VARCHAR(255) DEFAULT NULL COMMENT '跳转值',
    `read_status` TINYINT DEFAULT 0 COMMENT '阅读状态: 0-未读, 1-已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    KEY `idx_read_status` (`read_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- ============================================
-- 8. 初始化数据
-- ============================================

-- 插入默认分类（按权重降序，数值大的排前面）
INSERT INTO `artwork_category` (`name`, `weight`, `status`) VALUES
('油画', 100, 1),
('水墨', 90, 1),
('版画', 80, 1),
('雕塑', 70, 1),
('摄影', 60, 1),
('装置', 50, 1);

-- 插入默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `group_name`, `remark`) VALUES
('commission.direct.rate', '0.05', 'number', 'commission', '直接推广佣金比例'),
('commission.team.rate', '0.02', 'number', 'commission', '团队奖励佣金比例'),
('order.pay.expire.minutes', '30', 'number', 'order', '订单支付过期时间(分钟)'),
('artwork.commission.default.rate', '0.05', 'number', 'artwork', '作品默认佣金比例'),
('withdraw.min.amount', '100', 'number', 'withdraw', '最低提现金额'),
('withdraw.fee.rate', '0', 'number', 'withdraw', '提现手续费率');
