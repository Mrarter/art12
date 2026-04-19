package com.shiyiju.admin.controller;

import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        
        // 验证逻辑（实际应查询数据库验证）
        if ("admin".equals(username) && "admin123".equals(password)) {
            Map<String, Object> data = new HashMap<>();
            data.put("token", "admin_token_" + System.currentTimeMillis());
            Map<String, Object> admin = new HashMap<>();
            admin.put("id", 1);
            admin.put("username", username);
            admin.put("role", "super");
            data.put("admin", admin);
            return Result.success(data);
        }
        return Result.fail("用户名或密码错误");
    }

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", 12580);
        stats.put("artworkCount", 3560);
        stats.put("orderCount", 8920);
        stats.put("totalSales", 2568000);
        return Result.success(stats);
    }

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getAdminInfo() {
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", 1);
        admin.put("username", "admin");
        admin.put("role", "super");
        return Result.success(admin);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
