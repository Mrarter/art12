CREATE TABLE artist_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    artist_id BIGINT NOT NULL COMMENT '艺术家ID',
    total_score INT DEFAULT 0 COMMENT '总评分',
    sales_score INT DEFAULT 0 COMMENT '销售表现分',
    influence_score INT DEFAULT 0 COMMENT '市场影响力分',
    activity_score INT DEFAULT 0 COMMENT '活跃度分',
    quality_score INT DEFAULT 0 COMMENT '作品质量分',
    review_score INT DEFAULT 0 COMMENT '藏家评价分',
    academic_score INT DEFAULT 0 COMMENT '学术资质分',
    internet_score INT DEFAULT 0 COMMENT '互联网资质分',
    level VARCHAR(20) COMMENT '艺术家等级 A+ / A / B / C / D',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_artist_id (artist_id)
) COMMENT='艺术家评分表';

CREATE TABLE artist_identity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    artist_id BIGINT NOT NULL COMMENT '艺术家ID',
    school_name VARCHAR(100) COMMENT '毕业院校',
    degree VARCHAR(50) COMMENT '学历',
    academic_title VARCHAR(50) COMMENT '职称：教授/副教授/讲师',
    association_name VARCHAR(100) COMMENT '协会身份',
    exhibitions TEXT COMMENT '展览经历',
    awards TEXT COMMENT '获奖经历',
    social_platform VARCHAR(50) COMMENT '抖音/小红书/视频号',
    social_account_url VARCHAR(255) COMMENT '账号链接',
    follower_count INT DEFAULT 0 COMMENT '粉丝数',
    content_type VARCHAR(100) COMMENT '内容类型',
    content_quality_score INT DEFAULT 0 COMMENT '内容质量分',
    conversion_score INT DEFAULT 0 COMMENT '内容转化分',
    verified TINYINT DEFAULT 0 COMMENT '是否审核通过',
    audit_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PASS/REJECT',
    audit_remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='艺术家身份资质表';

CREATE TABLE artwork (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    artist_id BIGINT NOT NULL COMMENT '艺术家ID',
    title VARCHAR(100) NOT NULL COMMENT '作品名称',
    initial_price DECIMAL(10,2) NOT NULL COMMENT '初始价格',
    current_price DECIMAL(10,2) NOT NULL COMMENT '当前价格',
    status VARCHAR(20) DEFAULT 'ON_SALE' COMMENT 'ON_SALE/SOLD/OFF_SALE/COLLECTED',
    collect_count INT DEFAULT 0 COMMENT '收藏人数',
    sale_count INT DEFAULT 0 COMMENT '成交次数',
    last_sale_time DATETIME NULL COMMENT '最近成交时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='艺术品基础表';

CREATE TABLE artwork_price_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    artist_id BIGINT NOT NULL COMMENT '艺术家ID',
    old_price DECIMAL(10,2) NOT NULL,
    new_price DECIMAL(10,2) NOT NULL,
    change_rate DECIMAL(8,4) COMMENT '涨跌幅',
    change_reason VARCHAR(50) COMMENT 'DAILY / SALE / COLLECT / SCORE / MANUAL',
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='作品价格变动记录表';
