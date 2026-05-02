package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private String firstAvailableColumn(String tableName, String... candidates) {
        for (String candidate : candidates) {
            if (schemaInspector.hasColumn(tableName, candidate)) {
                return candidate;
            }
        }
        return null;
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

    private boolean toBool(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = value.toString().trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private int toCommissionPercent(Object value) {
        if (value == null) {
            return 10;
        }
        BigDecimal rate;
        if (value instanceof BigDecimal decimal) {
            rate = decimal;
        } else if (value instanceof Number number) {
            rate = BigDecimal.valueOf(number.doubleValue());
        } else {
            try {
                rate = new BigDecimal(value.toString());
            } catch (NumberFormatException ex) {
                return 10;
            }
        }
        if (rate.compareTo(BigDecimal.ONE) <= 0) {
            rate = rate.multiply(BigDecimal.valueOf(100));
        }
        return rate.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
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

    /**
     * 创建作品（支持自动创建艺术家）
     * @param params 作品参数，包含艺术家名称等
     * @return 创建的作品ID
     */
    @Transactional
    public Long createArtwork(Map<String, Object> params) {
        String artistName = Objects.toString(params.get("artistName"), "").trim();
        if (artistName.isEmpty()) {
            throw new IllegalArgumentException("艺术家名称不能为空");
        }

        // 1. 查找或创建艺术家
        Long authorId = findOrCreateArtist(artistName);

        // 2. 获取艺术家编号
        String artistCode = findArtistCodeByUserId(authorId);
        String authorUid = findAuthorUidByUserId(authorId);

        // 3. 构建作品SQL
        LocalDateTime now = LocalDateTime.now();
        StringBuilder sql = new StringBuilder("INSERT INTO artwork (title, author_id, author_name");
        StringBuilder values = new StringBuilder("VALUES (?, ?, ?");

        List<Object> args = new ArrayList<>();
        args.add(Objects.toString(params.get("title"), ""));
        args.add(authorId);
        args.add(artistName);

        if (schemaInspector.hasColumn("artwork", "author_uid")) {
            sql.append(", author_uid");
            values.append(", ?");
            args.add(authorUid);
        }

        // 可选字段
        if (schemaInspector.hasColumn("artwork", "category_id")) {
            sql.append(", category_id");
            values.append(", ?");
            Object categoryId = params.get("categoryId");
            args.add(categoryId != null ? ((Number) categoryId).longValue() : null);
        }

        if (schemaInspector.hasColumn("artwork", "category_name")) {
            sql.append(", category_name");
            values.append(", ?");
            args.add(Objects.toString(params.get("categoryName"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "cover_image")) {
            sql.append(", cover_image");
            values.append(", ?");
            args.add(Objects.toString(params.get("coverImage"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "images")) {
            sql.append(", images");
            values.append(", ?");
            args.add(Objects.toString(params.get("images"), "[]"));
        }

        if (schemaInspector.hasColumn("artwork", "description")) {
            sql.append(", description");
            values.append(", ?");
            args.add(Objects.toString(params.get("description"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "daily_view_count")) {
            sql.append(", daily_view_count");
            values.append(", ?");
            args.add(Math.max(toInt(params.get("dailyViewCount"), 0), 0));
        }

        if (schemaInspector.hasColumn("artwork", "daily_like_count")) {
            sql.append(", daily_like_count");
            values.append(", ?");
            args.add(Math.max(toInt(params.get("dailyLikeCount"), 0), 0));
        }

        if (schemaInspector.hasColumn("artwork", "price")) {
            sql.append(", price");
            values.append(", ?");
            Object price = params.get("price");
            if (price instanceof Number) {
                args.add(new BigDecimal(price.toString()));
            } else if (price != null) {
                args.add(new BigDecimal(Objects.toString(price, "0")));
            } else {
                args.add(BigDecimal.ZERO);
            }
        }

        if (schemaInspector.hasColumn("artwork", "size")) {
            sql.append(", size");
            values.append(", ?");
            args.add(Objects.toString(params.get("size"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "material")) {
            sql.append(", material");
            values.append(", ?");
            args.add(Objects.toString(params.get("material"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "art_type")) {
            sql.append(", art_type");
            values.append(", ?");
            args.add(Objects.toString(params.get("artType"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "style")) {
            sql.append(", style");
            values.append(", ?");
            args.add(Objects.toString(params.get("style"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "region")) {
            sql.append(", region");
            values.append(", ?");
            args.add(Objects.toString(params.get("region"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "tags")) {
            sql.append(", tags");
            values.append(", ?");
            args.add(Objects.toString(params.get("tags"), ""));
        }

        if (schemaInspector.hasColumn("artwork", "status")) {
            sql.append(", status");
            values.append(", ?");
            // 管理员创建默认直接上架
            args.add(toInt(params.get("status"), 1));
        } else {
            // 如果没有status字段，默认为1（审核通过/上架）
            args.add(1);
        }

        if (schemaInspector.hasColumn("artwork", "create_time")) {
            sql.append(", create_time");
            values.append(", ?");
            args.add(now);
        }

        if (schemaInspector.hasColumn("artwork", "update_time")) {
            sql.append(", update_time");
            values.append(", ?");
            args.add(now);
        }

        sql.append(") ").append(values).append(")");

        jdbcTemplate.update(sql.toString(), args.toArray());

        Long artworkId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 4. 记录日志
        org.slf4j.LoggerFactory.getLogger(getClass()).info("创建作品成功: id={}, title={}, author={}, authorId={}, artistCode={}",
            artworkId, params.get("title"), artistName, authorId, artistCode);

        return artworkId;
    }

    /**
     * 查找或创建艺术家
     * @param artistName 艺术家名称
     * @return 用户ID
     */
    private Long findOrCreateArtist(String artistName) {
        // 1. 先通过当前艺术家列表查找
        Long existingArtistId = findArtistByName(artistName);
        if (existingArtistId != null) {
            return existingArtistId;
        }

        // 2. 再通过用户昵称精确查找，找到后补齐艺术家列表
        String userTable = authorLookupUserTable();
        if (schemaInspector.hasColumn(userTable, "nickname")) {
            try {
                Long userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM " + userTable + " WHERE BINARY nickname = BINARY ? ORDER BY id DESC LIMIT 1",
                    Long.class,
                    artistName
                );
                if (userId != null) {
                    ensureUserHasArtistIdentity(userId);
                    ensureArtistProfile(userId, artistName, LocalDateTime.now());
                    return userId;
                }
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                // 没有找到匹配用户
            }
        }

        // 3. 自动创建新艺术家
        return createNewArtist(artistName);
    }

    /**
     * 根据艺术家名称查找艺术家认证记录
     */
    private Long findArtistByName(String artistName) {
        try {
            String artistTable = schemaInspector.resolveTable("product_artist_profile", "artist_profile", "artist_certifications");
            String userTable = authorLookupUserTable();
            String nameExpr = schemaInspector.hasColumn(artistTable, "real_name") && schemaInspector.hasColumn(artistTable, "artist_name")
                ? "COALESCE(ap.real_name, ap.artist_name)"
                : (schemaInspector.hasColumn(artistTable, "real_name") ? "ap.real_name" : "ap.artist_name");
            String sql =
                "SELECT ap.user_id FROM " + artistTable + " ap " +
                "LEFT JOIN " + userTable + " u ON ap.user_id = u.id " +
                "WHERE BINARY " + nameExpr + " = BINARY ? OR BINARY u.nickname = BINARY ? " +
                "ORDER BY ap.id DESC LIMIT 1";
            return jdbcTemplate.queryForObject(sql, Long.class, artistName, artistName);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            // 表或字段不存在时返回null
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("查找艺术家失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 确保用户具有艺术家身份
     */
    private void ensureUserHasArtistIdentity(Long userId) {
        String userTable = authorLookupUserTable();
        String identityColumn = identityColumn(userTable);
        if (!schemaInspector.hasColumn(userTable, identityColumn)) {
            return;
        }

        // 获取当前身份
        String currentIdentity = jdbcTemplate.queryForObject(
            "SELECT " + identityColumn + " FROM " + userTable + " WHERE id = ?",
            String.class, userId
        );

        // 如果没有艺术家身份，添加
        if (currentIdentity == null || !currentIdentity.contains("artist")) {
            String newIdentity = currentIdentity == null || currentIdentity.isEmpty()
                ? "artist"
                : currentIdentity + ",artist";

            jdbcTemplate.update(
                "UPDATE " + userTable + " SET " + identityColumn + " = ?, update_time = ? WHERE id = ?",
                newIdentity, LocalDateTime.now(), userId
            );
        }
    }

    /**
     * 创建新艺术家（同时创建用户和艺术家认证记录）
     */
    private Long createNewArtist(String artistName) {
        String userTable = authorLookupUserTable();
        LocalDateTime now = LocalDateTime.now();
        String userUid = generateInlineUserUid();

        String nicknameCol = schemaInspector.firstExistingColumn(userTable, "nickname", "name");
        String createTimeCol = schemaInspector.firstExistingColumn(userTable, "created_at", "create_time");
        String updateTimeCol = schemaInspector.firstExistingColumn(userTable, "updated_at", "update_time");
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(userTable).append(" (").append(nicknameCol);
        StringBuilder values = new StringBuilder("VALUES (?");

        List<Object> args = new ArrayList<>();
        args.add(artistName);

        if (schemaInspector.hasColumn(userTable, "status")) {
            sql.append(", status");
            values.append(", ?");
            args.add("user_account".equals(userTable) ? "ENABLED" : 1);
        }
        if (schemaInspector.hasColumn(userTable, "register_source")) {
            sql.append(", register_source");
            values.append(", ?");
            args.add("ADMIN");
        }
        if (schemaInspector.hasColumn(userTable, "user_no")) {
            sql.append(", user_no");
            values.append(", ?");
            args.add("U" + System.currentTimeMillis());
        }
        if (schemaInspector.hasColumn(userTable, "uid")) {
            sql.append(", uid");
            values.append(", ?");
            args.add(userUid);
        }
        if (schemaInspector.hasColumn(userTable, "user_uid")) {
            sql.append(", user_uid");
            values.append(", ?");
            args.add(userUid);
        }
        if (schemaInspector.hasColumn(userTable, "identity")) {
            sql.append(", identity");
            values.append(", ?");
            args.add("artist");
        }
        if (schemaInspector.hasColumn(userTable, "identity_json")) {
            sql.append(", identity_json");
            values.append(", ?");
            args.add("[\"artist\"]");
        }
        if (schemaInspector.hasColumn(userTable, createTimeCol)) {
            sql.append(", ").append(createTimeCol);
            values.append(", ?");
            args.add(now);
        }
        if (schemaInspector.hasColumn(userTable, updateTimeCol)) {
            sql.append(", ").append(updateTimeCol);
            values.append(", ?");
            args.add(now);
        }
        
        sql.append(") ").append(values).append(")");
        jdbcTemplate.update(sql.toString(), args.toArray());

        Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        ensureArtistProfile(userId, artistName, now);

        org.slf4j.LoggerFactory.getLogger(getClass()).info("自动创建艺术家成功: userId={}, artistName={}",
            userId, artistName);

        return userId;
    }

    /**
     * 创建艺术家认证记录（安全实现，兼容不同表结构）
     */
    private void ensureArtistProfile(Long userId, String artistName, LocalDateTime now) {
        String artistTable = schemaInspector.resolveTable("product_artist_profile", "artist_profile", "artist_certifications");
        try {
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + artistTable + " WHERE user_id = ?",
                Integer.class,
                userId
            );
            if (exists != null && exists > 0) {
                return;
            }
        } catch (Exception ignored) {
        }

        String userUid = findAuthorUidByUserId(userId);
        String artistCode = generateInlineArtistCode();
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(artistTable).append(" (user_id");
        StringBuilder values = new StringBuilder("VALUES (?");
        List<Object> args = new ArrayList<>();
        args.add(userId);

        if (schemaInspector.hasColumn(artistTable, "user_uid")) {
            sql.append(", user_uid");
            values.append(", ?");
            args.add(userUid);
        }
        if (schemaInspector.hasColumn(artistTable, "artist_name")) {
            sql.append(", artist_name");
            values.append(", ?");
            args.add(artistName);
        }
        if (schemaInspector.hasColumn(artistTable, "real_name")) {
            sql.append(", real_name");
            values.append(", ?");
            args.add(artistName);
        }
        if (schemaInspector.hasColumn(artistTable, "artist_code")) {
            sql.append(", artist_code");
            values.append(", ?");
            args.add(artistCode);
        }
        if (schemaInspector.hasColumn(artistTable, "is_signed")) {
            sql.append(", is_signed");
            values.append(", ?");
            args.add(1);
        }
        if (schemaInspector.hasColumn(artistTable, "status")) {
            sql.append(", status");
            values.append(", ?");
            args.add("artist_profile".equals(artistTable) ? "ACTIVE" : 1);
        }
        if (schemaInspector.hasColumn(artistTable, "cert_status")) {
            sql.append(", cert_status");
            values.append(", ?");
            args.add(1);
        }
        String createTimeCol = schemaInspector.firstExistingColumn(artistTable, "created_at", "create_time");
        if (schemaInspector.hasColumn(artistTable, createTimeCol)) {
            sql.append(", ").append(createTimeCol);
            values.append(", ?");
            args.add(now);
        }
        String updateTimeCol = schemaInspector.firstExistingColumn(artistTable, "updated_at", "update_time");
        if (schemaInspector.hasColumn(artistTable, updateTimeCol)) {
            sql.append(", ").append(updateTimeCol);
            values.append(", ?");
            args.add(now);
        }

        sql.append(") ").append(values).append(")");
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    /**
     * 生成用户UID（内联版本）
     * 格式: USR + yyyyMMdd + 4位序列号 + 4位随机码 = 19位
     */
    private String generateInlineUserUid() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 获取今日注册数量作为序列号基础
        Long todayCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + authorLookupUserTable() + " WHERE DATE(" +
                schemaInspector.firstExistingColumn(authorLookupUserTable(), "created_at", "create_time") + ") = CURDATE()",
            Long.class
        );
        int seq = (int)((todayCount != null ? todayCount : 0) % 10000);
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "USR" + date + String.format("%04d", seq + 1) + random;
    }

    /**
     * 生成艺术家编号（内联版本）
     * 格式: ART + yyyyMMdd + 4位序列号 + 4位随机码 = 19位
     */
    private String generateInlineArtistCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 获取今日艺术家认证数量作为序列号基础
        String artistTable = schemaInspector.resolveTable("product_artist_profile", "artist_profile", "artist_certifications");
        String createTimeCol = schemaInspector.firstExistingColumn(artistTable, "created_at", "create_time");
        Long todayCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + artistTable + " WHERE DATE(" + createTimeCol + ") = CURDATE()",
            Long.class
        );
        int seq = (int)((todayCount != null ? todayCount : 0) % 10000);
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "ART" + date + String.format("%04d", seq + 1) + random;
    }

    /**
     * 根据用户ID查找艺术家编号
     */
    private String findArtistCodeByUserId(Long userId) {
        try {
            String artistTable = schemaInspector.resolveTable("product_artist_profile", "artist_profile", "artist_certifications");
            if (schemaInspector.hasColumn(artistTable, "artist_code")) {
                return jdbcTemplate.queryForObject(
                    "SELECT artist_code FROM " + artistTable + " WHERE user_id = ? LIMIT 1",
                    String.class, userId
                );
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 没有找到
        }
        return null;
    }

    private String findAuthorUidByUserId(Long userId) {
        String userTable = authorLookupUserTable();
        List<String> uidColumns = new ArrayList<>();
        for (String candidate : List.of("uid", "user_uid", "user_no")) {
            if (schemaInspector.hasColumn(userTable, candidate)) {
                uidColumns.add(candidate);
            }
        }
        if (uidColumns.isEmpty()) {
            return userId == null ? null : String.valueOf(userId);
        }
        String uidExpression = uidColumns.size() == 1 ? uidColumns.get(0) : "COALESCE(" + String.join(", ", uidColumns) + ")";
        try {
            return jdbcTemplate.queryForObject(
                "SELECT " + uidExpression + " FROM " + userTable + " WHERE id = ? LIMIT 1",
                String.class,
                userId
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return userId == null ? null : String.valueOf(userId);
        }
    }

    /**
     * 获取用户表名
     */
    private String userTable() {
        return authorLookupUserTable();
    }

    private String authorLookupUserTable() {
        return schemaInspector.resolveTable("author_lookup_user", "user_account", "sys_user");
    }

    private String userUidColumn() {
        String userTable = authorLookupUserTable();
        return schemaInspector.firstExistingColumn(userTable, "uid", "user_uid", "user_no");
    }

    /**
     * 获取用户身份列名
     */
    private String identityColumn(String userTable) {
        return schemaInspector.firstExistingColumn(userTable, "identity", "identity_json");
    }

    /**
     * 作品列表查询
     */
    public Map<String, Object> listProducts(
        String keyword,
        String id,
        String artworkCode,
        String title,
        String authorName,
        Long categoryId,
        String artType,
        String status,
        int page,
        int size
    ) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (a.title LIKE ? OR a.author_name LIKE ?");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
            if (schemaInspector.hasColumn("artwork", "artwork_code")) {
                where.append(" OR a.artwork_code LIKE ?");
                args.add("%" + keyword + "%");
            }
            if (schemaInspector.hasColumn("artist_profile", "user_uid")) {
                where.append(" OR ap.user_uid LIKE ?");
                args.add("%" + keyword + "%");
            }
            where.append(")");
        }

        if (id != null && !id.isBlank()) {
            where.append(" AND a.id = ?");
            args.add(Long.parseLong(id));
        }

        if (artworkCode != null && !artworkCode.isBlank() && schemaInspector.hasColumn("artwork", "artwork_code")) {
            where.append(" AND a.artwork_code LIKE ?");
            args.add("%" + artworkCode.trim() + "%");
        }

        if (title != null && !title.isBlank()) {
            where.append(" AND a.title LIKE ?");
            args.add("%" + title.trim() + "%");
        }

        if (authorName != null && !authorName.isBlank()) {
            where.append(" AND a.author_name LIKE ?");
            args.add("%" + authorName.trim() + "%");
        }
        
        if (categoryId != null) {
            where.append(" AND a.category_id = ?");
            args.add(categoryId);
        }

        if (artType != null && !artType.isBlank()) {
            where.append(" AND a.art_type = ?");
            args.add(artType);
        }
        
        if (status != null && !status.isBlank()) {
            where.append(" AND a.status = ?");
            args.add(mapProductStatus(status));
        }
        
        // 获取总数
        boolean hasArtistProfileUid = schemaInspector.hasColumn("artist_profile", "user_uid");
        String artistProfileJoin = hasArtistProfileUid
            ? " LEFT JOIN artist_profile ap ON ap.user_id = a.author_id"
            : "";
        String artistProfileUidSelect = hasArtistProfileUid ? "ap.user_uid" : "NULL";
        String authorUserTable = authorLookupUserTable();
        boolean hasAuthorUserTable = !schemaInspector.getColumns(authorUserTable).isEmpty();
        String authorUserUidColumn = hasAuthorUserTable
            ? firstAvailableColumn(authorUserTable, "uid", "user_uid", "user_no")
            : null;
        String authorUserJoin = hasAuthorUserTable
            ? " LEFT JOIN " + authorUserTable + " au ON au.id = a.author_id"
            : "";
        String authorUserUidSelect = authorUserUidColumn != null ? "au." + authorUserUidColumn : "NULL";
        boolean hasArtistCertifications = !schemaInspector.getColumns("artist_certifications").isEmpty();
        String artistCertificationJoin = hasArtistCertifications
            ? " LEFT JOIN artist_certifications ac ON ac.user_id = a.author_id"
            : "";
        String artistCodeSelect = hasArtistCertifications && schemaInspector.hasColumn("artist_certifications", "artist_code")
            ? "ac.artist_code"
            : "NULL";
        String authorJoins = artistProfileJoin + authorUserJoin + artistCertificationJoin;

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM artwork a" + authorJoins + where,
            Long.class,
            args.toArray()
        );
        
        // 分页查询
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        String artworkCodeSelect = schemaInspector.hasColumn("artwork", "artwork_code") ? "a.artwork_code" : "NULL";
        String authorUidSelect = schemaInspector.hasColumn("artwork", "author_uid")
            ? "COALESCE(a.author_uid, " + authorUserUidSelect + ", " + artistProfileUidSelect + ", " + artistCodeSelect + ", LPAD(a.author_id, 4, '0'))"
            : "COALESCE(" + authorUserUidSelect + ", " + artistProfileUidSelect + ", " + artistCodeSelect + ", LPAD(a.author_id, 4, '0'))";
        String yearSelect = schemaInspector.hasColumn("artwork", "year") ? "a.year" :
            (schemaInspector.hasColumn("artwork", "create_year") ? "a.create_year" : "NULL");
        String favoriteCountSelect = schemaInspector.hasColumn("artwork", "favorite_count") ? "a.favorite_count" : "0";
        String viewCountSelect = schemaInspector.hasColumn("artwork", "view_count") ? "a.view_count" : "0";
        String descriptionSelect = schemaInspector.hasColumn("artwork", "description") ? "a.description" : "NULL";
        String dailyViewCountSelect = schemaInspector.hasColumn("artwork", "daily_view_count") ? "a.daily_view_count" : "0";
        String dailyLikeCountSelect = schemaInspector.hasColumn("artwork", "daily_like_count") ? "a.daily_like_count" : "0";
        boolean hasWeightColumn = schemaInspector.hasColumn("artwork", "weight");
        String weightSelect = hasWeightColumn ? "a.weight" : "0";
        String ownershipTypeSelect = schemaInspector.hasColumn("artwork", "ownership_type") ? "a.ownership_type" : "1";
        String distributionSelect = schemaInspector.hasColumn("artwork", "distribution_enabled") ? "a.distribution_enabled" :
            (schemaInspector.hasColumn("artwork", "distributable") ? "a.distributable" : "0");
        String commissionRateSelect = schemaInspector.hasColumn("artwork", "commission_rate") ? "a.commission_rate" : "10";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT a.id, a.title, a.author_name, a.cover_image, a.price, a.status,
                   a.author_id, %s AS artwork_code, %s AS author_uid, a.size, %s AS artwork_year,
                   a.art_type, %s AS favorite_count, %s AS weight_value, %s AS ownership_type,
                   %s AS distribution_enabled, %s AS commission_rate, %s AS view_count,
                   %s AS description, %s AS daily_view_count, %s AS daily_like_count,
                   a.create_time, c.name AS category_name
            FROM artwork a
            %s
            LEFT JOIN %s c ON a.category_id = c.id
            """.formatted(
                artworkCodeSelect,
                authorUidSelect,
                yearSelect,
                favoriteCountSelect,
                weightSelect,
                ownershipTypeSelect,
                distributionSelect,
                commissionRateSelect,
                viewCountSelect,
                descriptionSelect,
                dailyViewCountSelect,
                dailyLikeCountSelect,
                authorJoins,
                categoryTable()
            ) + where + " ORDER BY " + (hasWeightColumn ? "a.weight DESC, " : "") + "a.create_time DESC, a.id DESC LIMIT ?, ?",
            queryArgs.toArray()
        );
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("artworkId", row.get("id"));
            item.put("displayArtworkId", String.format("%04d", toInt(row.get("id"), 0)));
            item.put("artworkCode", row.get("artwork_code"));
            item.put("artworkUid", row.get("artwork_code"));
            item.put("authorId", row.get("author_id"));
            item.put("displayAuthorId", row.get("author_uid"));
            item.put("authorUid", row.get("author_uid"));
            item.put("title", row.get("title"));
            item.put("artistName", row.get("author_name"));
            item.put("authorName", row.get("author_name"));
            item.put("cover", row.get("cover_image"));
            item.put("price", row.get("price"));
            item.put("categoryName", row.get("category_name"));
            item.put("artType", row.get("art_type"));
            item.put("size", row.get("size"));
            item.put("year", row.get("artwork_year"));
            item.put("favoriteCount", toInt(row.get("favorite_count"), 0));
            item.put("viewCount", toInt(row.get("view_count"), 0));
            item.put("description", row.get("description"));
            item.put("dailyViewCount", toInt(row.get("daily_view_count"), 0));
            item.put("dailyLikeCount", toInt(row.get("daily_like_count"), 0));
            item.put("displayViewCount", toInt(row.get("view_count"), 0) + toInt(row.get("daily_view_count"), 0));
            item.put("displayLikeCount", toInt(row.get("favorite_count"), 0) + toInt(row.get("daily_like_count"), 0));
            item.put("weight", toInt(row.get("weight_value"), 0));
            item.put("ownershipType", toInt(row.get("ownership_type"), 1));
            item.put("status", toInt(row.get("status"), 0));
            item.put("distributionEnabled", toBool(row.get("distribution_enabled")));
            item.put("commissionRate", toCommissionPercent(row.get("commission_rate")));
            item.put("createTime", formatDateTime(row.get("create_time")));
            list.add(item);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total != null ? total : 0);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
    
    private int mapProductStatus(String status) {
        return switch (status) {
            case "1", "active", "approved" -> 1;
            case "0", "inactive", "rejected" -> 0;
            default -> 0;
        };
    }
    
    private String mapProductStatusText(int status) {
        return switch (status) {
            case 1 -> "active";
            default -> "inactive";
        };
    }
}
