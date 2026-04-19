-- 拾艺局数据库设计
-- 数据库: shiyiju

CREATE DATABASE IF NOT EXISTS shiyiju DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shiyiju;

-- ----------------------------
-- 1. 用户相关表
-- ----------------------------

-- 用户主表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    openid VARCHAR(64) NOT NULL COMMENT '微信OpenID',
    unionid VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    birthday VARCHAR(20) DEFAULT NULL COMMENT '生日',
    bio VARCHAR(500) DEFAULT NULL COMMENT '简介',
    region VARCHAR(100) DEFAULT NULL COMMENT '地区',
    identities VARCHAR(100) DEFAULT 'collector' COMMENT '身份列表，逗号分隔：artist,agent,collector,promoter',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    follower_count INT DEFAULT 0 COMMENT '粉丝数',
    following_count INT DEFAULT 0 COMMENT '关注数',
    register_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    UNIQUE KEY uk_openid (openid),
    KEY idx_phone (phone),
    KEY idx_unionid (unionid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 艺术家认证表
CREATE TABLE artist_certifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    id_card VARCHAR(20) NOT NULL COMMENT '身份证号',
    resume TEXT COMMENT '个人履历',
    artworks TEXT COMMENT '代表作图片URLs，逗号分隔',
    exhibits TEXT COMMENT '参展证明URLs，逗号分隔',
    status TINYINT DEFAULT 0 COMMENT '认证状态：0-待审核，1-已通过，2-已拒绝',
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
    review_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺术家认证表';

-- 经纪人认证表
CREATE TABLE agent_certifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    id_card VARCHAR(20) NOT NULL COMMENT '身份证号',
    company_name VARCHAR(100) DEFAULT NULL COMMENT '经纪公司名称',
    company_license VARCHAR(512) DEFAULT NULL COMMENT '公司营业执照',
    authorization_letter VARCHAR(512) DEFAULT NULL COMMENT '艺术家授权书',
    status TINYINT DEFAULT 0 COMMENT '认证状态：0-待审核，1-已通过，2-已拒绝',
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
    review_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经纪人认证表';

-- 艺荐官记录表
CREATE TABLE promoter_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    inviter_id BIGINT DEFAULT NULL COMMENT '邀请人ID',
    invite_code VARCHAR(20) NOT NULL COMMENT '邀请码',
    level TINYINT DEFAULT 1 COMMENT '等级：1-普通，2-高级，3-资深',
    agreement_status TINYINT DEFAULT 0 COMMENT '协议签署状态：0-未签署，1-已签署',
    agreement_time DATETIME DEFAULT NULL COMMENT '协议签署时间',
    total_commission BIGINT DEFAULT 0 COMMENT '累计佣金（分）',
    withdrawable_commission BIGINT DEFAULT 0 COMMENT '可提现佣金（分）',
    withdrawn_commission BIGINT DEFAULT 0 COMMENT '已提现佣金（分）',
    subordinate_count INT DEFAULT 0 COMMENT '下级人数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_invite_code (invite_code),
    KEY idx_inviter_id (inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺荐官记录表';

-- 收货地址表
CREATE TABLE addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区县',
    detail_address VARCHAR(200) NOT NULL COMMENT '详细地址',
    postal_code VARCHAR(20) DEFAULT NULL COMMENT '邮政编码',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认：0-否，1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- 用户关注关系表
CREATE TABLE user_follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    follow_user_id BIGINT NOT NULL COMMENT '被关注用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_follow (user_id, follow_user_id),
    KEY idx_follow_user_id (follow_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';

-- ----------------------------
-- 2. 商品相关表
-- ----------------------------

-- 艺术门类表
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    name VARCHAR(50) NOT NULL COMMENT '名称',
    icon VARCHAR(100) DEFAULT NULL COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_parent_id (parent_id),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺术门类表';

-- 作品主表
CREATE TABLE artworks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作品ID',
    title VARCHAR(200) NOT NULL COMMENT '作品名称',
    author_id BIGINT NOT NULL COMMENT '作者用户ID',
    category_id BIGINT DEFAULT NULL COMMENT '门类ID',
    art_type VARCHAR(50) DEFAULT NULL COMMENT '画种',
    medium VARCHAR(100) DEFAULT NULL COMMENT '材质/媒介',
    size VARCHAR(100) DEFAULT NULL COMMENT '尺寸',
    year INT DEFAULT NULL COMMENT '创作年份',
    edition VARCHAR(50) DEFAULT NULL COMMENT '版数',
    description TEXT COMMENT '作品描述',
    cover_image VARCHAR(512) NOT NULL COMMENT '封面图片',
    images TEXT COMMENT '图片URLs，逗号分隔',
    source TINYINT DEFAULT 1 COMMENT '来源：1-艺术家发布，2-经纪人代理，3-持有者转售，4-平台自营',
    holder_id BIGINT DEFAULT NULL COMMENT '持有者ID（转售时）',
    holder_since DATETIME DEFAULT NULL COMMENT '持有时间（转售时）',
    price BIGINT NOT NULL COMMENT '价格（分）',
    original_price BIGINT DEFAULT NULL COMMENT '原价（分）',
    stock INT DEFAULT 1 COMMENT '库存',
    status TINYINT DEFAULT 1 COMMENT '状态：0-下架，1-上架，2-已售罄',
    price_rise DECIMAL(5,2) DEFAULT 0.00 COMMENT '价格涨幅百分比',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    favorite_count INT DEFAULT 0 COMMENT '收藏数',
    sale_count INT DEFAULT 0 COMMENT '销量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_author_id (author_id),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_price (price),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品表';

-- 作品价格历史表
CREATE TABLE artwork_price_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    old_price BIGINT NOT NULL COMMENT '原价格（分）',
    new_price BIGINT NOT NULL COMMENT '新价格（分）',
    change_type TINYINT DEFAULT 1 COMMENT '变动类型：1-手动调整，2-动态调价',
    change_reason VARCHAR(200) DEFAULT NULL COMMENT '变动原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_artwork_id (artwork_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品价格历史表';

-- 作品收藏表
CREATE TABLE artwork_favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_favorite (user_id, artwork_id),
    KEY idx_artwork_id (artwork_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品收藏表';

-- 用户浏览足迹表
CREATE TABLE browse_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览足迹表';

-- ----------------------------
-- 3. 订单相关表
-- ----------------------------

-- 订单主表
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    promoter_id BIGINT DEFAULT NULL COMMENT '艺荐官ID（分销来源）',
    total_amount BIGINT NOT NULL COMMENT '订单总金额（分）',
    discount_amount BIGINT DEFAULT 0 COMMENT '优惠金额（分）',
    pay_amount BIGINT NOT NULL COMMENT '实付金额（分）',
    commission_amount BIGINT DEFAULT 0 COMMENT '分销佣金（分）',
    address_id BIGINT DEFAULT NULL COMMENT '收货地址ID',
    receiver_name VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
    receiver_address VARCHAR(300) DEFAULT NULL COMMENT '收货地址',
    remark VARCHAR(200) DEFAULT NULL COMMENT '订单备注',
    source TINYINT DEFAULT 1 COMMENT '来源：1-立即购买，2-购物车，3-拍卖成交',
    status TINYINT DEFAULT 1 COMMENT '状态：0-已取消，1-待付款，2-已付款，3-已发货，4-已收货，5-已完成，6-退款中，7-已退款',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    ship_time DATETIME DEFAULT NULL COMMENT '发货时间',
    receive_time DATETIME DEFAULT NULL COMMENT '收货时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单明细表
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    title VARCHAR(200) NOT NULL COMMENT '作品名称（冗余）',
    cover_image VARCHAR(512) NOT NULL COMMENT '封面图片（冗余）',
    author_name VARCHAR(100) DEFAULT NULL COMMENT '作者名称（冗余）',
    price BIGINT NOT NULL COMMENT '作品价格（分）',
    quantity INT DEFAULT 1 COMMENT '数量',
    subtotal BIGINT NOT NULL COMMENT '小计金额（分）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_order_id (order_id),
    KEY idx_artwork_id (artwork_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- 购物车表
CREATE TABLE carts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    quantity INT DEFAULT 1 COMMENT '数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_cart (user_id, artwork_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ----------------------------
-- 4. 拍卖相关表
-- ----------------------------

-- 拍卖专场表
CREATE TABLE auction_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '专场ID',
    name VARCHAR(200) NOT NULL COMMENT '专场名称',
    cover_image VARCHAR(512) DEFAULT NULL COMMENT '封面图片',
    description TEXT COMMENT '专场描述',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-预告中，2-进行中，3-已结束',
    lot_count INT DEFAULT 0 COMMENT '拍品数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拍卖专场表';

-- 拍卖拍品表
CREATE TABLE auction_lots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '拍品ID',
    session_id BIGINT NOT NULL COMMENT '专场ID',
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    lot_no VARCHAR(20) NOT NULL COMMENT '拍品编号',
    start_price BIGINT NOT NULL COMMENT '起拍价（分）',
    reserve_price BIGINT DEFAULT NULL COMMENT '保留价（分）',
    current_price BIGINT NOT NULL COMMENT '当前价（分）',
    bid_increment BIGINT DEFAULT 100 COMMENT '加价幅度（分）',
    bid_count INT DEFAULT 0 COMMENT '出价次数',
    highest_bidder_id BIGINT DEFAULT NULL COMMENT '最高出价者ID',
    deposit_amount BIGINT DEFAULT 10000 COMMENT '保证金金额（分）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待拍卖，1-拍卖中，2-已成交，3-流拍',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    final_price BIGINT DEFAULT NULL COMMENT '成交价（分）',
    winner_id BIGINT DEFAULT NULL COMMENT '成交者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_session_id (session_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拍卖拍品表';

-- 拍卖出价记录表
CREATE TABLE auction_bids (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    lot_id BIGINT NOT NULL COMMENT '拍品ID',
    user_id BIGINT NOT NULL COMMENT '出价用户ID',
    bid_price BIGINT NOT NULL COMMENT '出价金额（分）',
    bid_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '出价时间',
    is_winning TINYINT DEFAULT 0 COMMENT '是否当前最高：0-否，1-是',
    KEY idx_lot_id (lot_id),
    KEY idx_user_id (user_id),
    KEY idx_bid_time (bid_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拍卖出价记录表';

-- 拍卖保证金表
CREATE TABLE auction_deposits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    lot_id BIGINT NOT NULL COMMENT '拍品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount BIGINT NOT NULL COMMENT '保证金金额（分）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已退款，3-已抵扣',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    refund_time DATETIME DEFAULT NULL COMMENT '退款时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_lot_user (lot_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拍卖保证金表';

-- ----------------------------
-- 5. 分销相关表
-- ----------------------------

-- 佣金记录表
CREATE TABLE commission_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    promoter_id BIGINT NOT NULL COMMENT '艺荐官ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_item_id BIGINT DEFAULT NULL COMMENT '订单明细ID',
    level TINYINT NOT NULL COMMENT '分销层级：1-一级，2-二级',
    commission_rate DECIMAL(5,4) NOT NULL COMMENT '佣金比例',
    order_amount BIGINT NOT NULL COMMENT '订单金额（分）',
    commission_amount BIGINT NOT NULL COMMENT '佣金金额（分）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待结算，1-已结算，2-已提现，-1-已失效',
    settle_time DATETIME DEFAULT NULL COMMENT '结算时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_promoter_id (promoter_id),
    KEY idx_order_id (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金记录表';

-- 佣金规则表
CREATE TABLE commission_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    level TINYINT NOT NULL COMMENT '艺荐官等级',
    first_rate DECIMAL(5,4) NOT NULL COMMENT '一级佣金比例',
    second_rate DECIMAL(5,4) NOT NULL COMMENT '二级佣金比例',
    team_bonus DECIMAL(5,4) DEFAULT 0 COMMENT '团队奖金比例',
    min_withdraw BIGINT DEFAULT 100 COMMENT '最低提现金额（分）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金规则表';

-- 提现记录表
CREATE TABLE withdraw_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    promoter_id BIGINT NOT NULL COMMENT '艺荐官ID',
    amount BIGINT NOT NULL COMMENT '提现金额（分）',
    fee_amount BIGINT DEFAULT 0 COMMENT '手续费（分）',
    actual_amount BIGINT NOT NULL COMMENT '实际到账金额（分）',
    account_type VARCHAR(20) NOT NULL COMMENT '账户类型：bank, wechat, alipay',
    account_info VARCHAR(200) NOT NULL COMMENT '账户信息（加密）',
    account_name VARCHAR(50) NOT NULL COMMENT '账户姓名',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已通过，2-已拒绝，3-已打款',
    reject_reason VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    process_time DATETIME DEFAULT NULL COMMENT '处理时间',
    transfer_time DATETIME DEFAULT NULL COMMENT '打款时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_promoter_id (promoter_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提现记录表';

-- ----------------------------
-- 6. 社区相关表
-- ----------------------------

-- 帖子表
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    content TEXT NOT NULL COMMENT '帖子内容',
    images TEXT DEFAULT NULL COMMENT '图片URLs，逗号分隔',
    topic_id BIGINT DEFAULT NULL COMMENT '话题ID',
    artwork_id BIGINT DEFAULT NULL COMMENT '关联作品ID',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    share_count INT DEFAULT 0 COMMENT '分享数',
    status TINYINT DEFAULT 1 COMMENT '状态：0-删除，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id),
    KEY idx_topic_id (topic_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 帖子评论表
CREATE TABLE post_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '评论者ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID（回复）',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_post_id (post_id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论表';

-- 帖子点赞表
CREATE TABLE post_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_like (post_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';

-- 话题表
CREATE TABLE topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '话题ID',
    name VARCHAR(50) NOT NULL COMMENT '话题名称',
    description VARCHAR(200) DEFAULT NULL COMMENT '话题描述',
    cover_image VARCHAR(512) DEFAULT NULL COMMENT '话题封面',
    post_count INT DEFAULT 0 COMMENT '帖子数量',
    follow_count INT DEFAULT 0 COMMENT '关注数',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

-- ----------------------------
-- 7. 消息相关表
-- ----------------------------

-- 消息表
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    type VARCHAR(30) NOT NULL COMMENT '消息类型：order, system, activity, chat',
    title VARCHAR(100) DEFAULT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    data VARCHAR(500) DEFAULT NULL COMMENT '扩展数据（JSON）',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0-否，1-是',
    read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_type (type),
    KEY idx_is_read (is_read),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ----------------------------
-- 8. 系统配置表
-- ----------------------------

-- 系统配置表
CREATE TABLE sys_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_type VARCHAR(30) DEFAULT 'string' COMMENT '配置类型：string, number, boolean, json',
    description VARCHAR(200) DEFAULT NULL COMMENT '配置描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- Banner 配置表
CREATE TABLE banners (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    image_url VARCHAR(512) NOT NULL COMMENT '图片URL',
    link_type VARCHAR(30) DEFAULT NULL COMMENT '跳转类型：gallery, auction, article, url',
    link_value VARCHAR(200) DEFAULT NULL COMMENT '跳转值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    start_time DATETIME DEFAULT NULL COMMENT '展示开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '展示结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Banner配置表';

-- ----------------------------
-- 初始化数据
-- ----------------------------

-- 插入艺术门类
INSERT INTO categories (name, icon, sort_order) VALUES
('油画', '🖼️', 1),
('雕塑', '🗿', 2),
('水墨', '🖌️', 3),
('版画', '📜', 4),
('装置', '⚙️', 5),
('摄影', '📷', 6);

-- 插入佣金规则
INSERT INTO commission_rules (name, level, first_rate, second_rate, min_withdraw) VALUES
('普通艺荐官', 1, 0.1000, 0.0500, 10000),
('高级艺荐官', 2, 0.1500, 0.0800, 10000),
('资深艺荐官', 3, 0.2000, 0.1000, 10000);
