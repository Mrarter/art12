package com.shiyiju.admin.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminAccountService {

    private final JdbcTemplate jdbcTemplate;

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
            item.put("role", Objects.equals(row.get("role_code"), "super") ? "super" : "admin");
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
            password,
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
            SELECT id, username, role_code, status
            FROM admin_user
            WHERE username = ? AND password = ?
            LIMIT 1
            """, username, password);

        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        if (toInt(row.get("status"), 0) != 1) {
            return null;
        }

        Long adminId = toLong(row.get("id"));
        jdbcTemplate.update("UPDATE admin_user SET last_login_time = ?, update_time = ? WHERE id = ?",
            LocalDateTime.now(), LocalDateTime.now(), adminId);

        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("id", adminId);
        admin.put("username", row.get("username"));
        admin.put("role", normalizeRole(row.get("role_code")));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", "admin:" + adminId + ":" + System.currentTimeMillis());
        data.put("admin", admin);
        return data;
    }

    public Map<String, Object> getAdminInfo(String authorizationHeader) {
        Long adminId = parseAdminId(authorizationHeader);
        if (adminId == null) {
            adminId = 1L;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, username, role_code
            FROM admin_user
            WHERE id = ?
            LIMIT 1
            """, adminId);
        if (rows.isEmpty()) {
            return Map.of("id", 1, "username", "admin", "role", "super");
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("id", toLong(row.get("id")));
        admin.put("username", row.get("username"));
        admin.put("role", normalizeRole(row.get("role_code")));
        return admin;
    }

    private Long parseAdminId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String token = authorizationHeader.replace("Bearer ", "").trim();
        String[] parts = token.split(":");
        if (parts.length < 2 || !"admin".equals(parts[0])) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeRole(Object role) {
        return "super".equals(Objects.toString(role, "")) ? "super" : "admin";
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
