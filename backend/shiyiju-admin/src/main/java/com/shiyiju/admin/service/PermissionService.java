package com.shiyiju.admin.service;

import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 权限管理服务 - 真实持久化（使用 JdbcTemplate）
 */
@Service
public class PermissionService {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 角色管理 ====================

    public List<Map<String, Object>> getRoles() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, role_code, role_name, description, status, create_time, update_time FROM admin_role ORDER BY create_time DESC");
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("name", row.get("role_name"));
            item.put("code", row.get("role_code"));
            item.put("description", row.get("description"));
            item.put("status", row.get("status"));
            item.put("createTime", row.get("create_time"));
            item.put("updateTime", row.get("update_time"));
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getRoleById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM admin_role WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        return convertRole(rows.get(0));
    }

    @Transactional
    public Long createRole(Map<String, Object> params) {
        String name = (String) params.get("name");
        String code = (String) params.get("code");
        String description = params.get("description") != null ? (String) params.get("description") : "";
        
        jdbcTemplate.update(
            "INSERT INTO admin_role (role_name, role_code, description, status, create_time) VALUES (?, ?, ?, 1, ?)",
            name, code, description, LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void updateRole(Long id, Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("UPDATE admin_role SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        
        if (params.containsKey("name")) {
            sql.append(", role_name = ?");
            args.add(params.get("name"));
        }
        if (params.containsKey("description")) {
            sql.append(", description = ?");
            args.add(params.get("description"));
        }
        if (params.containsKey("status")) {
            sql.append(", status = ?");
            args.add(params.get("status"));
        }
        
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    @Transactional
    public boolean deleteRole(Long id) {
        if (id == 1L) return false;
        jdbcTemplate.update("DELETE FROM admin_role_menu WHERE role_id = ?", id);
        int rows = jdbcTemplate.update("DELETE FROM admin_role WHERE id = ?", id);
        return rows > 0;
    }

    public List<Long> getRoleMenuIds(Long roleId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT menu_id FROM admin_role_menu WHERE role_id = ?", roleId);
        List<Long> menuIds = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            menuIds.add((Long) row.get("menu_id"));
        }
        return menuIds;
    }

    @Transactional
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        jdbcTemplate.update("DELETE FROM admin_role_menu WHERE role_id = ?", roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                jdbcTemplate.update(
                    "INSERT INTO admin_role_menu (role_id, menu_id) VALUES (?, ?)",
                    roleId, menuId);
            }
        }
    }

    // ==================== 菜单管理 ====================

    public List<Map<String, Object>> getMenus() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, name, path, icon, sort, parent_id, level, status, create_time FROM admin_menu ORDER BY sort ASC");
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("name", row.get("name"));
            item.put("path", row.get("path"));
            item.put("icon", row.get("icon"));
            item.put("sort", row.get("sort"));
            item.put("parentId", row.get("parent_id"));
            item.put("level", row.get("level"));
            item.put("status", row.get("status"));
            item.put("createTime", row.get("create_time"));
            result.add(item);
        }
        return result;
    }

    public int getRoleUserCount(Long roleId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_user WHERE role_id = ?", Integer.class, roleId);
        return count != null ? count : 0;
    }

    // ==================== 管理员管理 ====================

    public PageResult<Map<String, Object>> getAdmins(int page, int size) {
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_user", Long.class);

        List<Object> args = new ArrayList<>();
        args.add((page - 1) * size);
        args.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT u.id, u.username, u.nickname, u.email, u.phone, u.role_id, u.status,
                   u.last_login_time, u.create_time, r.role_name
            FROM admin_user u
            LEFT JOIN admin_role r ON u.role_id = r.id
            ORDER BY u.create_time DESC LIMIT ?, ?
            """, args.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("username", row.get("username"));
            item.put("nickname", row.get("nickname"));
            item.put("email", row.get("email"));
            item.put("phone", row.get("phone"));
            item.put("roleId", row.get("role_id"));
            item.put("roleName", row.get("role_name"));
            item.put("status", row.get("status"));
            item.put("lastLoginTime", row.get("last_login_time"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public Long createAdmin(Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = passwordEncoder.encode((String) params.get("password"));
        String nickname = params.get("nickname") != null ? (String) params.get("nickname") : username;
        String email = params.get("email") != null ? (String) params.get("email") : "";
        String phone = params.get("phone") != null ? (String) params.get("phone") : "";
        Long roleId = params.get("roleId") != null ? Long.parseLong(params.get("roleId").toString()) : null;
        
        jdbcTemplate.update(
            "INSERT INTO admin_user (username, password, nickname, email, phone, role_id, status, create_time) VALUES (?, ?, ?, ?, ?, ?, 1, ?)",
            username, password, nickname, email, phone, roleId, LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void updateAdmin(Long id, Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("UPDATE admin_user SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        
        if (params.containsKey("nickname")) {
            sql.append(", nickname = ?");
            args.add(params.get("nickname"));
        }
        if (params.containsKey("email")) {
            sql.append(", email = ?");
            args.add(params.get("email"));
        }
        if (params.containsKey("phone")) {
            sql.append(", phone = ?");
            args.add(params.get("phone"));
        }
        if (params.containsKey("roleId")) {
            sql.append(", role_id = ?");
            args.add(params.get("roleId"));
        }
        if (params.containsKey("status")) {
            sql.append(", status = ?");
            args.add(params.get("status"));
        }
        
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    @Transactional
    public String resetPassword(Long id) {
        String newPassword = "Shiyiju" + System.currentTimeMillis() % 10000;
        String encodedPassword = passwordEncoder.encode(newPassword);
        jdbcTemplate.update("UPDATE admin_user SET password = ?, update_time = ? WHERE id = ?",
            encodedPassword, LocalDateTime.now(), id);
        return newPassword;
    }

    @Transactional
    public void toggleAdminStatus(Long id, Integer status) {
        jdbcTemplate.update("UPDATE admin_user SET status = ?, update_time = ? WHERE id = ?",
            status, LocalDateTime.now(), id);
    }

    @Transactional
    public boolean deleteAdmin(Long id) {
        if (id == 1L) return false;
        int rows = jdbcTemplate.update("DELETE FROM admin_user WHERE id = ?", id);
        return rows > 0;
    }

    // ==================== 操作日志 ====================

    @Transactional
    public void saveLog(Map<String, Object> params) {
        Long adminId = params.get("adminId") != null ? Long.parseLong(params.get("adminId").toString()) : null;
        String adminName = params.get("adminName") != null ? (String) params.get("adminName") : "";
        String module = params.get("module") != null ? (String) params.get("module") : "";
        String operation = params.get("operation") != null ? (String) params.get("operation") : "";
        String detail = params.get("detail") != null ? (String) params.get("detail") : "";
        String ip = params.get("ip") != null ? (String) params.get("ip") : "";
        String userAgent = params.get("userAgent") != null ? (String) params.get("userAgent") : "";
        
        jdbcTemplate.update(
            "INSERT INTO admin_operation_log (admin_id, admin_name, module, operation, detail, ip, user_agent) VALUES (?, ?, ?, ?, ?, ?, ?)",
            adminId, adminName, module, operation, detail, ip, userAgent);
    }

    private Map<String, Object> convertRole(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.get("id"));
        item.put("name", row.get("role_name"));
        item.put("code", row.get("role_code"));
        item.put("description", row.get("description"));
        item.put("status", row.get("status"));
        item.put("createTime", row.get("create_time"));
        item.put("updateTime", row.get("update_time"));
        return item;
    }
}
