package com.shiyiju.admin.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 分销统计控制器
 */
@RestController
@RequestMapping("/admin/promotion/admin")
public class PromotionStatsController {

    private final JdbcTemplate jdbcTemplate;

    public PromotionStatsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // 默认统计
        stats.put("totalPromoters", 0);
        stats.put("activePromoters", 0);
        stats.put("totalCommission", BigDecimal.ZERO);
        stats.put("pendingWithdraw", BigDecimal.ZERO);
        stats.put("totalOrders", 0);
        
        // 如果表存在，查询真实数据
        if (tableExists("promoter_record")) {
            try {
                Long totalPromoters = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM promoter_record", Long.class);
                BigDecimal totalCommission = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(commission_amount), 0) FROM commission_record", BigDecimal.class);
                BigDecimal pendingWithdraw = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM withdraw_record WHERE status = 'pending'", BigDecimal.class);
                
                stats.put("totalPromoters", totalPromoters != null ? totalPromoters : 0);
                stats.put("totalCommission", totalCommission != null ? totalCommission : BigDecimal.ZERO);
                stats.put("pendingWithdraw", pendingWithdraw != null ? pendingWithdraw : BigDecimal.ZERO);
            } catch (Exception e) {
                // 使用默认值
            }
        }
        
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", stats);
        return result;
    }

    @GetMapping("/rank")
    public Map<String, Object> getRank() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", new ArrayList<>());
        return result;
    }

    @GetMapping("/team-list")
    public Map<String, Object> getTeamList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String promoterId) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("total", 0);
        pageData.put("records", new ArrayList<>());
        
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", pageData);
        return result;
    }

    @GetMapping("/trend")
    public Map<String, Object> getTrend(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", new ArrayList<>());
        return result;
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'shiyiju' AND table_name = ?",
                Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
