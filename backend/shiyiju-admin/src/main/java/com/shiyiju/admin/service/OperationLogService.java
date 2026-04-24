package com.shiyiju.admin.service;

import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 操作日志服务 - 真实持久化（使用 JdbcTemplate）
 */
@Service
public class OperationLogService {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<Map<String, Object>> getLogs(String adminName, String operation, 
                                                     String startDate, String endDate, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (adminName != null && !adminName.isEmpty()) {
            where.append(" AND admin_name LIKE ?");
            args.add("%" + adminName + "%");
        }
        if (operation != null && !operation.isEmpty()) {
            where.append(" AND operation LIKE ?");
            args.add("%" + operation + "%");
        }
        if (startDate != null) {
            where.append(" AND create_time >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            where.append(" AND create_time <= ?");
            args.add(endDate);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_operation_log" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, admin_id, admin_name, module, operation, detail, ip, user_agent, create_time
            FROM admin_operation_log
            """ + where + " ORDER BY create_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("adminId", row.get("admin_id"));
            item.put("adminName", row.get("admin_name"));
            item.put("module", row.get("module"));
            item.put("operation", row.get("operation"));
            item.put("detail", row.get("detail"));
            item.put("ip", row.get("ip"));
            item.put("userAgent", row.get("user_agent"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        stats.put("todayCount", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_operation_log WHERE DATE(create_time) = CURDATE()", Long.class));
        stats.put("weekCount", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_operation_log WHERE YEARWEEK(create_time, 1) = YEARWEEK(CURDATE(), 1)", Long.class));
        stats.put("monthCount", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_operation_log WHERE DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')", Long.class));
        
        stats.put("topOperations", new ArrayList<>());
        
        return stats;
    }
}
