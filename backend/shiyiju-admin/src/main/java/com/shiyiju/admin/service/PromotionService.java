package com.shiyiju.admin.service;

import com.shiyiju.common.client.WalletRestClient;
import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 分销管理服务 - 真实持久化（使用 JdbcTemplate）
 */
@Service
public class PromotionService {

    private final JdbcTemplate jdbcTemplate;
    private final WalletRestClient walletClient;

    public PromotionService(JdbcTemplate jdbcTemplate, WalletRestClient walletClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.walletClient = walletClient;
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " LIMIT 1", Long.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 分销配置 ====================

    public Map<String, Object> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT config_key, config_value FROM system_config WHERE config_key LIKE 'commission.%'");
        
        Map<String, String> configMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            configMap.put((String) row.get("config_key"), (String) row.get("config_value"));
        }
        
        config.put("directRate", configMap.getOrDefault("commission.direct.rate", "0.05"));
        config.put("teamRate", configMap.getOrDefault("commission.team.rate", "0.02"));
        config.put("firstLevelRate", configMap.getOrDefault("commission.level.first", "0.10"));
        config.put("secondLevelRate", configMap.getOrDefault("commission.level.second", "0.05"));
        
        return config;
    }

    @Transactional
    public void updateConfig(String key, String value) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM system_config WHERE config_key = ?", Integer.class, key);
        if (count > 0) {
            jdbcTemplate.update("UPDATE system_config SET config_value = ?, update_time = ? WHERE config_key = ?",
                value, LocalDateTime.now(), key);
        } else {
            jdbcTemplate.update("INSERT INTO system_config (config_key, config_value, create_time, update_time) VALUES (?, ?, ?, ?)",
                key, value, LocalDateTime.now(), LocalDateTime.now());
        }
    }

    // ==================== 艺荐官管理 ====================

    public PageResult<Map<String, Object>> getPromoters(int page, int size, Integer level) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (level != null) {
            where.append(" AND level = ?");
            args.add(level);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM promoter_record" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, user_id, nickname, level, team_size, total_orders, total_sales,
                   available_commission, total_commission, status, create_time
            FROM promoter_record
            """ + where + " ORDER BY create_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("userId", row.get("user_id"));
            item.put("nickname", row.get("nickname"));
            item.put("level", row.get("level"));
            item.put("teamSize", row.get("team_size"));
            item.put("totalOrders", row.get("total_orders"));
            item.put("totalSales", row.get("total_sales"));
            item.put("availableCommission", row.get("available_commission"));
            item.put("totalCommission", row.get("total_commission"));
            item.put("status", row.get("status"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    public Map<String, Object> getPromoterById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM promoter_record WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        return convertPromoter(rows.get(0));
    }

    @Transactional
    public void togglePromoterStatus(Long id, Integer status) {
        jdbcTemplate.update("UPDATE promoter_record SET status = ?, update_time = ? WHERE id = ?",
            status, LocalDateTime.now(), id);
    }

    // ==================== 佣金管理 ====================

    public PageResult<Map<String, Object>> getCommissions(int page, int size) {
        return getCommissions(page, size, null, null, null, null);
    }

    public PageResult<Map<String, Object>> getCommissions(int page, int size, Long userId) {
        return getCommissions(page, size, userId, null, null, null);
    }

    public PageResult<Map<String, Object>> getCommissions(int page, int size, Long userId,
                                                           String type, String startDate, String endDate) {
        if (!tableExists("commission_record")) {
            PageResult<Map<String, Object>> result = new PageResult<>();
            result.setRecords(new ArrayList<>());
            result.setTotal(0L);
            return result;
        }

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (userId != null) {
            where.append(" AND cr.user_id = ?");
            args.add(userId);
        }
        if ("direct".equals(type)) where.append(" AND cr.commission_level = 1");
        if ("team".equals(type)) where.append(" AND cr.commission_level = 2");
        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND cr.created_time >= ?");
            args.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND cr.created_time < DATE_ADD(?, INTERVAL 1 DAY)");
            args.add(endDate + " 00:00:00");
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM commission_record cr" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT cr.id, cr.user_id, cr.source_user_id, cr.order_id, cr.artwork_id,
                   cr.commission_type, cr.commission_level, cr.rate, cr.amount, cr.status,
                   cr.remark, cr.created_time,
                   promoter.nickname AS promoter_name, promoter.phone AS promoter_phone,
                   buyer.nickname AS buyer_name,
                   COALESCE(o.order_no, SUBSTRING_INDEX(cr.remark, ' ', -1)) AS order_no,
                   COALESCE(o.pay_amount,
                     CASE WHEN cr.rate > 0 THEN cr.amount * 100 / cr.rate ELSE 0 END
                   ) AS order_amount
            FROM commission_record cr
            LEFT JOIN users promoter ON promoter.id = cr.user_id
            LEFT JOIN users buyer ON buyer.id = cr.source_user_id
            LEFT JOIN trade_order o ON o.id = cr.order_id
            """ + where + " ORDER BY cr.created_time DESC, cr.id DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Long recordId = ((Number) row.get("id")).longValue();
            item.put("id", recordId);
            item.put("commissionCode", "CMS" + String.format("%016d", recordId));
            item.put("promoterId", row.get("user_id"));
            item.put("promoterName", row.get("promoter_name"));
            item.put("promoterPhone", row.get("promoter_phone"));
            item.put("orderId", row.get("order_id"));
            item.put("orderNo", row.get("order_no"));
            item.put("buyerName", row.get("buyer_name"));
            item.put("orderAmount", toFen(row.get("order_amount")));
            BigDecimal ratePercent = row.get("rate") instanceof BigDecimal value ? value : BigDecimal.ZERO;
            item.put("rate", ratePercent.movePointLeft(2));
            item.put("commission", toFen(row.get("amount")));
            Integer level = row.get("commission_level") instanceof Number value ? value.intValue() : 1;
            item.put("level", level);
            item.put("type", level == 2 ? "team" : "direct");
            item.put("status", row.get("status"));
            item.put("createTime", row.get("created_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public void settleCommission(Long id) {
        jdbcTemplate.update("UPDATE commission_record SET status = 'settled', updated_time = ? WHERE id = ?",
            LocalDateTime.now(), id);
    }

    private long toFen(Object value) {
        if (value == null) return 0L;
        BigDecimal amount = value instanceof BigDecimal decimal
            ? decimal : new BigDecimal(value.toString());
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }

    // ==================== 提现管理 ====================

    public PageResult<Map<String, Object>> getWithdraws(int page, int size, Integer status,
                                                        Long userId, String startDate, String endDate) {
        // 检查表是否存在
        if (!tableExists("withdraw_records")) {
            PageResult<Map<String, Object>> result = new PageResult<>();
            result.setRecords(new ArrayList<>());
            result.setTotal(0L);
            return result;
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        if (status != null) {
            if (status == 3) {
                where.append(" AND (wr.status = ? OR wr.transfer_time IS NOT NULL)");
                args.add(status);
            } else if (status == 1) {
                where.append(" AND wr.status = ? AND wr.transfer_time IS NULL");
                args.add(status);
            } else {
                where.append(" AND wr.status = ?");
                args.add(status);
            }
        }
        if (userId != null) {
            where.append(" AND CASE WHEN wr.promoter_id > 0 THEN pr.user_id ELSE -wr.promoter_id END = ?");
            args.add(userId);
        }
        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND wr.create_time >= ?");
            args.add(startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND wr.create_time <= ?");
            args.add(endDate.trim() + " 23:59:59");
        }

        Long total = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM withdraw_records wr
            LEFT JOIN promoter_record pr ON wr.promoter_id > 0 AND pr.id = wr.promoter_id
            LEFT JOIN user_account u ON u.id = CASE WHEN wr.promoter_id > 0 THEN pr.user_id ELSE -wr.promoter_id END
            """ + where,
            Long.class,
            args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT wr.id,
                   wr.promoter_id,
                   CASE WHEN wr.promoter_id > 0 THEN pr.user_id ELSE -wr.promoter_id END AS user_id,
                   u.nickname,
                   u.phone,
                   wr.amount,
                   wr.fee_amount,
                   wr.actual_amount,
                   wr.account_type,
                   wr.account_info,
                   wr.account_name,
                   wr.status,
                   wr.reject_reason,
                   wr.process_time,
                   wr.transfer_time,
                   wr.create_time
            FROM withdraw_records wr
            LEFT JOIN promoter_record pr ON wr.promoter_id > 0 AND pr.id = wr.promoter_id
            LEFT JOIN user_account u ON u.id = CASE WHEN wr.promoter_id > 0 THEN pr.user_id ELSE -wr.promoter_id END
            """ + where + " ORDER BY wr.create_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("userId", row.get("user_id"));
            item.put("withdrawCode", "WR" + row.get("id"));
            item.put("userName", row.get("nickname"));
            item.put("phone", row.get("phone"));
            item.put("identityType", resolveIdentityType(row.get("promoter_id")));
            item.put("amount", row.get("amount"));
            item.put("feeAmount", row.get("fee_amount"));
            item.put("actualAmount", row.get("actual_amount"));
            item.put("type", row.get("account_type"));
            item.put("account", row.get("account_info"));
            item.put("realName", row.get("account_name"));
            item.put("status", mapWithdrawStatus(row.get("status"), row.get("transfer_time")));
            item.put("paymentStarted", isPaymentStarted(row.get("status"), row.get("process_time")));
            item.put("paymentArrived", isPaymentArrived(row.get("status"), row.get("transfer_time")));
            item.put("createTime", row.get("create_time"));
            item.put("handleTime", row.get("process_time"));
            item.put("completeTime", row.get("transfer_time"));
            item.put("rejectReason", row.get("reject_reason"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public void handleWithdraw(Long id, Integer status, String remark, Long operatorId, String operatorName) {
        LocalDateTime now = LocalDateTime.now();
        
        // 查询提现记录
        Map<String, Object> record = jdbcTemplate.queryForMap(
            """
            SELECT wr.amount,
                   CASE WHEN wr.promoter_id > 0 THEN pr.user_id ELSE -wr.promoter_id END AS user_id
            FROM withdraw_records wr
            LEFT JOIN promoter_record pr ON wr.promoter_id > 0 AND pr.id = wr.promoter_id
            WHERE wr.id = ?
            """,
            id
        );
        Long userId = record.get("user_id") instanceof Number n ? n.longValue() : null;
        java.math.BigDecimal amount = toYuan(record.get("amount"));

        if (status == 1) {
            // 审核通过/启动打款 - 出账（扣除冻结金额）
            if (userId != null && amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                walletClient.expense(userId, amount, "withdraw", id, "withdraw", remark);
            }
            jdbcTemplate.update(
                "UPDATE withdraw_records SET status = ?, process_time = ?, update_time = ? WHERE id = ?",
                status, now, now, id);
        } else if (status == 3) {
            // 确认到账
            jdbcTemplate.update(
                "UPDATE withdraw_records SET status = ?, transfer_time = ?, update_time = ? WHERE id = ?",
                status, now, now, id);
        } else if (status == 2) {
            // 审核拒绝 - 解冻
            if (userId != null && amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                walletClient.unfreeze(userId, amount, id, "withdraw", "提现驳回: " + remark);
            }
            jdbcTemplate.update(
                "UPDATE withdraw_records SET status = ?, process_time = ?, reject_reason = ?, update_time = ? WHERE id = ?",
                status, now, remark, now, id);
        } else {
            jdbcTemplate.update(
                "UPDATE withdraw_records SET status = ?, process_time = ?, update_time = ? WHERE id = ?",
                status, now, now, id);
        }
    }

    private String mapWithdrawStatus(Object rawStatus, Object transferTime) {
        if (transferTime != null) {
            return "paid";
        }
        int status = rawStatus instanceof Number n ? n.intValue() : -1;
        return switch (status) {
            case 0 -> "pending";
            case 1 -> "approved";
            case 2 -> "rejected";
            case 3 -> "paid";
            default -> String.valueOf(rawStatus);
        };
    }

    private String resolveIdentityType(Object rawPromoterId) {
        long promoterId = rawPromoterId instanceof Number n ? n.longValue() : 0L;
        return promoterId > 0 ? "promoter" : "artist";
    }

    private boolean isPaymentStarted(Object rawStatus, Object processTime) {
        int status = rawStatus instanceof Number n ? n.intValue() : -1;
        return processTime != null || status == 1 || status == 3;
    }

    private boolean isPaymentArrived(Object rawStatus, Object transferTime) {
        int status = rawStatus instanceof Number n ? n.intValue() : -1;
        return transferTime != null || status == 3;
    }

    private BigDecimal toYuan(Object rawAmount) {
        if (!(rawAmount instanceof Number number)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(number.longValue()).movePointLeft(2);
    }

    // ==================== 统计 ====================

    public Map<String, Object> getLevelDistribution() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> distribution = new ArrayList<>();
        
        String[] levelNames = {"", "Lv.1见习", "Lv.2新锐", "Lv.3资深", "Lv.4金牌", "Lv.5合伙人"};
        
        int total = 0;
        for (int i = 1; i <= 5; i++) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM promoter_record WHERE level = ?", Integer.class, i);
            count = count != null ? count : 0;
            total += count;
            
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("level", i);
            item.put("name", levelNames[i]);
            item.put("count", count);
            item.put("percent", total > 0 ? Math.round(count * 100.0 / total * 10) / 10.0 : 0);
            distribution.add(item);
        }
        
        result.put("total", total);
        result.put("distribution", distribution);
        return result;
    }

    public List<Map<String, Object>> getCommissionTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            String dateStr = date.toLocalDate().toString();
            
            BigDecimal amount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(commission_amount), 0) FROM commission_log WHERE DATE(create_time) = ?",
                BigDecimal.class, dateStr);
            
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            item.put("amount", amount != null ? amount : BigDecimal.ZERO);
            result.add(item);
        }
        
        return result;
    }

    public List<Map<String, Object>> getTopTeams(int limit) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, user_id, nickname, level, team_size, total_sales
            FROM promoter_record ORDER BY total_sales DESC LIMIT ?
            """, limit
        );

        List<Map<String, Object>> teams = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rows) {
            Map<String, Object> team = new LinkedHashMap<>();
            team.put("rank", rank++);
            team.put("userId", row.get("user_id"));
            team.put("nickname", row.get("nickname"));
            team.put("level", row.get("level"));
            team.put("teamSize", row.get("team_size"));
            team.put("totalSales", row.get("total_sales"));
            teams.add(team);
        }
        
        return teams;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        stats.put("totalPromoters", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM promoter_record", Long.class));
        stats.put("totalCommission", jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(commission_amount), 0) FROM commission_log WHERE status = 1", BigDecimal.class));
        stats.put("pendingCommission", jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(commission_amount), 0) FROM commission_log WHERE status = 0", BigDecimal.class));
        stats.put("totalWithdraw", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM withdraw_record", Long.class));
        stats.put("pendingWithdraw", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM withdraw_record WHERE status = 0", Long.class));
        
        return stats;
    }

    private Map<String, Object> convertPromoter(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.get("id"));
        item.put("userId", row.get("user_id"));
        item.put("nickname", row.get("nickname"));
        item.put("level", row.get("level"));
        item.put("teamSize", row.get("team_size"));
        item.put("totalOrders", row.get("total_orders"));
        item.put("totalSales", row.get("total_sales"));
        item.put("availableCommission", row.get("available_commission"));
        item.put("totalCommission", row.get("total_commission"));
        item.put("status", row.get("status"));
        item.put("createTime", row.get("create_time"));
        return item;
    }
}
