package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 分销统计控制器
 */
@RestController
@RequestMapping("/admin/promotion/admin")
public class PromotionStatsController {

    /**
     * 分销统计概览
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPromoters", 500);
        stats.put("activePromoters", 320);
        stats.put("totalCommission", 850000.00);
        stats.put("todayCommission", 12500.00);
        stats.put("totalOrders", 2500);
        stats.put("conversionRate", 15.5);
        return Result.success(stats);
    }

    /**
     * 艺荐官排行
     */
    @GetMapping("/rank")
    public Result<List<Map<String, Object>>> getRank(
            @RequestParam(defaultValue = "20") int limit) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= Math.min(limit, 10); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", 1000 + i);
            item.put("nickname", "艺荐官" + i);
            item.put("level", i <= 3 ? "钻石" : "金牌");
            item.put("teamSize", 50 - i * 3);
            item.put("commission", 50000.00 - i * 3000);
            item.put("orderCount", 200 - i * 10);
            list.add(item);
        }
        return Result.success(list);
    }

    /**
     * 团队列表
     */
    @GetMapping("/team-list")
    public Result<PageResult<Map<String, Object>>> getTeamList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> team = new LinkedHashMap<>();
            team.put("leaderId", 1000 + i);
            team.put("leaderName", "团队长" + i);
            team.put("memberCount", 50 + i * 5);
            team.put("totalCommission", 80000.00 + i * 5000);
            team.put("thisMonthCommission", 5000.00 + i * 300);
            list.add(team);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(50L);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }

    /**
     * 分销趋势
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(defaultValue = "7") int days) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", "2024-04-" + String.format("%02d", 15 - i));
            item.put("newPromoters", 10 + i % 5);
            item.put("orders", 80 + i * 5);
            item.put("commission", 5000.00 + i * 500);
            list.add(item);
        }
        return Result.success(list);
    }
}