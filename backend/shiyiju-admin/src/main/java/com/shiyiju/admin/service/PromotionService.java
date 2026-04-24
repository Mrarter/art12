package com.shiyiju.admin.service;

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

    public PromotionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM commission_log", Long.class);

        List<Object> args = new ArrayList<>();
        args.add((page - 1) * size);
        args.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, promoter_id, order_id, order_no, commission_amount, level,
                   status, settle_time, create_time
            FROM commission_log ORDER BY create_time DESC LIMIT ?, ?
            """, args.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("promoterId", row.get("promoter_id"));
            item.put("orderId", row.get("order_id"));
            item.put("orderNo", row.get("order_no"));
            item.put("commissionAmount", row.get("commission_amount"));
            item.put("level", row.get("level"));
            item.put("status", row.get("status"));
            item.put("settleTime", row.get("settle_time"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public void settleCommission(Long id) {
        jdbcTemplate.update("UPDATE commission_log SET status = 1, settle_time = ? WHERE id = ?",
            LocalDateTime.now(), id);
    }

    // ==================== 提现管理 ====================

    public PageResult<Map<String, Object>> getWithdraws(int page, int size, Integer status) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM withdraw_record" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, user_id, nickname, bank_name, bank_account, amount,
                   status, remark, apply_time, handle_time, complete_time, reject_reason
            FROM withdraw_record
            """ + where + " ORDER BY apply_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("userId", row.get("user_id"));
            item.put("nickname", row.get("nickname"));
            item.put("bankName", row.get("bank_name"));
            item.put("bankAccount", row.get("bank_account"));
            item.put("amount", row.get("amount"));
            item.put("status", row.get("status"));
            item.put("remark", row.get("remark"));
            item.put("applyTime", row.get("apply_time"));
            item.put("handleTime", row.get("handle_time"));
            item.put("completeTime", row.get("complete_time"));
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
        if (status == 1) {
            jdbcTemplate.update(
                "UPDATE withdraw_record SET status = ?, handle_time = ?, complete_time = ?, operator_id = ?, operator_name = ? WHERE id = ?",
                status, now, now, operatorId, operatorName, id);
        } else if (status == 2) {
            jdbcTemplate.update(
                "UPDATE withdraw_record SET status = ?, handle_time = ?, reject_reason = ?, operator_id = ?, operator_name = ? WHERE id = ?",
                status, now, remark, operatorId, operatorName, id);
        } else {
            jdbcTemplate.update(
                "UPDATE withdraw_record SET status = ?, handle_time = ?, operator_id = ?, operator_name = ? WHERE id = ?",
                status, now, operatorId, operatorName, id);
        }
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
