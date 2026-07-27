package com.shiyiju.admin.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.admin.service.support.SchemaInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员 - 轮播图管理控制器
 */
@RestController
@RequestMapping("/admin/banner")
public class BannerController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaInspector schemaInspector;

    /**
     * 轮播图列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getBannerList(
            @RequestParam(required = false) String type) {
        String linkTypeColumn = firstExistingColumn("banner", "link_type", "type");
        String linkValueColumn = firstExistingColumn("banner", "link_value", "target");
        String sortColumn = firstExistingColumn("banner", "sort_no", "sort");
        String createTimeColumn = schemaInspector.hasColumn("banner", "create_time") ? "create_time" : "NULL";
        
        List<Map<String, Object>> banners = jdbcTemplate.queryForList(
            String.format(
                "SELECT id, title, image_url, %s AS link_type, %s AS link_value, %s AS sort_no, status, %s AS create_time FROM banner ORDER BY %s DESC, id DESC",
                linkTypeColumn,
                linkValueColumn,
                sortColumn,
                createTimeColumn,
                sortColumn
            )
        );
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> b : banners) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", b.get("id"));
            item.put("title", b.get("title"));
            item.put("imageUrl", b.get("image_url"));
            item.put("type", b.get("link_type"));
            item.put("target", b.get("link_value"));
            item.put("sortNo", b.get("sort_no"));
            item.put("status", normalizeStatus(b.get("status")));
            item.put("createTime", b.get("create_time"));
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 创建轮播图
     */
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        String title = (String) params.get("title");
        String imageUrl = (String) params.get("imageUrl");
        String linkType = (String) params.getOrDefault("type", "BANNER");
        String linkValue = (String) params.getOrDefault("target", "");
        Integer sort = params.get("sortNo") != null ? ((Number) params.get("sortNo")).intValue() : 0;
        String status = (String) params.getOrDefault("status", "ENABLED");
        
        LocalDateTime now = LocalDateTime.now();
        List<String> columns = new ArrayList<>(List.of("title", "image_url"));
        List<Object> args = new ArrayList<>(List.of(title, imageUrl));
        addColumnValue(columns, args, firstExistingColumn("banner", "link_type", "type"), linkType);
        addColumnValue(columns, args, firstExistingColumn("banner", "link_value", "target"), linkValue);
        addColumnValue(columns, args, firstExistingColumn("banner", "sort_no", "sort"), sort);
        addColumnValue(columns, args, "status", statusDbValue(status));
        if (schemaInspector.hasColumn("banner", "create_time")) {
            addColumnValue(columns, args, "create_time", now);
        }
        if (schemaInspector.hasColumn("banner", "update_time")) {
            addColumnValue(columns, args, "update_time", now);
        }

        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        jdbcTemplate.update(
            "INSERT INTO banner (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")",
            args.toArray()
        );
        
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Result.success(id);
    }

    /**
     * 更新轮播图
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("UPDATE banner SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        
        if (params.containsKey("title")) {
            sql.append(", title = ?");
            args.add(params.get("title"));
        }
        if (params.containsKey("imageUrl")) {
            sql.append(", image_url = ?");
            args.add(params.get("imageUrl"));
        }
        if (params.containsKey("type")) {
            sql.append(", link_type = ?");
            replaceLastAssignment(sql, firstExistingColumn("banner", "link_type", "type"));
            args.add(params.get("type"));
        }
        if (params.containsKey("target")) {
            sql.append(", link_value = ?");
            replaceLastAssignment(sql, firstExistingColumn("banner", "link_value", "target"));
            args.add(params.get("target"));
        }
        if (params.containsKey("sortNo")) {
            sql.append(", ").append(firstExistingColumn("banner", "sort_no", "sort")).append(" = ?");
            args.add(((Number) params.get("sortNo")).intValue());
        }
        if (params.containsKey("status")) {
            sql.append(", status = ?");
            args.add(statusDbValue(Objects.toString(params.get("status"), "DISABLED")));
        }
        
        sql.append(" WHERE id = ?");
        args.add(id);
        
        jdbcTemplate.update(sql.toString(), args.toArray());
        return Result.success();
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM banner WHERE id = ?", id);
        return Result.success();
    }

    /**
     * 更新轮播图状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        String status = (String) params.get("status");
        Object statusVal = statusDbValue(status);
        jdbcTemplate.update("UPDATE banner SET status = ?, update_time = ? WHERE id = ?", 
            statusVal, LocalDateTime.now(), id);
        return Result.success();
    }

    private String firstExistingColumn(String tableName, String... candidates) {
        return schemaInspector.firstExistingColumn(tableName, candidates);
    }

    private void addColumnValue(List<String> columns, List<Object> args, String column, Object value) {
        if (column != null && schemaInspector.hasColumn("banner", column) && !columns.contains(column)) {
            columns.add(column);
            args.add(value);
        }
    }

    private void replaceLastAssignment(StringBuilder sql, String column) {
        int start = sql.lastIndexOf(", ");
        if (start >= 0) {
            sql.replace(start, sql.length(), ", " + column + " = ?");
        }
    }

    private String normalizeStatus(Object raw) {
        if (raw == null) return "DISABLED";
        if (raw instanceof Number number) {
            return number.intValue() == 1 ? "ENABLED" : "DISABLED";
        }
        String text = Objects.toString(raw, "").trim();
        if ("1".equals(text) || "ENABLED".equalsIgnoreCase(text)) {
            return "ENABLED";
        }
        return "DISABLED";
    }

    private Object statusDbValue(String status) {
        boolean enabled = "ENABLED".equalsIgnoreCase(Objects.toString(status, ""));
        if (isNumericStatusColumn()) {
            return enabled ? 1 : 0;
        }
        return enabled ? "ENABLED" : "DISABLED";
    }

    private boolean isNumericStatusColumn() {
        try {
            String dataType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'banner' AND COLUMN_NAME = 'status'",
                String.class
            );
            return dataType != null && Set.of("tinyint", "smallint", "int", "bigint", "bit").contains(dataType.toLowerCase(Locale.ROOT));
        } catch (Exception ignored) {
            return false;
        }
    }
}
