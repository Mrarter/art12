-- ============================================
-- 拾艺局·运营后台管理表
-- 版本: 1.1
-- 日期: 2026-04-24
-- 说明: 支持角色、菜单、管理员真实持久化
-- ============================================

USE shiyiju;

-- ============================================
-- 后台角色表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台角色表';

-- ============================================
-- 后台菜单表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_menu` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    `name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `path` VARCHAR(255) NOT NULL COMMENT '菜单路径',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID, 0为顶级',
    `level` INT DEFAULT 1 COMMENT '层级深度',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台菜单表';

-- ============================================
-- 后台角色-菜单关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_role_menu` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台角色菜单关联表';

-- ============================================
-- 后台管理员表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `role_id` BIGINT DEFAULT NULL COMMENT '关联角色ID',
    `role_code` VARCHAR(50) DEFAULT 'admin' COMMENT '角色代码',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_role_code` (`role_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员表';

-- ============================================
-- 管理员操作日志表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `admin_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `admin_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人名称',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作类型',
    `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
    `detail` TEXT COMMENT '操作详情',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'UserAgent',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- ============================================
-- 初始化菜单数据
-- ============================================
INSERT INTO `admin_menu` (`id`, `name`, `path`, `icon`, `sort`, `parent_id`, `level`) VALUES
-- 一级菜单
(1, '工作台', '/dashboard', 'DashboardOutlined', 1, 0, 1),
(2, '用户管理', '/user', 'TeamOutlined', 2, 0, 1),
(201, '用户列表', '/user/list', 'UserOutlined', 1, 2, 2),
(202, '艺术家管理', '/user/artist', 'StarOutlined', 2, 2, 2),
(203, '艺荐官管理', '/user/promoter', 'CrownOutlined', 3, 2, 2),
(3, '作品管理', '/product', 'PictureOutlined', 3, 0, 1),
(301, '作品列表', '/product/list', 'AppstoreOutlined', 1, 3, 2),
(302, '待审核', '/product/audit', 'SafetyOutlined', 2, 3, 2),
(303, '分类管理', '/product/category', 'TagsOutlined', 3, 3, 2),
(4, '订单管理', '/order', 'ShoppingCartOutlined', 4, 0, 1),
(401, '订单列表', '/order/list', 'FileTextOutlined', 1, 4, 2),
(402, '售后管理', '/order/aftersale', 'CustomerServiceOutlined', 2, 4, 2),
(5, '社区管理', '/community', 'CommentOutlined', 5, 0, 1),
(501, '帖子管理', '/community/post', 'MessageOutlined', 1, 5, 2),
(502, '评论管理', '/community/comment', 'MessageOutlined', 2, 5, 2),
(503, '话题管理', '/community/topic', 'TagOutlined', 3, 5, 2),
(504, '举报管理', '/community/report', 'WarningOutlined', 4, 5, 2),
(6, '拍卖管理', '/auction', 'GavelOutlined', 6, 0, 1),
(601, '专场列表', '/auction/session', 'FolderOutlined', 1, 6, 2),
(602, '拍品管理', '/auction/lot', 'DatabaseOutlined', 2, 6, 2),
(603, '拍卖统计', '/auction/stats', 'LineChartOutlined', 3, 6, 2),
(7, '分销管理', '/promotion', 'TrophyOutlined', 7, 0, 1),
(701, '佣金配置', '/promotion/commission', 'SettingOutlined', 1, 7, 2),
(702, '提现管理', '/promotion/withdraw', 'BankOutlined', 2, 7, 2),
(703, '分销统计', '/promotion/stats', 'LineChartOutlined', 3, 7, 2),
(8, '系统设置', '/system', 'SettingOutlined', 8, 0, 1),
(801, '管理员', '/system/admin', 'SafetyCertificateOutlined', 1, 8, 2),
(802, '角色权限', '/system/role', 'KeyOutlined', 2, 8, 2),
(803, '轮播图', '/system/banner', 'PictureOutlined', 3, 8, 2),
(804, '消息模板', '/system/message', 'MailOutlined', 4, 8, 2),
(805, '系统配置', '/system/config', 'ToolOutlined', 5, 8, 2),
(9, '日志管理', '/logs', 'FileTextOutlined', 9, 0, 1),
(901, '操作日志', '/logs/operation', 'HistoryOutlined', 1, 9, 2);

-- ============================================
-- 初始化角色数据
-- ============================================
INSERT INTO `admin_role` (`id`, `role_code`, `role_name`, `description`, `status`) VALUES
(1, 'super', '超级管理员', '拥有所有权限', 1),
(2, 'admin', '运营专员', '负责日常运营工作', 1),
(3, 'finance', '财务专员', '负责财务相关工作', 1),
(4, 'auditor', '内容审核员', '负责内容审核', 1);

-- ============================================
-- 初始化超级管理员菜单权限（拥有所有菜单）
-- ============================================
INSERT INTO `admin_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM admin_menu WHERE status = 1;

-- ============================================
-- 社区举报表
-- ============================================
CREATE TABLE IF NOT EXISTS `community_report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID',
    `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
    `reporter_nickname` VARCHAR(50) DEFAULT NULL COMMENT '举报人昵称',
    `reported_user_id` BIGINT NOT NULL COMMENT '被举报用户ID',
    `reported_nickname` VARCHAR(50) DEFAULT NULL COMMENT '被举报人昵称',
    `target_id` BIGINT NOT NULL COMMENT '目标ID(帖子/评论ID)',
    `target_type` VARCHAR(20) DEFAULT NULL COMMENT '目标类型: post/comment',
    `report_type` VARCHAR(50) NOT NULL COMMENT '举报类型: 涉黄/广告/人身攻击/其他',
    `content` TEXT COMMENT '举报内容描述',
    `images` JSON DEFAULT NULL COMMENT '证据图片',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-已处理',
    `handle_result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
    `handler_name` VARCHAR(50) DEFAULT NULL COMMENT '处理人姓名',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_reported_user_id` (`reported_user_id`),
    KEY `idx_target` (`target_id`, `target_type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区举报表';

-- ============================================
-- 初始化管理员账户（密码: admin123）
-- ============================================
INSERT INTO `admin_user` (`id`, `username`, `password`, `email`, `phone`, `role_id`, `role_code`, `status`) VALUES
(1, 'admin', 'admin123', 'admin@shiyiju.com', '13800000000', 1, 'super', 1);
