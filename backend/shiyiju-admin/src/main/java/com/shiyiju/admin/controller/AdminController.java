package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.AdminAccountService;
import com.shiyiju.admin.service.support.SchemaInspector;
import com.shiyiju.admin.mapper.*;
import com.shiyiju.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaInspector schemaInspector;

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

        String userTable = schemaInspector.resolveTable("dashboard_user_table", "user_account", "users", "sys_user");
        String artworkTable = schemaInspector.resolveTable("dashboard_artwork_table", "artwork", "artworks", "product");
        String orderTable = schemaInspector.resolveTable("dashboard_order_table", "trade_order", "orders", "order");
        String artistTable = schemaInspector.resolveTable("dashboard_artist_table", "artist_profile", "artist_certifications", "artist");
        String lotTable = schemaInspector.resolveTable("dashboard_lot_table", "auction_lots", "auction_lot");

        String orderAmountColumn = schemaInspector.firstExistingColumn(orderTable, "pay_amount", "total_amount", "order_amount", "amount");
        String orderTimeColumn = schemaInspector.firstExistingColumn(orderTable, "created_at", "create_time");
        String orderStatusColumn = schemaInspector.firstExistingColumn(orderTable, "order_status", "status");

        stats.put("userCount", countRows(userTable));
        stats.put("artworkCount", countRows(artworkTable));
        stats.put("productCount", countRows(artworkTable));
        stats.put("orderCount", countRows(orderTable));
        stats.put("totalSales", queryDecimal("SELECT COALESCE(SUM(" + orderAmountColumn + "), 0) FROM " + orderTable));
        stats.put("amount", stats.get("totalSales"));
        stats.put("userGrowth", 0);
        stats.put("productGrowth", 0);
        stats.put("orderGrowth", 0);
        stats.put("amountGrowth", 0);

        Map<String, Object> todo = new LinkedHashMap<>();
        todo.put("pendingArtist", countByStatus(artistTable, schemaInspector.firstExistingColumn(artistTable, "status", "cert_status"), 0));
        todo.put("pendingProduct", countByStatus(artworkTable, schemaInspector.firstExistingColumn(artworkTable, "status", "audit_status"), 0));
        todo.put("pendingLot", countByStatus(lotTable, schemaInspector.firstExistingColumn(lotTable, "status"), 0));
        stats.put("todo", todo);

        stats.put("recentOrders", recentOrders(orderTable, orderAmountColumn, orderStatusColumn, orderTimeColumn));
        stats.put("orderStatus", orderStatusDistribution(orderTable, orderStatusColumn));
        stats.put("trend", orderTrend(orderTable, orderAmountColumn, orderTimeColumn));

        return Result.success(stats);
    }

    private Long countRows(String tableName) {
        if (tableName == null || schemaInspector.getColumns(tableName).isEmpty()) {
            return 0L;
        }
        return queryLong("SELECT COUNT(*) FROM " + tableName);
    }

    private Long countByStatus(String tableName, String statusColumn, Object status) {
        if (tableName == null || statusColumn == null || !schemaInspector.hasColumn(tableName, statusColumn)) {
            return 0L;
        }
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + statusColumn + " = ?",
                Long.class,
                status
            );
        } catch (Exception ex) {
            return 0L;
        }
    }

    private Long queryLong(String sql) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class);
            return value == null ? 0L : value;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private BigDecimal queryDecimal(String sql) {
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            return value == null ? BigDecimal.ZERO : value;
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private List<Map<String, Object>> recentOrders(String orderTable, String amountColumn, String statusColumn, String timeColumn) {
        if (orderTable == null || schemaInspector.getColumns(orderTable).isEmpty()) {
            return List.of();
        }
        String orderNoColumn = schemaInspector.firstExistingColumn(orderTable, "order_no", "orderNo");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT " + orderNoColumn + " AS order_no, " + amountColumn + " AS amount, " + statusColumn + " AS status FROM " +
                orderTable + " ORDER BY " + timeColumn + " DESC LIMIT 4"
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderNo", row.get("order_no"));
            item.put("amount", row.get("amount"));
            item.put("status", normalizeOrderStatus(row.get("status")));
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> orderStatusDistribution(String orderTable, String statusColumn) {
        if (orderTable == null || statusColumn == null || !schemaInspector.hasColumn(orderTable, statusColumn)) {
            return List.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT " + statusColumn + " AS status, COUNT(*) AS count FROM " + orderTable + " GROUP BY " + statusColumn
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", orderStatusName(normalizeOrderStatus(row.get("status"))));
            item.put("status", normalizeOrderStatus(row.get("status")));
            item.put("value", row.get("count"));
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> orderTrend(String orderTable, String amountColumn, String timeColumn) {
        if (orderTable == null || schemaInspector.getColumns(orderTable).isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT DATE(" + timeColumn + ") AS day, COUNT(*) AS order_count, COALESCE(SUM(" + amountColumn + "), 0) AS amount " +
                "FROM " + orderTable + " WHERE DATE(" + timeColumn + ") >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                "GROUP BY DATE(" + timeColumn + ") ORDER BY day ASC"
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", String.valueOf(row.get("day")));
            item.put("orderCount", row.get("order_count"));
            item.put("amount", row.get("amount"));
            list.add(item);
        }
        return list;
    }

    private String normalizeOrderStatus(Object status) {
        String value = status == null ? "" : status.toString();
        return switch (value) {
            case "PENDING_PAYMENT", "UNPAID", "pending" -> "pending";
            case "PAID", "paid" -> "paid";
            case "WAIT_DELIVER", "WAIT_SHIP", "wait_deliver" -> "paid";
            case "SHIPPED", "shipped" -> "shipped";
            case "COMPLETED", "FINISHED", "completed" -> "completed";
            case "CANCELLED", "CANCELED", "cancelled" -> "cancelled";
            default -> value.toLowerCase();
        };
    }

    private String orderStatusName(String status) {
        return switch (status) {
            case "pending" -> "待付款";
            case "paid" -> "已付款";
            case "shipped" -> "已发货";
            case "completed" -> "已完成";
            case "cancelled" -> "已取消";
            default -> status;
        };
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
