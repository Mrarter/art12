package com.shiyiju.admin.service;

import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MessageAdminPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public MessageAdminPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<Map<String, Object>> listMessages(String type, String status, int page, int size) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (type != null && !type.isBlank()) {
            where.append(" AND type = ?");
            args.add(type);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_message_record" + where,
            Long.class,
            args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, type, title, content, target_type, target_name, group_name, phone,
                   push_enabled, sms_enabled, email_enabled, status, success_count,
                   fail_count, scheduled_time, sent_at, create_time
            FROM admin_message_record
            """ + where + """
            ORDER BY create_time DESC, id DESC
            LIMIT ?, ?
            """, queryArgs.toArray());

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("type", row.get("type"));
            item.put("title", row.get("title"));
            item.put("content", row.get("content"));
            item.put("targetType", row.get("target_type"));
            item.put("targetName", row.get("target_name"));
            item.put("groupName", row.get("group_name"));
            item.put("phone", row.get("phone"));
            item.put("push", toBoolean(row.get("push_enabled")));
            item.put("sms", toBoolean(row.get("sms_enabled")));
            item.put("email", toBoolean(row.get("email_enabled")));
            item.put("status", row.get("status"));
            item.put("successCount", toInt(row.get("success_count"), 0));
            item.put("failCount", toInt(row.get("fail_count"), 0));
            item.put("sentAt", row.get("sent_at"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }
        return PageResult.of(total == null ? 0L : total, page, size, records);
    }

    public Long sendMessage(Map<String, Object> params) {
        String targetType = Objects.toString(params.get("targetType"), "all");
        String status = toBoolean(params.get("timing")) ? "pending" : "sent";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduled = parseDateTime(params.get("sendTime"));
        LocalDateTime sentAt = "sent".equals(status) ? now : null;
        int successCount = "sent".equals(status) ? estimateSuccessCount(targetType) : 0;

        jdbcTemplate.update("""
            INSERT INTO admin_message_record (
                type, title, content, target_type, target_name, group_name, phone,
                push_enabled, sms_enabled, email_enabled, status, success_count,
                fail_count, scheduled_time, sent_at, create_time, update_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            Objects.toString(params.get("type"), "system"),
            Objects.toString(params.get("title"), ""),
            Objects.toString(params.get("content"), ""),
            targetType,
            "single".equals(targetType) ? Objects.toString(params.get("phone"), "") : null,
            "group".equals(targetType) ? Objects.toString(params.get("groupId"), "") : null,
            Objects.toString(params.get("phone"), null),
            hasMethod(params, "push") ? 1 : 0,
            hasMethod(params, "sms") ? 1 : 0,
            hasMethod(params, "email") ? 1 : 0,
            status,
            successCount,
            0,
            scheduled,
            sentAt,
            now,
            now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<Map<String, Object>> listTemplates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, name, type, title, content, status, create_time
            FROM admin_message_template
            ORDER BY id DESC
            """);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("name", row.get("name"));
            item.put("type", row.get("type"));
            item.put("title", row.get("title"));
            item.put("content", row.get("content"));
            item.put("status", toInt(row.get("status"), 1));
            item.put("createTime", row.get("create_time"));
            result.add(item);
        }
        return result;
    }

    public Long createTemplate(Map<String, Object> params) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
            INSERT INTO admin_message_template (name, type, title, content, status, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            Objects.toString(params.get("name"), ""),
            Objects.toString(params.get("type"), "system"),
            Objects.toString(params.get("title"), ""),
            Objects.toString(params.get("content"), ""),
            toInt(params.get("status"), 1),
            now,
            now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateTemplate(Long id, Map<String, Object> params) {
        jdbcTemplate.update("""
            UPDATE admin_message_template
            SET name = ?, type = ?, title = ?, content = ?, status = ?, update_time = ?
            WHERE id = ?
            """,
            Objects.toString(params.get("name"), ""),
            Objects.toString(params.get("type"), "system"),
            Objects.toString(params.get("title"), ""),
            Objects.toString(params.get("content"), ""),
            toInt(params.get("status"), 1),
            LocalDateTime.now(),
            id
        );
    }

    public void deleteTemplate(Long id) {
        jdbcTemplate.update("DELETE FROM admin_message_template WHERE id = ?", id);
    }

    public void updateTemplateStatus(Long id, int status) {
        jdbcTemplate.update("""
            UPDATE admin_message_template
            SET status = ?, update_time = ?
            WHERE id = ?
            """, status, LocalDateTime.now(), id);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_message_record", Long.class);
        Long todaySent = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM admin_message_record
            WHERE DATE(create_time) = ? AND status = 'sent'
            """, Long.class, LocalDate.now());
        Long pending = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM admin_message_record
            WHERE status = 'pending'
            """, Long.class);
        Long failed = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM admin_message_record
            WHERE status = 'failed'
            """, Long.class);
        stats.put("total", total == null ? 0 : total);
        stats.put("todaySent", todaySent == null ? 0 : todaySent);
        stats.put("pending", pending == null ? 0 : pending);
        stats.put("failed", failed == null ? 0 : failed);
        return stats;
    }

    private boolean hasMethod(Map<String, Object> params, String method) {
        Object methods = params.get("methods");
        if (methods instanceof Iterable<?> iterable) {
            for (Object value : iterable) {
                if (method.equals(Objects.toString(value, ""))) {
                    return true;
                }
            }
            return false;
        }
        return Objects.toString(methods, "").contains(method);
    }

    private int estimateSuccessCount(String targetType) {
        return "all".equals(targetType) ? 1 : 1;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return Boolean.parseBoolean(Objects.toString(value, "false"));
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        String text = Objects.toString(value, "").trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ex) {
            return null;
        }
    }
}
