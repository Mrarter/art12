package com.shiyiju.admin.service.support;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures admin-only persistence tables exist so the backend does not fall back
 * to in-memory mock data after a restart.
 */
@Component
public class AdminSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public AdminSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createAdminRoleTable();
        createAdminUserTable();
        createAdminRoleMenuTable();
        createMessageTemplateTable();
        createMessageRecordTable();
        seedRoles();
        seedSuperAdmin();
        seedMessageTemplates();
    }

    private void createAdminRoleTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS admin_role (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                role_code VARCHAR(50) NOT NULL,
                role_name VARCHAR(50) NOT NULL,
                description VARCHAR(255) DEFAULT NULL,
                status TINYINT DEFAULT 1,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_role_code (role_code)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void createAdminUserTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS admin_user (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(64) NOT NULL,
                password VARCHAR(128) NOT NULL,
                email VARCHAR(100) DEFAULT NULL,
                phone VARCHAR(20) DEFAULT NULL,
                avatar VARCHAR(255) DEFAULT NULL,
                role_id BIGINT DEFAULT NULL,
                role_code VARCHAR(50) DEFAULT 'admin',
                status TINYINT DEFAULT 1,
                last_login_time DATETIME DEFAULT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void createAdminRoleMenuTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS admin_role_menu (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                role_id BIGINT NOT NULL,
                menu_id BIGINT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_role_menu (role_id, menu_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void createMessageTemplateTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS admin_message_template (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                type VARCHAR(50) NOT NULL,
                title VARCHAR(200) NOT NULL,
                content TEXT NOT NULL,
                status TINYINT DEFAULT 1,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void createMessageRecordTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS admin_message_record (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                type VARCHAR(50) NOT NULL,
                title VARCHAR(200) NOT NULL,
                content TEXT NOT NULL,
                target_type VARCHAR(20) DEFAULT 'all',
                target_name VARCHAR(100) DEFAULT NULL,
                group_name VARCHAR(100) DEFAULT NULL,
                phone VARCHAR(20) DEFAULT NULL,
                push_enabled TINYINT DEFAULT 1,
                sms_enabled TINYINT DEFAULT 0,
                email_enabled TINYINT DEFAULT 0,
                status VARCHAR(20) DEFAULT 'sent',
                success_count INT DEFAULT 0,
                fail_count INT DEFAULT 0,
                scheduled_time DATETIME DEFAULT NULL,
                sent_at DATETIME DEFAULT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void seedRoles() {
        jdbcTemplate.update("""
            INSERT INTO admin_role (role_code, role_name, description, status)
            VALUES ('super', '超级管理员', '拥有全部后台权限', 1)
            ON DUPLICATE KEY UPDATE
                role_name = VALUES(role_name),
                description = VALUES(description),
                status = VALUES(status)
            """);
        jdbcTemplate.update("""
            INSERT INTO admin_role (role_code, role_name, description, status)
            VALUES ('admin', '普通管理员', '负责日常运营与维护', 1)
            ON DUPLICATE KEY UPDATE
                role_name = VALUES(role_name),
                description = VALUES(description),
                status = VALUES(status)
            """);
    }

    private void seedSuperAdmin() {
        jdbcTemplate.update("""
            INSERT INTO admin_user (username, password, email, phone, role_code, status)
            VALUES ('admin', 'admin123', 'admin@shiyiju.com', '13800000000', 'super', 1)
            ON DUPLICATE KEY UPDATE
                email = COALESCE(admin_user.email, VALUES(email)),
                phone = COALESCE(admin_user.phone, VALUES(phone)),
                role_code = admin_user.role_code
            """);
    }

    private void seedMessageTemplates() {
        jdbcTemplate.update("""
            INSERT INTO admin_message_template (name, type, title, content, status)
            SELECT '订单支付成功', 'order', '订单支付成功通知', '尊敬的{username}，您的订单{orderNo}已支付成功，金额{amount}元', 1
            WHERE NOT EXISTS (
                SELECT 1 FROM admin_message_template WHERE name = '订单支付成功'
            )
            """);
        jdbcTemplate.update("""
            INSERT INTO admin_message_template (name, type, title, content, status)
            SELECT '艺术家认证结果', 'audit', '认证审核结果', '{username}，您的艺术家认证申请已{result}，{reason}', 1
            WHERE NOT EXISTS (
                SELECT 1 FROM admin_message_template WHERE name = '艺术家认证结果'
            )
            """);
    }
}
