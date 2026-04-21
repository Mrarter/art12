package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 权限管理控制器
 */
@RestController
@RequestMapping("/admin/permission")
public class PermissionController {

    private static final Logger log = LoggerFactory.getLogger(PermissionController.class);

    /**
     * 获取角色列表
     */
    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> getRoles() {
        List<Map<String, Object>> roles = new ArrayList<>();
        roles.add(createRole(1L, "超级管理员", "super_admin", "拥有所有权限", 2));
        roles.add(createRole(2L, "运营专员", "operator", "负责日常运营工作", 5));
        roles.add(createRole(3L, "财务专员", "finance", "负责财务相关工作", 2));
        roles.add(createRole(4L, "内容审核员", "content_auditor", "负责内容审核", 3));
        return Result.success(roles);
    }

    private Map<String, Object> createRole(Long id, String name, String code, String desc, int userCount) {
        Map<String, Object> role = new HashMap<>();
        role.put("id", id);
        role.put("name", name);
        role.put("code", code);
        role.put("description", desc);
        role.put("userCount", userCount);
        role.put("status", 1);
        role.put("createTime", "2024-01-01 00:00:00");
        return role;
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/roles/{id}")
    public Result<Map<String, Object>> getRoleDetail(@PathVariable Long id) {
        Map<String, Object> role = createRole(id, "超级管理员", "super_admin", "拥有所有权限", 2);
        role.put("menuIds", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        return Result.success(role);
    }

    /**
     * 创建角色
     */
    @PostMapping("/roles")
    public Result<Void> createRole(@RequestBody Map<String, Object> params) {
        log.info("创建角色: {}", params);
        return Result.success();
    }

    /**
     * 更新角色
     */
    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("更新角色: id={}", id);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        log.info("删除角色: id={}", id);
        return Result.success();
    }

    /**
     * 获取菜单列表
     */
    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> getMenus() {
        List<Map<String, Object>> menus = new ArrayList<>();
        menus.add(createMenu(1L, "工作台", "/dashboard", 1, 0L));
        menus.add(createMenu(2L, "用户管理", "/user", 2, 0L));
        menus.add(createMenu(201L, "用户列表", "/user/list", 1, 2L));
        menus.add(createMenu(202L, "艺术家管理", "/user/artist", 2, 2L));
        menus.add(createMenu(203L, "艺荐官管理", "/user/promoter", 3, 2L));
        menus.add(createMenu(3L, "作品管理", "/product", 3, 0L));
        menus.add(createMenu(301L, "作品列表", "/product/list", 1, 3L));
        menus.add(createMenu(302L, "待审核", "/product/audit", 2, 3L));
        menus.add(createMenu(303L, "分类管理", "/product/category", 3, 3L));
        menus.add(createMenu(4L, "订单管理", "/order", 4, 0L));
        menus.add(createMenu(401L, "订单列表", "/order/list", 1, 4L));
        menus.add(createMenu(402L, "售后管理", "/order/aftersale", 2, 4L));
        menus.add(createMenu(5L, "社区管理", "/community", 5, 0L));
        menus.add(createMenu(501L, "帖子管理", "/community/post", 1, 5L));
        menus.add(createMenu(502L, "评论管理", "/community/comment", 2, 5L));
        menus.add(createMenu(6L, "拍卖管理", "/auction", 6, 0L));
        menus.add(createMenu(601L, "专场列表", "/auction/session", 1, 6L));
        menus.add(createMenu(602L, "拍品管理", "/auction/lot", 2, 6L));
        menus.add(createMenu(7L, "分销管理", "/promotion", 7, 0L));
        menus.add(createMenu(701L, "佣金配置", "/promotion/commission", 1, 7L));
        menus.add(createMenu(702L, "提现管理", "/promotion/withdraw", 2, 7L));
        menus.add(createMenu(8L, "系统设置", "/system", 8, 0L));
        menus.add(createMenu(801L, "管理员", "/system/admin", 1, 8L));
        menus.add(createMenu(802L, "轮播图", "/system/banner", 2, 8L));
        menus.add(createMenu(803L, "系统配置", "/system/config", 3, 8L));
        return Result.success(menus);
    }

    private Map<String, Object> createMenu(Long id, String name, String path, int sort, Long parentId) {
        Map<String, Object> menu = new HashMap<>();
        menu.put("id", id);
        menu.put("name", name);
        menu.put("path", path);
        menu.put("sort", sort);
        menu.put("parentId", parentId);
        return menu;
    }

    /**
     * 分配角色菜单权限
     */
    @PostMapping("/roles/{id}/menus")
    public Result<Void> assignRoleMenus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> menuIds = (List<Long>) params.get("menuIds");
        log.info("分配菜单权限: roleId={}, menuIds={}", id, menuIds);
        return Result.success();
    }

    /**
     * 获取管理员列表
     */
    @GetMapping("/admins")
    public Result<PageResult<Map<String, Object>>> getAdmins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Map<String, Object>> admins = new ArrayList<>();
        String[] names = {"管理员A", "运营专员B", "财务专员C", "审核员D"};
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> admin = new HashMap<>();
            admin.put("id", i + 1);
            admin.put("nickname", names[i]);
            admin.put("phone", "13800138" + String.format("%03d", i + 1));
            admin.put("roleName", i == 0 ? "超级管理员" : i == 1 ? "运营专员" : i == 2 ? "财务专员" : "内容审核员");
            admin.put("status", 1);
            admin.put("lastLoginTime", "2024-01-15 10:30:00");
            admins.add(admin);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(admins);
        result.setTotal(4L);
        return Result.success(result);
    }

    /**
     * 创建管理员
     */
    @PostMapping("/admins")
    public Result<Void> createAdmin(@RequestBody Map<String, Object> params) {
        log.info("创建管理员: {}", params);
        return Result.success();
    }

    /**
     * 重置管理员密码
     */
    @PostMapping("/admins/{id}/reset-password")
    public Result<Map<String, Object>> resetPassword(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("password", "Shiyiju" + System.currentTimeMillis() % 10000);
        log.info("重置管理员密码: id={}", id);
        return Result.success(result);
    }
}
