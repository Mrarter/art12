package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProductAdminPersistenceService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final SchemaInspector schemaInspector;

    public ProductAdminPersistenceService(JdbcTemplate jdbcTemplate, SchemaInspector schemaInspector) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaInspector = schemaInspector;
    }

    public List<Map<String, Object>> listCategories() {
        String categoryTable = categoryTable();
        String weightColumn = categoryWeightColumn();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT c.id, c.name, %s AS weight_value, %s AS icon_value, c.status, c.create_time,
                   COALESCE((SELECT COUNT(*) FROM artwork a WHERE a.category_id = c.id), 0) AS artwork_count
            FROM %s c
            ORDER BY %s DESC, c.id DESC
            """.formatted(weightColumn, categoryIconColumn(), categoryTable, weightColumn)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("name", row.get("name"));
            item.put("icon", Objects.toString(row.get("icon_value"), "Picture"));
            item.put("weight", toInt(row.get("weight_value"), 0));
            item.put("artworkCount", toInt(row.get("artwork_count"), 0));
            item.put("status", toInt(row.get("status"), 1));
            item.put("createTime", formatDateTime(row.get("create_time")));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Long createCategory(Map<String, Object> params) {
        String categoryTable = categoryTable();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            """
            INSERT INTO %s (name, %s, %s, status, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?)
            """.formatted(categoryTable, categoryIconColumn(), categoryWeightColumn()),
            Objects.toString(params.get("name"), ""),
            nullableText(params.get("icon")),
            toInt(params.get("weight"), 0),
            toInt(params.get("status"), 1),
            now,
            now
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void updateCategory(Long id, Map<String, Object> params) {
        String categoryTable = categoryTable();
        jdbcTemplate.update(
            """
            UPDATE %s
            SET name = ?, %s = ?, %s = ?, status = ?, update_time = ?
            WHERE id = ?
            """.formatted(categoryTable, categoryIconColumn(), categoryWeightColumn()),
            Objects.toString(params.get("name"), ""),
            nullableText(params.get("icon")),
            toInt(params.get("weight"), 0),
            toInt(params.get("status"), 1),
            LocalDateTime.now(),
            id
        );
    }

    @Transactional
    public void deleteCategory(Long id) {
        String categoryTable = categoryTable();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM artwork WHERE category_id = ?", Integer.class, id);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("该分类下仍有关联作品，无法删除");
        }
        jdbcTemplate.update("DELETE FROM " + categoryTable + " WHERE id = ?", id);
    }

    public Map<String, Object> listAuditRecords(String auditStatus, int page, int size) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (auditStatus != null && !auditStatus.isBlank()) {
            where.append(" AND a.status = ?");
            args.add(mapAuditStatus(auditStatus));
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM artwork a" + where,
            Long.class,
            args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT a.id, a.title, a.author_name, a.cover_image, a.price, a.description,
                   a.size, a.art_type, a.status, a.create_time, a.reject_reason,
                   c.name AS category_name
            FROM artwork a
            LEFT JOIN %s c ON a.category_id = c.id
            """.formatted(categoryTable()) + where + " ORDER BY a.create_time DESC, a.id DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("artworkId", row.get("id"));
            item.put("title", row.get("title"));
            item.put("artistName", row.get("author_name"));
            item.put("cover", row.get("cover_image"));
            item.put("categoryName", row.get("category_name"));
            item.put("price", row.get("price"));
            item.put("size", row.get("size"));
            item.put("material", row.get("art_type"));
            item.put("description", row.get("description"));
            item.put("createTime", formatDateTime(row.get("create_time")));
            item.put("submitTime", formatDateTime(row.get("create_time")));
            item.put("auditStatus", mapAuditStatusText(toInt(row.get("status"), 0)));
            item.put("rejectReason", row.get("reject_reason"));
            list.add(item);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("list", list);
        payload.put("total", total == null ? 0 : total);
        payload.put("page", page);
        payload.put("size", size);
        return payload;
    }

    @Transactional
    public void approveArtwork(Long artworkId) {
        executeArtworkAuditUpdate(artworkId, 1, null);
    }

    @Transactional
    public void rejectArtwork(Long artworkId, String reason) {
        executeArtworkAuditUpdate(artworkId, 4, reason);
    }

    private int mapAuditStatus(String auditStatus) {
        return switch (auditStatus) {
            case "approved" -> 1;
            case "rejected" -> 4;
            default -> 0;
        };
    }

    private String mapAuditStatusText(int status) {
        return switch (status) {
            case 1 -> "approved";
            case 4 -> "rejected";
            default -> "pending";
        };
    }

    private String categoryTable() {
        return schemaInspector.resolveTable("artworkCategory", "artwork_category");
    }

    private String categoryWeightColumn() {
        return schemaInspector.firstExistingColumn(categoryTable(), "weight", "sort");
    }

    private String categoryIconColumn() {
        return schemaInspector.firstExistingColumn(categoryTable(), "icon");
    }

    private String nullableText(Object value) {
        String text = Objects.toString(value, "").trim();
        return text.isEmpty() ? null : text;
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

    private String formatDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME_FORMATTER);
        }
        return Objects.toString(value, "");
    }

    private void executeArtworkAuditUpdate(Long artworkId, int status, String reason) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("UPDATE artwork SET status = ?");
        args.add(status);
        if (schemaInspector.hasColumn("artwork", "reject_reason")) {
            sql.append(", reject_reason = ?");
            args.add(reason);
        }
        if (schemaInspector.hasColumn("artwork", "audit_time")) {
            sql.append(", audit_time = ?");
            args.add(LocalDateTime.now());
        }
        if (schemaInspector.hasColumn("artwork", "update_time")) {
            sql.append(", update_time = ?");
            args.add(LocalDateTime.now());
        }
        sql.append(" WHERE id = ?");
        args.add(artworkId);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }
}
