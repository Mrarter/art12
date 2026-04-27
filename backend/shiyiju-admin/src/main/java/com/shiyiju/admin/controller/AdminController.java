package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.AdminAccountService;
import com.shiyiju.admin.mapper.*;
import com.shiyiju.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员主控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private com.shiyiju.admin.mapper.SysUserMapper userMapper;

    @Autowired
    private com.shiyiju.admin.mapper.ArtworkMapper artworkMapper;

    @Autowired
    private com.shiyiju.admin.mapper.OrderInfoMapper orderMapper;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        Map<String, Object> data = adminAccountService.login(username, password);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail("用户名或密码错误");
    }

    /**
     * 获取仪表盘统计数据（真实查询数据库）
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 用户总数
        Long userCount = userMapper.selectCount(null);
        stats.put("userCount", userCount);

        // 作品总数
        Long artworkCount = artworkMapper.selectCount(null);
        stats.put("artworkCount", artworkCount);

        // 订单总数
        Long orderCount = orderMapper.selectCount(null);
        stats.put("orderCount", orderCount);

        // 销售总额
        BigDecimal totalSales = orderMapper.getTotalSales();
        stats.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);

        return Result.success(stats);
    }

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getAdminInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(adminAccountService.getAdminInfo(authorization));
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
