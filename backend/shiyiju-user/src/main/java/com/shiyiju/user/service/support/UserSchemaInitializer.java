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
        ensureArtistProfileTable();
        ensureArtistCertificationsTable();
        ensureArtistProfileColumns();
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
              uid, openid, unionid, nickname, avatar, phone, gender, birthday, bio, region,
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
        String createTimeColumn = firstExistingColumn("user_account", "create_time", "created_at");
        String updateTimeColumn = firstExistingColumn("user_account", "update_time", "updated_at");
        String statusColumn = firstExistingColumn("user_account", "status");
        String openidColumn = firstExistingColumn("user_account", "openid");

        String uidExpr = uidColumn != null
            ? "COALESCE(NULLIF(a." + uidColumn + ", ''), CONCAT('USR', DATE_FORMAT(COALESCE(a." + createTimeColumn + ", NOW()), '%Y%m%d'), LPAD(a." + userIdColumn + ", 8, '0')))"
            : "CONCAT('USR', DATE_FORMAT(COALESCE(a." + createTimeColumn + ", NOW()), '%Y%m%d'), LPAD(a." + userIdColumn + ", 8, '0'))";
        String sql = """
            INSERT INTO users (
              uid, openid, unionid, nickname, avatar, phone, gender, birthday, bio, region,
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
