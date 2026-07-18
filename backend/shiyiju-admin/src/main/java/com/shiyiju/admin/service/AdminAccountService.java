package com.shiyiju.admin.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminAccountService {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listAdmins() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, username, email, phone, role_code, status, avatar, last_login_time, create_time
            FROM admin_user
            ORDER BY id DESC
            """);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", toLong(row.get("id")));
            item.put("username", row.get("username"));
            item.put("email", row.get("email"));
            item.put("phone", row.get("phone"));
            item.put("avatar", row.get("avatar"));
            item.put("role", normalizeRole(row.get("role_code")));
            item.put("status", toInt(row.get("status"), 1));
            item.put("lastLoginTime", row.get("last_login_time"));
            item.put("createTime", row.get("create_time"));
            result.add(item);
        }
        return result;
    }

    public Long createAdmin(Map<String, Object> params) {
        String username = Objects.toString(params.get("username"), "").trim();
        String password = Objects.toString(params.get("password"), "").trim();
        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        String role = normalizeRole(params.get("role"));
        jdbcTemplate.update("""
            INSERT INTO admin_user (username, password, email, phone, role_code, status, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            username,
            passwordEncoder.encode(password),
            nullableText(params.get("email")),
            nullableText(params.get("phone")),
            role,
            toInt(params.get("status"), 1),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateAdmin(Long id, Map<String, Object> params) {
        jdbcTemplate.update("""
            UPDATE admin_user
            SET email = ?, phone = ?, role_code = ?, status = ?, update_time = ?
            WHERE id = ?
            """,
            nullableText(params.get("email")),
            nullableText(params.get("phone")),
            normalizeRole(params.get("role")),
            toInt(params.get("status"), 1),
            LocalDateTime.now(),
            id
        );
    }

    public void deleteAdmin(Long id) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_user WHERE id = ? AND role_code = 'super'",
            Integer.class,
            id
        );
        if (count != null && count > 0) {
            throw new IllegalArgumentException("不能删除超级管理员");
        }
        jdbcTemplate.update("DELETE FROM admin_user WHERE id = ?", id);
    }

    public Map<String, Object> login(String username, String password) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, username, password, role_code, status
            FROM admin_user
            WHERE username = ?
            LIMIT 1
            """, username);

        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        if (toInt(row.get("status"), 0) != 1) {
            return null;
        }
        String storedPassword = Objects.toString(row.get("password"), "");
        boolean bcrypt = storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$");
        boolean passwordMatches = bcrypt
            ? passwordEncoder.matches(password, storedPassword)
            : MessageDigest.isEqual(storedPassword.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
        if (!passwordMatches) return null;

        Long adminId = toLong(row.get("id"));
        if (!bcrypt) {
            jdbcTemplate.update("UPDATE admin_user SET password = ? WHERE id = ?", passwordEncoder.encode(password), adminId);
        }
        jdbcTemplate.update("UPDATE admin_user SET last_login_time = ?, update_time = ? WHERE id = ?",
            LocalDateTime.now(), LocalDateTime.now(), adminId);
        jdbcTemplate.update("DELETE FROM admin_session WHERE expires_at < ? OR revoked = 1", LocalDateTime.now());
        String token = generateToken();
        jdbcTemplate.update("""
            INSERT INTO admin_session (admin_id, token_hash, expires_at, revoked, create_time, last_access_time)
            VALUES (?, ?, ?, 0, ?, ?)
            """, adminId, hashToken(token), LocalDateTime.now().plusHours(24), LocalDateTime.now(), LocalDateTime.now());

        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("id", adminId);
        admin.put("username", row.get("username"));
        admin.put("role", normalizeRole(row.get("role_code")));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("admin", admin);
        return data;
    }

    public Map<String, Object> getAdminInfo(String authorizationHeader) {
        Long adminId = resolveAdminId(authorizationHeader, false);
        if (adminId == null) return null;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, username, role_code
            FROM admin_user
            WHERE id = ?
            LIMIT 1
            """, adminId);
        if (rows.isEmpty()) return null;
        Map<String, Object> row = rows.get(0);
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("id", toLong(row.get("id")));
        admin.put("username", row.get("username"));
        admin.put("role", normalizeRole(row.get("role_code")));
        return admin;
    }

    public boolean isValidSession(String authorizationHeader) {
        return resolveAdminId(authorizationHeader, true) != null;
    }

    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token != null) jdbcTemplate.update("UPDATE admin_session SET revoked=1 WHERE token_hash=?", hashToken(token));
    }

    private Long resolveAdminId(String authorizationHeader, boolean touch) {
        String token = extractToken(authorizationHeader);
        if (token == null) return null;
        List<Long> ids = jdbcTemplate.query("""
            SELECT s.admin_id FROM admin_session s
            JOIN admin_user a ON a.id=s.admin_id
            WHERE s.token_hash=? AND s.revoked=0 AND s.expires_at>? AND a.status=1
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong(1), hashToken(token), LocalDateTime.now());
        if (ids.isEmpty()) return null;
        if (touch) jdbcTemplate.update("UPDATE admin_session SET last_access_time=? WHERE token_hash=?", LocalDateTime.now(), hashToken(token));
        return ids.get(0);
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String token = authorizationHeader.startsWith("Bearer ")
            ? authorizationHeader.substring(7).trim() : authorizationHeader.trim();
        return token.length() >= 32 ? token : null;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String normalizeRole(Object role) {
        String value = Objects.toString(role, "").trim();
        return switch (value) {
            case "super", "admin", "operation", "finance", "audit" -> value;
            default -> "admin";
        };
    }

    private String nullableText(Object value) {
        String text = Objects.toString(value, "").trim();
        return text.isEmpty() ? null : text;
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(Objects.toString(value, "0"));
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
