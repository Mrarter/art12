-- ============================================================
-- 拾艺局本地数据库导入脚本
-- 从腾讯云 CynosDB 迁移到本地 MySQL
-- 运行方式: mysql -u root -p < import_local.sql
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS shiyiju_local DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shiyiju_local;

-- ============================================================
-- 作品分类表
-- ============================================================
DROP TABLE IF EXISTS artwork_category;
CREATE TABLE artwork_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    icon VARCHAR(50) DEFAULT 'Picture' COMMENT '图标',
    weight INT DEFAULT 0 COMMENT '排序权重',
    artwork_count INT DEFAULT 0 COMMENT '作品数量',
    status INT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品分类表';

-- ============================================================
-- 作品表
-- ============================================================
DROP TABLE IF EXISTS artwork;
CREATE TABLE artwork (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    artwork_id BIGINT NOT NULL COMMENT '作品ID',
    display_artwork_id VARCHAR(20) COMMENT '展示用作品ID',
    artwork_code VARCHAR(50) COMMENT '作品编码',
    artwork_uid VARCHAR(50) COMMENT '作品唯一标识',
    author_id BIGINT COMMENT '作者用户ID',
    display_author_id VARCHAR(30) COMMENT '展示用作者ID',
    author_uid VARCHAR(50) COMMENT '作者唯一标识',
    title VARCHAR(200) NOT NULL COMMENT '作品标题',
    artist_name VARCHAR(100) COMMENT '艺术家姓名',
    author_name VARCHAR(100) COMMENT '作者姓名',
    cover VARCHAR(500) COMMENT '封面图URL',
    price DECIMAL(15,2) DEFAULT 0 COMMENT '价格（分）',
    category_name VARCHAR(100) COMMENT '分类名称',
    art_type VARCHAR(100) COMMENT '艺术品类型',
    size VARCHAR(100) COMMENT '尺寸',
    year INT COMMENT '创作年份',
    favorite_count INT DEFAULT 0 COMMENT '收藏数',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    description TEXT COMMENT '作品描述',
    daily_view_count INT DEFAULT 0 COMMENT '日浏览数',
    daily_like_count INT DEFAULT 0 COMMENT '日点赞数',
    display_view_count INT DEFAULT 0 COMMENT '展示浏览数',
    display_like_count INT DEFAULT 0 COMMENT '展示点赞数',
    weight INT DEFAULT 0 COMMENT '排序权重',
    ownership_type INT DEFAULT 1 COMMENT '所有权类型',
    status INT DEFAULT 1 COMMENT '状态：1上架 0下架',
    distribution_enabled TINYINT DEFAULT 0 COMMENT '是否开启分销',
    commission_rate DECIMAL(5,2) DEFAULT 10.00 COMMENT '佣金比例%',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品表';

-- ============================================================
-- 用户表
-- ============================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(50) NOT NULL COMMENT '用户唯一标识',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '/images/default-avatar.png' COMMENT '头像',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    is_vip TINYINT DEFAULT 0 COMMENT '是否VIP',
    is_artist TINYINT DEFAULT 0 COMMENT '是否艺术家',
    is_promoter TINYINT DEFAULT 0 COMMENT '是否推广员',
    balance DECIMAL(15,2) DEFAULT 0 COMMENT '余额',
    coupon_count INT DEFAULT 0 COMMENT '优惠券数量',
    total_consume DECIMAL(15,2) DEFAULT 0 COMMENT '累计消费',
    order_count INT DEFAULT 0 COMMENT '订单数',
    register_time DATETIME COMMENT '注册时间',
    source VARCHAR(50) COMMENT '来源',
    status INT DEFAULT 1 COMMENT '状态',
    identities JSON COMMENT '身份信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- Banner表
-- ============================================================
DROP TABLE IF EXISTS banner;
CREATE TABLE banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '标题',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    type VARCHAR(50) DEFAULT 'OTHER' COMMENT '类型',
    target VARCHAR(100) COMMENT '跳转目标',
    sort_no INT DEFAULT 0 COMMENT '排序号',
    status VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Banner表';

-- ============================================================
-- 管理员表
-- ============================================================
DROP TABLE IF EXISTS admin_user;
CREATE TABLE admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码（明文）',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role_code VARCHAR(50) DEFAULT 'admin' COMMENT '角色',
    status INT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ============================================================
-- 数据导入
-- ============================================================

-- 管理员（默认账号）
INSERT INTO admin_user (username, password, email, phone, role_code, status) VALUES
('admin', 'admin123', 'admin@shiyiju.com', '13800000000', 'super', 1);

-- 作品分类数据
INSERT INTO artwork_category (id, name, icon, weight, artwork_count, status, create_time) VALUES
(1, '油画（布面油画）', 'Picture', 12, 23, 1, '2026-04-20 08:30:00'),
(3, '油画（木板油画）', 'Picture', 11, 4, 1, '2026-04-20 08:30:00'),
(11, '中国画', 'Picture', 10, 0, 1, '2026-04-28 08:34:27'),
(4, '雕塑', 'Picture', 2, 0, 1, '2026-04-20 08:30:00'),
(2, '水墨', 'Picture', 2, 2, 1, '2026-04-20 08:30:00'),
(14, '最终测试', 'Picture', 1, 0, 1, '2026-04-28 08:36:34');

-- 作品数据
INSERT INTO artwork (id, artwork_id, display_artwork_id, artwork_code, artwork_uid, author_id, display_author_id, author_uid, title, artist_name, author_name, cover, price, category_name, art_type, size, year, favorite_count, view_count, description, daily_view_count, daily_like_count, display_view_count, display_like_count, weight, ownership_type, status, distribution_enabled, commission_rate, create_time) VALUES
(37, 37, '0037', 'qt202604260003', 'qt202604260003', 13, 'USR2026050700010013', 'USR2026050700010013', '静物0752', '孟儒', '孟儒', '/upload/images/2026/05/01/57de9acd0d67471596b0a719ebfc7a07.png', 1010000, '油画（木板油画）', '分类:油画（布面油画）', '40 × 40', 2024, 1, 44, NULL, 5, 0, 49, 1, 2, 1, 1, 0, 10, '2026-04-26 19:55:44'),
(19, 19, '0019', 'ART202604200001K9M3', 'ART202604200001K9M3', 1, 'USR2026050700010001', 'USR2026050700010001', '测试作品-已修复 作品描述保存不下来', '罗中立', '罗中立', 'http://localhost:8087/upload/images/2026/04/27/42309a412da343f3ba2f8a4269237d76.png', 1240673, '油画（布面油画）', '布面油画', '100*80', 2024, 0, 104, '撒地方萨达', 0, 0, 104, 0, 2, 1, 1, 1, 13, '2026-04-20 18:22:57'),
(38, 38, '0038', NULL, NULL, 13, 'USR2026050700010013', 'USR2026050700010013', '小女孩', '孟儒', '孟儒', 'http://localhost:8087/upload/images/2026/05/04/faf48178c39340d78309baefc0d22384.png', 1200000, NULL, '分类:油画（布面油画）', '100*80', 2024, 0, 36, '', 1, 0, 37, 0, 1, 1, 1, 0, 10, '2026-04-26 20:47:26'),
(64, 64, '0064', 'yh202605030004', 'yh202605030004', 13, 'USR2026050700010013', 'USR2026050700010013', '思 2', '孟儒', '孟儒', '/upload/images/2026/05/03/bb403e94b5d24a729cc0478f457be31c.png', 980000, '油画（布面油画）', '油画（布面油画）', '50×70cm', 2024, 0, 32, NULL, 2, 0, 34, 0, 0, 1, 1, 0, 10, '2026-05-03 13:28:02'),
(63, 63, '0063', 'yh202605030003', 'yh202605030003', 15, 'USR2026050700010015', 'USR2026050700010015', '秋', '艺术品爱好者', '艺术品爱好者', '/upload/images/2026/05/03/07a23c08a40745bca74ac8cf53ae54fb.png', 10000, '油画（布面油画）', '油画（布面油画）', '30×90cm', 2024, 0, 37, '秋天来了', 664, 96, 701, 96, 0, 1, 1, 0, 10, '2026-05-03 13:23:31'),
(53, 53, '0053', 'yh202605020002', 'yh202605020002', 14, 'USR2026050700010014', 'USR2026050700010014', '静思', '孟娜丽莎', '孟娜丽莎', 'http://localhost:8087/upload/images/2026/05/02/092822c3c3694bd0b09260870da0e3db.png', 1280000, NULL, '布面油画', '50×70cm', 2026, 0, 8, '床前明月光', 0, 0, 8, 0, 0, 1, 1, 0, 10, '2026-05-02 23:14:40'),
(49, 49, '0049', 'yh202605020001', 'yh202605020001', 13, 'USR2026050700010013', 'USR2026050700010013', 'Codex发布验证0502', '孟儒', '孟儒', 'http://localhost:8087/upload/images/2026/05/01/87da759725254b498b1f5abf36f2b936.jpg', 1236150, '油画（布面油画）', '油画（布面油画）', '50 × 70', 2026, 1, 12, '发布接口验证', 0, 0, 12, 1, 0, 1, 1, 0, 10, '2026-05-02 15:42:49'),
(46, 46, '0046', 'qt202604300001', 'qt202604300001', 21, 'USR202605070001E8F0', 'USR202605070001E8F0', '归来', '00000000', '00000000', 'http://localhost:8087/upload/images/2026/05/01/61907e62deda4f8991befa3e6f0e1a9f.png', 1000000, NULL, '分类:油画（布面油画）', '40*60', NULL, 0, 4, '遥襟甫畅，逸兴遄飞。天高地迥，觉宇宙之无穷。', 0, 0, 4, 0, 0, 1, 1, 0, 10, '2026-04-30 22:19:22'),
(45, 45, '0045', 'qt202604270003', 'qt202604270003', 3, 'USR2026050700010003', 'USR2026050700010003', '这是测试链接', '岳敏君', '岳敏君', 'http://localhost:8087/upload/images/2026/04/29/eccbb3db1cac4fd4a3d0948169ce19e8.jpg', 1000000, '油画（木板油画）', '分类:油画（布面油画）', '100*60', 2024, 0, 12, 'sadf jhoas df[pwoqjf e;wqke ;jqwelf; jka sadfsadf', 0, 0, 12, 0, 0, 1, 1, 1, 10, '2026-04-27 13:02:18'),
(44, 44, '0044', 'qt202604270002', 'qt202604270002', 20, 'USR202605070001810A', 'USR202605070001810A', '爱思', '11 ', '11 ', 'http://localhost:8087/upload/images/2026/05/04/eda11ec09f294ce2976dec0767af1120.jpg', 1000000, '油画（布面油画）', '分类:油画（布面油画）', '50', 2021, 0, 6, '阿斯顿发见识到了咖啡机哈上课两极分化阿是抠脚大汉法拉克 ', 0, 0, 6, 0, 0, 1, 1, 0, 10, '2026-04-27 12:59:53'),
(43, 43, '0043', 'qt202604270001', 'qt202604270001', 19, 'USR202605070001F4C4', 'USR202605070001F4C4', '2', '2', '2', 'http://localhost:8087/upload/images/2026/04/27/6aba32ce9d0e4d16a835262044b81944.png', 0, '油画（布面油画）', NULL, NULL, NULL, 0, 6, '', 0, 0, 6, 0, 0, 1, 1, 0, 10, '2026-04-27 12:23:02'),
(42, 42, '0042', 'qt202604260005', 'qt202604260005', 12, 'USR2026050700010012', 'USR2026050700010012', '1111111', '缩放', '缩放', 'http://localhost:8087/upload/images/2026/04/26/dad180ddaf1943a381526326522561ea.jpg', 0, '油画（布面油画）', '1', NULL, NULL, 0, 1, '', 0, 0, 1, 0, 0, 1, 1, 0, 10, '2026-04-26 20:57:56'),
(41, 41, '0041', 'qt202604260004', 'qt202604260004', 11, 'USR2026050700010011', 'USR2026050700010011', '前端测试作品', '前端测试艺术家', '前端测试艺术家', 'http://localhost:8087/upload/images/2026/04/27/533df4d88c644c569a12965facb30800.png', 10000000, '油画（布面油画）', NULL, '100*80', 2025, 0, 3, '', 0, 0, 3, 0, 0, 1, 1, 0, 10, '2026-04-26 20:53:35'),
(40, 40, '0040', NULL, NULL, 10, 'USR2026050700010010', 'USR2026050700010010', '继续测试作品', '测试艺术家A', '测试艺术家A', 'http://localhost:8087/upload/images/2026/04/27/7899ceb38e9d4887a28e3e6f12d34f4f.jpeg', 50000, '油画（布面油画）', '', '', NULL, 0, 2, '', 0, 0, 2, 0, 0, 1, 1, 0, 10, '2026-04-26 20:50:29'),
(39, 39, '0039', NULL, NULL, 9, 'USR2026050700010009', 'USR2026050700010009', '测试作品5', '孙七', '孙七', 'http://localhost:8087/upload/images/2026/04/27/6a4a6ea5d8f2491aac1fd3a9d2e1034f.png', 50000, '油画（布面油画）', '', '', NULL, 0, 0, '', 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-26 20:47:38'),
(35, 35, '0035', 'qt202604260001', 'qt202604260001', 4, 'USR2026050700010004', 'USR2026050700010004', '测试作品', '刘亦菲', '刘亦菲', 'https://picsum.photos/400/400?random=203', 10000, '油画（木板油画）', '', '', NULL, 0, 2, '', 0, 0, 2, 0, 0, 1, 1, 0, 10, '2026-04-26 19:55:23'),
(34, 34, '0034', 'qt202604250003', 'qt202604250003', 8, 'USR2026050700010008', 'USR2026050700010008', '加勒比海盗', '李小璐', '李小璐', 'https://picsum.photos/400/400?random=206', 1000000, '油画（布面油画）', '布面油画', '60*80', 2024, 0, 7, '白日依山尽  黄鹤入海流', 0, 0, 7, 0, 0, 1, 1, 0, 10, '2026-04-25 22:32:39'),
(33, 33, '0033', 'qt202604250006', 'qt202604250006', 7, 'USR2026050700010007', 'USR2026050700010007', '更新后的标题', '画家A', '画家A', 'https://picsum.photos/400/400?random=205', 41000, '油画（布面油画）', NULL, NULL, NULL, 0, 1, '', 0, 0, 1, 0, 0, 1, 1, 0, 10, '2026-04-25 20:58:22'),
(32, 32, '0032', 'qt202604250005', 'qt202604250005', 5, 'USR2026050700010005', 'USR2026050700010005', '测试发布', '测试', '测试', 'https://picsum.photos/400/400', 0, '油画（布面油画）', NULL, NULL, NULL, 0, 0, '', 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:58:17'),
(31, 31, '0031', 'qt202604250004', 'qt202604250004', 6, 'USR2026050700010006', 'USR2026050700010006', '测试作品', '测试艺术家', '测试艺术家', 'https://picsum.photos/400/400', 100000, NULL, NULL, NULL, NULL, 0, 0, NULL, 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:56:55'),
(30, 30, '0030', 'qt202604250006', 'qt202604250006', 46, '0046', '0046', '测试原价0', NULL, NULL, 'https://picsum.photos/400/400', 0, NULL, NULL, NULL, NULL, 0, 0, NULL, 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:35:40'),
(29, 29, '0029', 'qt202604250005', 'qt202604250005', 46, '0046', '0046', '测试原价null', NULL, NULL, 'https://picsum.photos/400/400', 10000, NULL, NULL, NULL, NULL, 0, 1, NULL, 0, 0, 1, 0, 0, 1, 1, 0, 10, '2026-04-25 20:35:29'),
(28, 28, '0028', 'qt202604250004', 'qt202604250004', 18, 'USR202605070001198A', 'USR202605070001198A', '测试价格0', '123', '123', 'https://picsum.photos/400/400', 100, '油画（布面油画）', NULL, NULL, NULL, 0, 0, '', 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:21:17'),
(27, 27, '0027', 'qt202604250003', 'qt202604250003', 46, '0046', '0046', '测试只填价格', NULL, NULL, 'https://picsum.photos/400/400', 20000, NULL, NULL, NULL, NULL, 0, 0, NULL, 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:20:52'),
(26, 26, '0026', 'qt202604250002', 'qt202604250002', 4, 'USR2026050700010004', 'USR2026050700010004', '测试价格', '刘亦菲', '刘亦菲', 'https://picsum.photos/400/400', 0, '油画（布面油画）', NULL, NULL, NULL, 0, 0, '', 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:20:11'),
(25, 25, '0025', 'qt202604250001', 'qt202604250001', 1, 'USR2026050700010001', 'USR2026050700010001', '新作品1', '罗中立', '罗中立', 'http://localhost:8087/upload/images/2026/04/29/b854903755c8405da8a80ba3a4b56799.jpg', 500, '油画（布面油画）', '分类:油画（布面油画）', '', NULL, 0, 2, '', 0, 0, 2, 0, 0, 1, 1, 0, 10, '2026-04-25 20:17:07'),
(24, 24, '0024', 'yh202604250002', 'yh202604250002', 1, 'USR2026050700010001', 'USR2026050700010001', '远航1', '罗中立', '罗中立', '', 0, '油画（布面油画）', '布面油画', '100*80', 2024, 0, 0, '这里是详情描述', 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:07:05'),
(23, 23, '0023', 'yh202604250001', 'yh202604250001', 1, 'USR2026050700010001', 'USR2026050700010001', '远航1', '罗中立', '罗中立', '', 0, '油画（布面油画）', '布面油画', '100*80', 2024, 0, 0, '这里是详情描述', 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-25 20:06:56'),
(22, 22, '0022', 'ART202604220022F8FD', 'ART202604220022F8FD', 17, 'USR202605070001CFB5', 'USR202605070001CFB5', '11', '1', '1', 'https://picsum.photos/400/500?random=107', 100, '水墨', NULL, NULL, NULL, 0, 0, '', 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-22 19:14:25'),
(21, 21, '0021', 'ART20260422002145EP', 'ART20260422002145EP', 4, 'USR2026050700010004', 'USR2026050700010004', '归来的帆船', '刘亦菲', '刘亦菲', 'https://picsum.photos/400/400?random=204', 10000, '油画（布面油画）', '布面油画', '60*60', 2024, 0, 34, '唧唧复唧唧，木兰当户织。', 0, 0, 34, 0, 0, 1, 1, 1, 7, '2026-04-22 19:11:37'),
(18, 18, '0018', 'ART2026042000183JDQ', 'ART2026042000183JDQ', 5, 'USR2026050700010005', 'USR2026050700010005', '测试0价格', '测试', '测试', 'https://picsum.photos/400/400?random=105', 0, '油画（布面油画）', NULL, NULL, NULL, 0, 0, NULL, 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-20 18:22:46'),
(17, 17, '0017', 'ART2026042000171SYV', 'ART2026042000171SYV', 17, 'USR202605070001CFB5', 'USR202605070001CFB5', '1', '1', '1', 'https://picsum.photos/400/500?random=104', 1000, '水墨', NULL, NULL, NULL, 0, 0, NULL, 0, 0, 0, 0, 0, 1, 1, 0, 10, '2026-04-20 18:21:02'),
(16, 16, '0016', 'ART202604200016BUS6', 'ART202604200016BUS6', 4, 'USR2026050700010004', 'USR2026050700010004', '测试作品', '刘亦菲', '刘亦菲', 'https://picsum.photos/400/400?random=203', 100, '油画（木板油画）', NULL, NULL, NULL, 0, 4, '', 0, 0, 4, 0, 0, 1, 1, 1, 9, '2026-04-20 18:18:24'),
(15, 15, '0015', 'ART2026042000159GTF', 'ART2026042000159GTF', 3, 'USR2026050700010003', 'USR2026050700010003', '处决', '岳敏君', '岳敏君', 'https://picsum.photos/400/400?random=202', 120000000, '油画（布面油画）', '油画', '100*80', NULL, 0, 0, '', 0, 0, 0, 0, 0, 1, 1, 1, 11, '2026-04-20 17:56:17'),
(14, 14, '0014', 'ART2026042000143AIJ', 'ART2026042000143AIJ', 2, 'USR2026050700010002', 'USR2026050700010002', '测试作品更新', '曾凡志', '曾凡志', 'https://picsum.photos/400/400?random=201', 2500000, '油画（布面油画）', '油画', '140*40', 2024, 0, 1, '萨德啤酒哦阿斯蒂芬撒地方；教案苏东坡否啊我额飞机；啊我快乐飞；卡位附件萨德啤酒哦阿斯蒂芬撒地方；教案苏东坡否啊我额飞机；啊我快乐飞；卡位附件二‘啊微积分i啊我姐夫乐卡斯的；冷风机卡死；蝶恋蜂狂静安寺；来得及反馈；啊双联单控辅；啊四六级快递费；利君沙地方；利君沙看对方；看见撒地方；颗粒剂萨德； 龙口粉丝；的记录卡撒；拉倒辅萨德啤酒哦阿斯蒂芬撒地方；教案苏东坡否啊我额飞机；啊我快乐飞；卡位附件二‘啊微积分i啊我姐夫乐卡斯的；冷风机卡死；蝶恋蜂狂静安寺；来得及反馈；啊双联单控辅；啊四六级快递费；利君沙地方；利君沙看对方；看见撒地方；颗粒剂萨德； 龙口粉丝；的记录卡撒；拉倒辅萨德啤酒哦阿斯蒂芬撒地方；教案苏东坡否啊我额飞机；啊我快乐飞；卡位附件二‘啊微积分i啊我姐夫乐卡斯的；冷风机卡死；蝶恋蜂狂静安寺；来得及反馈；啊双联单控辅；啊四六级快递费；利君沙地方；利君沙看对方；看见撒地方；颗粒剂萨德； 龙口粉丝；的记录卡撒；拉倒辅萨德啤酒哦阿斯蒂芬撒地方；教案苏东坡否啊我额飞机；啊我快乐飞；卡位附件二‘啊微积分i啊我姐夫乐卡斯的；冷风机卡死；蝶恋蜂狂静安寺；来得及反馈；啊双联单控辅；啊四六级快递费；利君沙地方；利君沙看对方；看见撒地方；颗粒剂萨德； 龙口粉丝；的记录卡撒；拉倒辅二‘啊微积分i啊我姐夫乐卡斯的；冷风机卡死；蝶恋蜂狂静安寺；来得及反馈；啊双联单控辅；啊四六级快递费；利君沙地方；利君沙看对方；看见撒地方；颗粒剂萨德； 龙口粉丝；的记录卡撒；拉倒辅', 100, 50, 101, 50, 0, 1, 0, 0, 10, '2026-04-20 17:54:14'),
(1, 1, '0001', 'ART202604200001GU4J', 'ART202604200001GU4J', 1, 'USR2026050700010001', 'USR2026050700010001', '测试作品', '罗中立', '罗中立', 'https://picsum.photos/400/400?random=101', 0, '油画（布面油画）', 'æ°´å¢¨', '50x70cm', NULL, 0, 10, '', 0, 0, 10, 0, 0, 1, 1, 0, 10, '2026-04-20 08:21:53');

-- 用户数据
INSERT INTO sys_user (user_id, uid, nickname, avatar, phone, email, is_vip, is_artist, is_promoter, balance, coupon_count, total_consume, order_count, register_time, source, status, identities) VALUES
(21, 'USR202605070001E8F0', '00000000', '/images/default-avatar.png', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 21:03:37', 'wechat', 1, '["collector", "artist"]'),
(20, 'USR202605070001810A', '11', '/images/default-avatar.png', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 21:03:37', 'wechat', 1, '["collector", "artist"]'),
(19, 'USR202605070001F4C4', '2', '/images/default-avatar.png', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 21:03:37', 'wechat', 1, '["collector", "artist"]'),
(18, 'USR202605070001198A', '123', '/images/default-avatar.png', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 21:03:36', 'wechat', 1, '["collector", "artist"]'),
(17, 'USR202605070001CFB5', '1', '/images/default-avatar.png', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 21:03:36', 'wechat', 1, '["collector", "artist"]'),
(16, 'USR2026050700017D12', '1', '/images/default-avatar.png', '', NULL, 0, 0, 0, 0, 0, 0, 0, '2026-05-07 21:02:25', 'wechat', 1, '["collector"]'),
(15, 'USR2026050700010015', '艺术品爱好者', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:29', 'wechat', 1, '["artist", "collector"]'),
(14, 'USR2026050700010014', '孟娜丽莎', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:28', 'wechat', 1, '["artist", "collector"]'),
(13, 'USR2026050700010013', '孟儒', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:27', 'wechat', 1, '["artist", "collector"]'),
(12, 'USR2026050700010012', '缩放', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:25', 'wechat', 1, '["artist", "collector"]'),
(11, 'USR2026050700010011', '前端测试艺术家', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:23', 'wechat', 1, '["artist", "collector"]'),
(10, 'USR2026050700010010', '测试艺术家A', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:21', 'wechat', 1, '["artist", "collector"]'),
(9, 'USR2026050700010009', '孙七', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:19', 'wechat', 1, '["artist", "collector"]'),
(8, 'USR2026050700010008', '李小璐', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:18', 'wechat', 1, '["artist", "collector"]'),
(7, 'USR2026050700010007', '画家A', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:16', 'wechat', 1, '["artist", "collector"]'),
(6, 'USR2026050700010006', '测试艺术家', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:15', 'wechat', 1, '["artist", "collector"]'),
(5, 'USR2026050700010005', '测试', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:10', 'wechat', 1, '["artist", "collector"]'),
(4, 'USR2026050700010004', '刘亦菲', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:08', 'wechat', 1, '["artist", "collector"]'),
(3, 'USR2026050700010003', '岳敏君', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:06', 'wechat', 1, '["artist", "collector"]'),
(2, 'USR2026050700010002', '曾凡志', '', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:05', 'wechat', 1, '["artist", "collector"]'),
(1, 'USR2026050700010001', '罗中立', 'http://192.168.1.109:8087/upload/images/2026/05/10/7bb890175b7f4a539093d48b65cf08e7.png', '', NULL, 0, 1, 0, 0, 0, 0, 0, '2026-05-07 20:52:04', 'wechat', 1, '["artist"]');

-- Banner数据
INSERT INTO banner (id, title, image_url, type, target, sort_no, status, create_time) VALUES
(3, '111', 'http://192.168.1.109:8087/upload/images/2026/05/07/99b72aa5c284454884b486ef1bc0f0ea.png', 'OTHER', '1', 3, 'ENABLED', '2026-05-07T21:18:43'),
(2, '222', 'http://192.168.1.109:8087/upload/images/2026/05/07/e66aa06239d74a8da68209e6128dd264.png', 'OTHER', '1', 2, 'ENABLED', '2026-05-07T21:18:43'),
(1, '333', 'http://192.168.1.109:8087/upload/images/2026/05/07/f640378c6f32491f907e53ded4f8911c.png', 'OTHER', '1', 1, 'ENABLED', '2026-05-07T21:18:43');
