package com.shiyiju.user.service.support;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 本地旧库兼容初始化。
 * 当前 art12 的本地库以 sys_user 为主，用户服务仍依赖 users 表，
 * 因此在 users 不存在时自动补表，并尽量从 sys_user / user_account 回填基础数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        ensureUsersTable();
        ensureUsersColumns();
        ensureArtistProfileTable();
        ensureArtistCertificationsTable();
        ensureArtistProfileColumns();
        ensureRealnameTable();
        ensureRealnameColumns();
        ensureUserFollowsTable();
        ensurePayAccountTable();
        ensureWalletTables();
        ensureWithdrawRecordsTable();
        ensurePromoterRecordTable();
        ensureCommissionRecordTable();
        ensureResaleTables();
        backfillUsersFromLegacyTables();
    }

    private void ensureUsersTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS users (
              id BIGINT NOT NULL AUTO_INCREMENT,
              uid VARCHAR(19) DEFAULT NULL,
              openid VARCHAR(64) DEFAULT NULL,
              unionid VARCHAR(64) DEFAULT NULL,
              nickname VARCHAR(100) DEFAULT NULL,
              avatar VARCHAR(255) DEFAULT NULL,
              phone VARCHAR(20) DEFAULT NULL,
              email VARCHAR(128) DEFAULT NULL,
              wechat VARCHAR(64) DEFAULT NULL,
              password VARCHAR(255) DEFAULT NULL,
              gender INT DEFAULT 0,
              birthday VARCHAR(32) DEFAULT NULL,
              bio VARCHAR(500) DEFAULT NULL,
              region VARCHAR(128) DEFAULT NULL,
              identities VARCHAR(255) DEFAULT 'collector',
              status INT DEFAULT 1,
              follower_count INT DEFAULT 0,
              following_count INT DEFAULT 0,
              register_time DATETIME DEFAULT NULL,
              last_login_time DATETIME DEFAULT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              deleted TINYINT DEFAULT 0,
              PRIMARY KEY (id),
              UNIQUE KEY uk_users_uid (uid),
              UNIQUE KEY uk_users_openid (openid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureUsersColumns() {
        addColumnIfMissing("users", "password",
            "ALTER TABLE users ADD COLUMN password VARCHAR(255) DEFAULT NULL COMMENT '登录密码哈希' AFTER phone");
        addColumnIfMissing("users", "email",
            "ALTER TABLE users ADD COLUMN email VARCHAR(128) DEFAULT NULL COMMENT '邮箱' AFTER phone");
        addColumnIfMissing("users", "wechat",
            "ALTER TABLE users ADD COLUMN wechat VARCHAR(64) DEFAULT NULL COMMENT '微信号' AFTER email");
    }

    private void backfillUsersFromLegacyTables() {
        if (tableExists("sys_user")) {
            backfillFromSysUser();
            return;
        }
        if (tableExists("user_account")) {
            backfillFromUserAccount();
        }
    }

    private void backfillFromSysUser() {
        String userIdColumn = firstExistingColumn("sys_user", "user_id", "id");
        String uidColumn = firstExistingColumn("sys_user", "uid");
        String nicknameColumn = firstExistingColumn("sys_user", "nickname");
        String avatarColumn = firstExistingColumn("sys_user", "avatar", "avatar_url");
        String phoneColumn = firstExistingColumn("sys_user", "phone", "mobile");
        String emailColumn = firstExistingColumn("sys_user", "email", "mail");
        String wechatColumn = firstExistingColumn("sys_user", "wechat", "wechat_no", "wechat_id");
        String genderColumn = firstExistingColumn("sys_user", "gender");
        String birthdayColumn = firstExistingColumn("sys_user", "birthday");
        String bioColumn = firstExistingColumn("sys_user", "bio", "resume");
        String regionColumn = firstExistingColumn("sys_user", "region");
        String identitiesColumn = firstExistingColumn("sys_user", "identities", "identity_json", "identity");
        String statusColumn = firstExistingColumn("sys_user", "status");
        String createTimeColumn = firstExistingColumn("sys_user", "create_time", "register_time");
        String updateTimeColumn = firstExistingColumn("sys_user", "update_time");

        String uidExpr = uidColumn != null
            ? "COALESCE(NULLIF(s." + uidColumn + ", ''), CONCAT('USR', DATE_FORMAT(COALESCE(s." + createTimeColumn + ", NOW()), '%Y%m%d'), LPAD(s." + userIdColumn + ", 8, '0')))"
            : "CONCAT('USR', DATE_FORMAT(COALESCE(s." + createTimeColumn + ", NOW()), '%Y%m%d'), LPAD(s." + userIdColumn + ", 8, '0'))";
        String identitiesExpr = identitiesColumn != null
            ? "COALESCE(NULLIF(REPLACE(REPLACE(REPLACE(CAST(s." + identitiesColumn + " AS CHAR), '[', ''), ']', ''), '\"', ''), ''), 'collector')"
            : "'collector'";

        String sql = """
            INSERT INTO users (
              uid, openid, unionid, nickname, avatar, phone, email, wechat, gender, birthday, bio, region,
              identities, status, follower_count, following_count, register_time, last_login_time,
              create_time, update_time, deleted
            )
            SELECT
              %s,
              NULL,
              NULL,
              %s,
              %s,
              %s,
              %s,
              %s,
              %s,
              %s,
              %s,
              %s,
              %s,
              %s,
              0,
              0,
              %s,
              %s,
              %s,
              %s,
              0
            FROM sys_user s
            WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.uid = %s)
            """.formatted(
            uidExpr,
            columnExpr("s", nicknameColumn, "CONCAT('用户', s." + userIdColumn + ")"),
            columnExpr("s", avatarColumn, "NULL"),
            columnExpr("s", phoneColumn, "NULL"),
            columnExpr("s", emailColumn, "NULL"),
            columnExpr("s", wechatColumn, "NULL"),
            columnExpr("s", genderColumn, "0"),
            columnExpr("s", birthdayColumn, "NULL"),
            columnExpr("s", bioColumn, "NULL"),
            columnExpr("s", regionColumn, "NULL"),
            identitiesExpr,
            columnExpr("s", statusColumn, "1"),
            columnExpr("s", createTimeColumn, "NOW()"),
            columnExpr("s", updateTimeColumn, columnExpr("s", createTimeColumn, "NOW()")),
            columnExpr("s", createTimeColumn, "NOW()"),
            columnExpr("s", updateTimeColumn, columnExpr("s", createTimeColumn, "NOW()")),
            uidExpr
        );
        int inserted = jdbcTemplate.update(sql);
        if (inserted > 0) {
            log.info("从 sys_user 回填 users 成功: {} 条", inserted);
        }
    }

    private void backfillFromUserAccount() {
        String userIdColumn = firstExistingColumn("user_account", "id", "user_id");
        String uidColumn = firstExistingColumn("user_account", "user_uid", "uid", "user_no");
        String nicknameColumn = firstExistingColumn("user_account", "nickname", "name");
        String avatarColumn = firstExistingColumn("user_account", "avatar", "avatar_url");
        String phoneColumn = firstExistingColumn("user_account", "phone", "mobile");
        String emailColumn = firstExistingColumn("user_account", "email", "mail");
        String wechatColumn = firstExistingColumn("user_account", "wechat", "wechat_no", "wechat_id");
        String createTimeColumn = firstExistingColumn("user_account", "create_time", "created_at");
        String updateTimeColumn = firstExistingColumn("user_account", "update_time", "updated_at");
        String statusColumn = firstExistingColumn("user_account", "status");
        String openidColumn = firstExistingColumn("user_account", "openid");

        String uidExpr = uidColumn != null
            ? "COALESCE(NULLIF(a." + uidColumn + ", ''), CONCAT('USR', DATE_FORMAT(COALESCE(a." + createTimeColumn + ", NOW()), '%Y%m%d'), LPAD(a." + userIdColumn + ", 8, '0')))"
            : "CONCAT('USR', DATE_FORMAT(COALESCE(a." + createTimeColumn + ", NOW()), '%Y%m%d'), LPAD(a." + userIdColumn + ", 8, '0'))";
        String sql = """
            INSERT INTO users (
              uid, openid, unionid, nickname, avatar, phone, email, wechat, gender, birthday, bio, region,
              identities, status, follower_count, following_count, register_time, last_login_time,
              create_time, update_time, deleted
            )
            SELECT
              %s,
              %s,
              NULL,
              %s,
              %s,
              %s,
              %s,
              %s,
              0,
              NULL,
              NULL,
              NULL,
              'collector',
              %s,
              0,
              0,
              %s,
              %s,
              %s,
              %s,
              0
            FROM user_account a
            WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.uid = %s)
            """.formatted(
            uidExpr,
            columnExpr("a", openidColumn, "NULL"),
            columnExpr("a", nicknameColumn, "CONCAT('用户', a." + userIdColumn + ")"),
            columnExpr("a", avatarColumn, "NULL"),
            columnExpr("a", phoneColumn, "NULL"),
            columnExpr("a", emailColumn, "NULL"),
            columnExpr("a", wechatColumn, "NULL"),
            columnExpr("a", statusColumn, "1"),
            columnExpr("a", createTimeColumn, "NOW()"),
            columnExpr("a", updateTimeColumn, columnExpr("a", createTimeColumn, "NOW()")),
            columnExpr("a", createTimeColumn, "NOW()"),
            columnExpr("a", updateTimeColumn, columnExpr("a", createTimeColumn, "NOW()")),
            uidExpr
        );
        int inserted = jdbcTemplate.update(sql);
        if (inserted > 0) {
            log.info("从 user_account 回填 users 成功: {} 条", inserted);
        }
    }

    private void ensureArtistProfileColumns() {
        if (!tableExists("artist_profile")) {
            return;
        }
        addColumnIfMissing("artist_profile", "artist_title", "ALTER TABLE artist_profile ADD COLUMN artist_title VARCHAR(128) DEFAULT NULL");
        addColumnIfMissing("artist_profile", "homepage_cover", "ALTER TABLE artist_profile ADD COLUMN homepage_cover VARCHAR(512) DEFAULT NULL");
        addColumnIfMissing("artist_profile", "artist_tags", "ALTER TABLE artist_profile ADD COLUMN artist_tags VARCHAR(255) DEFAULT NULL");
        addColumnIfMissing("artist_profile", "homepage_style", "ALTER TABLE artist_profile ADD COLUMN homepage_style VARCHAR(16) DEFAULT '2'");
        addColumnIfMissing("artist_profile", "resume", "ALTER TABLE artist_profile ADD COLUMN resume TEXT DEFAULT NULL COMMENT '结构化艺术履历'");
    }

    private void ensureArtistProfileTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS artist_profile (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT DEFAULT NULL,
              user_uid VARCHAR(32) DEFAULT NULL,
              real_name VARCHAR(100) DEFAULT NULL,
              artist_name VARCHAR(100) DEFAULT NULL,
              id_card VARCHAR(32) DEFAULT NULL,
              bio TEXT DEFAULT NULL,
              resume TEXT DEFAULT NULL,
              status INT DEFAULT 0,
              artist_level VARCHAR(32) DEFAULT NULL,
              artist_code VARCHAR(32) DEFAULT NULL,
              reject_reason VARCHAR(500) DEFAULT NULL,
              review_time DATETIME DEFAULT NULL,
              artist_title VARCHAR(128) DEFAULT NULL,
              homepage_cover VARCHAR(512) DEFAULT NULL,
              artist_tags VARCHAR(255) DEFAULT NULL,
              homepage_style VARCHAR(16) DEFAULT '2',
              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_artist_profile_user_id (user_id),
              KEY idx_artist_profile_user_uid (user_uid),
              KEY idx_artist_profile_artist_code (artist_code),
              KEY idx_artist_profile_real_name (real_name),
              KEY idx_artist_profile_artist_name (artist_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureArtistCertificationsTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS artist_certifications (
              id BIGINT NOT NULL AUTO_INCREMENT,
              artist_code VARCHAR(32) DEFAULT NULL,
              user_id BIGINT DEFAULT NULL,
              real_name VARCHAR(100) DEFAULT NULL,
              id_card VARCHAR(32) DEFAULT NULL,
              resume TEXT DEFAULT NULL,
              artworks TEXT DEFAULT NULL,
              exhibits TEXT DEFAULT NULL,
              status INT DEFAULT 0,
              reject_reason VARCHAR(500) DEFAULT NULL,
              review_time DATETIME DEFAULT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_artist_certifications_artist_code (artist_code),
              KEY idx_artist_certifications_user_id (user_id),
              KEY idx_artist_certifications_real_name (real_name),
              KEY idx_artist_certifications_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureRealnameTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS realname_certifications (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL COMMENT '用户ID',
              real_name VARCHAR(100) DEFAULT NULL COMMENT '真实姓名',
              id_card VARCHAR(64) DEFAULT NULL COMMENT '身份证号（脱敏存储）',
              id_card_hash VARCHAR(128) DEFAULT NULL COMMENT '身份证号SHA256（查重用）',
              id_front_url VARCHAR(512) DEFAULT NULL COMMENT '身份证正面照URL',
              id_back_url VARCHAR(512) DEFAULT NULL COMMENT '身份证背面照URL',
              face_verified TINYINT DEFAULT 0 COMMENT '人脸核验状态',
              verify_channel VARCHAR(32) DEFAULT 'manual' COMMENT '实名认证渠道：manual/alipay',
              certify_id VARCHAR(128) DEFAULT NULL COMMENT '支付宝实名认证流水号',
              external_status VARCHAR(64) DEFAULT NULL COMMENT '外部实名认证状态',
              status INT DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
              reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
              review_time DATETIME DEFAULT NULL COMMENT '审核时间',
              reviewer_id BIGINT DEFAULT NULL COMMENT '审核人ID',
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_realname_user_id (user_id),
              KEY idx_realname_status (status),
              UNIQUE KEY uk_realname_user_id (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureUserFollowsTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_follows (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL COMMENT '关注者用户ID',
              follow_user_id BIGINT NOT NULL COMMENT '被关注艺术家用户ID',
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              deleted TINYINT DEFAULT 0,
              PRIMARY KEY (id),
              UNIQUE KEY uk_user_follow (user_id, follow_user_id),
              KEY idx_user_follows_user_id (user_id),
              KEY idx_user_follows_follow_user_id (follow_user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureCommissionRecordTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS commission_record (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL COMMENT '佣金接收人用户ID',
              source_user_id BIGINT DEFAULT NULL COMMENT '来源用户ID（购买者/推广人）',
              order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
              artwork_id BIGINT DEFAULT NULL COMMENT '关联作品ID',
              commission_type VARCHAR(32) NOT NULL COMMENT '类型：artwork_sale/promoter_reward/resale_reward/team_reward',
              commission_level INT DEFAULT 1 COMMENT '佣金层级：1-一级 2-二级',
              rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '佣金比例(%)',
              amount DECIMAL(12,2) NOT NULL COMMENT '佣金金额',
              status VARCHAR(16) DEFAULT 'pending' COMMENT '状态：pending/settled/freeze/cancel',
              remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_commission_user_id (user_id),
              KEY idx_commission_order (order_id),
              KEY idx_commission_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensurePromoterRecordTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS promoter_record (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL COMMENT '用户ID',
              invite_code VARCHAR(20) NOT NULL COMMENT '邀请码',
              parent_id BIGINT DEFAULT NULL COMMENT '上级艺荐官用户ID',
              level TINYINT DEFAULT 1 COMMENT '等级: 1-普通, 2-白银, 3-黄金, 4-钻石',
              team_size INT DEFAULT 0 COMMENT '团队人数',
              total_orders INT DEFAULT 0 COMMENT '累计订单数',
              total_sales DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计销售额',
              status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
              sign_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '签约时间',
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_promoter_user_id (user_id),
              UNIQUE KEY uk_promoter_invite_code (invite_code),
              KEY idx_promoter_parent_id (parent_id),
              KEY idx_promoter_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureResaleTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS resale_record (
              id BIGINT NOT NULL AUTO_INCREMENT,
              artwork_id BIGINT NOT NULL COMMENT '作品ID',
              seller_user_id BIGINT NOT NULL COMMENT '转售卖家',
              buyer_user_id BIGINT DEFAULT NULL COMMENT '转售买家',
              source_order_id BIGINT DEFAULT NULL COMMENT '来源订单ID',
              resale_price DECIMAL(12,2) NOT NULL COMMENT '转售价格',
              artist_income DECIMAL(12,2) DEFAULT 0.00 COMMENT '艺术家追续收益',
              platform_fee DECIMAL(12,2) DEFAULT 0.00 COMMENT '平台服务费',
              seller_income DECIMAL(12,2) DEFAULT 0.00 COMMENT '卖家收入',
              status VARCHAR(32) DEFAULT 'pending' COMMENT '状态：pending/paid/completed/cancel',
              trade_no VARCHAR(64) DEFAULT NULL COMMENT '转售交易编号',
              version INT DEFAULT 0 COMMENT '乐观锁版本',
              remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_resale_artwork_status (artwork_id, status),
              KEY idx_resale_seller_status (seller_user_id, status),
              KEY idx_resale_buyer_status (buyer_user_id, status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS artwork_trade_record (
              id BIGINT NOT NULL AUTO_INCREMENT,
              artwork_id BIGINT NOT NULL COMMENT '作品ID',
              trade_no VARCHAR(64) DEFAULT NULL COMMENT '交易编号',
              seller_user_id BIGINT DEFAULT NULL COMMENT '卖家用户ID',
              buyer_user_id BIGINT NOT NULL COMMENT '买家用户ID',
              trade_price DECIMAL(12,2) NOT NULL COMMENT '成交价格',
              trade_type VARCHAR(32) NOT NULL COMMENT '交易类型：first_sale/resale',
              trade_round INT DEFAULT 1 COMMENT '交易轮次',
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_trade_artwork (artwork_id),
              KEY idx_trade_buyer (buyer_user_id),
              KEY idx_trade_seller (seller_user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS artwork_price_history (
              id BIGINT NOT NULL AUTO_INCREMENT,
              artwork_id BIGINT NOT NULL COMMENT '作品ID',
              before_price DECIMAL(12,2) DEFAULT NULL COMMENT '变动前价格',
              after_price DECIMAL(12,2) NOT NULL COMMENT '变动后价格',
              growth_rate DECIMAL(10,4) DEFAULT 0.0000 COMMENT '涨幅百分比',
              reason VARCHAR(32) NOT NULL COMMENT '变动原因',
              related_resale_id BIGINT DEFAULT NULL COMMENT '关联转售ID',
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_price_history_artwork (artwork_id),
              KEY idx_price_history_resale (related_resale_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensurePayAccountTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS pay_account (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL COMMENT '用户ID',
              account_type TINYINT NOT NULL COMMENT '账户类型：1-微信 2-支付宝 3-银行卡',
              real_name VARCHAR(100) DEFAULT NULL COMMENT '收款人姓名',
              id_card VARCHAR(64) DEFAULT NULL COMMENT '身份证号（脱敏）',
              phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
              bank_name VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
              bank_card VARCHAR(256) DEFAULT NULL COMMENT '银行卡号（AES加密）',
              alipay_account VARCHAR(100) DEFAULT NULL COMMENT '支付宝账号',
              wechat_openid VARCHAR(64) DEFAULT NULL COMMENT '微信OpenId',
              is_default TINYINT DEFAULT 0 COMMENT '是否默认：0-否 1-是',
              verify_status TINYINT DEFAULT 0 COMMENT '实名认证状态：0-未认证 1-已认证',
              status TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_pay_account_user_id (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureWalletTables() {
        // 用户钱包表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_wallet (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL COMMENT '用户ID',
              balance DECIMAL(12,2) DEFAULT 0.00 COMMENT '可用余额',
              freeze_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '冻结金额',
              pending_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '待结算金额',
              deposit_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '保证金',
              total_income DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计收入',
              total_withdraw DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计提现',
              version INT DEFAULT 0 COMMENT '乐观锁',
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_wallet_user_id (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        // 钱包流水表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS wallet_bill (
              id BIGINT NOT NULL AUTO_INCREMENT,
              user_id BIGINT NOT NULL COMMENT '用户ID',
              bill_type VARCHAR(32) NOT NULL COMMENT '流水类型：income/withdraw/freeze/unfreeze/commission/resale/refund/deposit',
              amount DECIMAL(12,2) NOT NULL COMMENT '变动金额',
              before_balance DECIMAL(12,2) NOT NULL COMMENT '变动前余额',
              after_balance DECIMAL(12,2) NOT NULL COMMENT '变动后余额',
              related_id BIGINT DEFAULT NULL COMMENT '关联业务ID',
              related_type VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型：order/commission/resale/withdraw/deposit',
              remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_bill_user_id (user_id),
              KEY idx_bill_related (related_type, related_id),
              UNIQUE KEY uk_bill_biz (bill_type, related_type, related_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureWithdrawRecordsTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS withdraw_records (
              id BIGINT NOT NULL AUTO_INCREMENT,
              promoter_id BIGINT NOT NULL COMMENT '艺荐官记录ID',
              amount BIGINT NOT NULL COMMENT '提现金额',
              fee_amount BIGINT DEFAULT 0 COMMENT '手续费',
              actual_amount BIGINT NOT NULL COMMENT '实际到账金额',
              account_type VARCHAR(20) NOT NULL COMMENT '账户类型：bank/wechat/alipay',
              account_info VARCHAR(200) DEFAULT NULL COMMENT '账户信息',
              account_name VARCHAR(50) DEFAULT NULL COMMENT '账户姓名',
              status TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已通过，2-已拒绝，3-已打款',
              reject_reason VARCHAR(200) DEFAULT NULL,
              process_time DATETIME DEFAULT NULL,
              transfer_time DATETIME DEFAULT NULL,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_withdraw_promoter_id (promoter_id),
              KEY idx_withdraw_status (status),
              KEY idx_withdraw_create_time (create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void ensureRealnameColumns() {
        addColumnIfMissing("users", "real_name_verified",
            "ALTER TABLE users ADD COLUMN real_name_verified TINYINT DEFAULT 0 COMMENT '实名认证状态：0-未认证，1-已认证'");
        addColumnIfMissing("realname_certifications", "verify_channel",
            "ALTER TABLE realname_certifications ADD COLUMN verify_channel VARCHAR(32) DEFAULT 'manual' COMMENT '实名认证渠道：manual/alipay' AFTER face_verified");
        addColumnIfMissing("realname_certifications", "certify_id",
            "ALTER TABLE realname_certifications ADD COLUMN certify_id VARCHAR(128) DEFAULT NULL COMMENT '支付宝实名认证流水号' AFTER verify_channel");
        addColumnIfMissing("realname_certifications", "external_status",
            "ALTER TABLE realname_certifications ADD COLUMN external_status VARCHAR(64) DEFAULT NULL COMMENT '外部实名认证状态' AFTER certify_id");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private String firstExistingColumn(String tableName, String... candidates) {
        for (String candidate : candidates) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                candidate
            );
            if (count != null && count > 0) {
                return candidate;
            }
        }
        return null;
    }

    private String columnExpr(String alias, String column, String fallback) {
        return column == null ? fallback : alias + "." + column;
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (firstExistingColumn(tableName, columnName) == null) {
            jdbcTemplate.execute(ddl);
        }
    }
}
