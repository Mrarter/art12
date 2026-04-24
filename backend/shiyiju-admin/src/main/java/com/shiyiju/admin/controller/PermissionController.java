package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.PermissionService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 权限管理控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin/permission")
public class PermissionController {

    private static final Logger log = LoggerFactory.getLogger(PermissionController.class);

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // ==================== 角色管理 ====================

    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> getRoles() {
        List<Map<String, Object>> roles = permissionService.getRoles();
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> role : roles) {
            Map<String, Object> map = new LinkedHashMap<>(role);
            map.put("userCount", permissionService.getRoleUserCount((Long) role.get("id")));
            result.add(map);
        }
        
        return Result.success(result);
    }

    @GetMapping("/roles/{id}")
    public Result<Map<String, Object>> getRoleDetail(@PathVariable Long id) {
        Map<String, Object> role = permissionService.getRoleById(id);
        if (role == null) {
            return Result.fail("角色不存在");
        }
        
        List<Long> menuIds = permissionService.getRoleMenuIds(id);
        role.put("menuIds", menuIds);
        
        return Result.success(role);
    }

    @PostMapping("/roles")
    public Result<Void> createRole(@RequestBody Map<String, Object> params) {
        String name = (String) params.get("name");
        String code = (String) params.get("code");
        
        if (name == null || name.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            return Result.fail("角色名称和编码不能为空");
        }

        try {
            permissionService.createRole(params);
            log.info("创建角色成功: {}", name);
            return Result.success();
        } catch (Exception e) {
            log.error("创建角色失败", e);
            return Result.fail("创建角色失败: " + e.getMessage());
        }
    }

    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        String name = (String) params.get("name");
        
        if (name == null || name.trim().isEmpty()) {
            return Result.fail("角色名称不能为空");
        }

        try {
            permissionService.updateRole(id, params);
            log.info("更新角色成功: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("更新角色失败", e);
            return Result.fail("更新角色失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        if (id == 1L) {
            return Result.fail("禁止删除超级管理员角色");
        }

        try {
            boolean success = permissionService.deleteRole(id);
            if (success) {
                log.info("删除角色成功: id={}", id);
                return Result.success();
            } else {
                return Result.fail("角色不存在");
            }
        } catch (Exception e) {
            log.error("删除角色失败", e);
            return Result.fail("删除角色失败: " + e.getMessage());
        }
    }

    // ==================== 菜单管理 ====================

    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> getMenus() {
        return Result.success(permissionService.getMenus());
    }

    @PostMapping("/roles/{id}/menus")
    public Result<Void> assignRoleMenus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> menuIds = (List<Long>) params.get("menuIds");

        if (menuIds == null) {
            menuIds = new ArrayList<>();
        }

        try {
            permissionService.assignRoleMenus(id, menuIds);
            log.info("分配菜单权限成功: roleId={}, menuIds={}", id, menuIds);
            return Result.success();
        } catch (Exception e) {
            log.error("分配菜单权限失败", e);
            return Result.fail("分配菜单权限失败: " + e.getMessage());
        }
    }

    // ==================== 管理员管理 ====================

    @GetMapping("/admins")
    public Result<PageResult<Map<String, Object>>> getAdmins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return Result.success(permissionService.getAdmins(page, size));
    }

    @PostMapping("/admins")
    public Result<Void> createAdmin(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Result.fail("用户名和密码不能为空");
        }

        try {
            permissionService.createAdmin(params);
            log.info("创建管理员成功: username={}", username);
            return Result.success();
        } catch (Exception e) {
            log.error("创建管理员失败", e);
            return Result.fail("创建管理员失败: " + e.getMessage());
        }
    }

    @PostMapping("/admins/{id}/reset-password")
    public Result<Map<String, Object>> resetPassword(@PathVariable Long id) {
        try {
            String newPassword = permissionService.resetPassword(id);
            log.info("重置管理员密码成功: id={}", id);

            Map<String, Object> result = new HashMap<>();
            result.put("password", newPassword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("重置管理员密码失败", e);
            return Result.fail("重置密码失败: " + e.getMessage());
        }
    }

    @PostMapping("/admins/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer status = Integer.parseInt(params.get("status").toString());

        if (id == 1L) {
            return Result.fail("禁止修改超级管理员状态");
        }

        try {
            permissionService.toggleAdminStatus(id, status);
            log.info("切换管理员状态成功: id={}, status={}", id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("切换管理员状态失败", e);
            return Result.fail("操作失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/admins/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        if (id == 1L) {
            return Result.fail("禁止删除超级管理员");
        }

        try {
            boolean success = permissionService.deleteAdmin(id);
            if (success) {
                log.info("删除管理员成功: id={}", id);
                return Result.success();
            } else {
                return Result.fail("管理员不存在");
            }
        } catch (Exception e) {
            log.error("删除管理员失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }
}
