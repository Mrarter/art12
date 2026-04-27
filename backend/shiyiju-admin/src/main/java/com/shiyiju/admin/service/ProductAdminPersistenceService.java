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

        // 3. 构建作品SQL
        LocalDateTime now = LocalDateTime.now();
        StringBuilder sql = new StringBuilder("INSERT INTO artwork (title, author_id, author_name");
        StringBuilder values = new StringBuilder("VALUES (?, ?, ?");

        List<Object> args = new ArrayList<>();
        args.add(Objects.toString(params.get("title"), ""));
        args.add(authorId);
        args.add(artistName);

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
        // 1. 先通过艺术家认证表查找
        Long existingArtistId = findArtistByName(artistName);
        if (existingArtistId != null) {
            return existingArtistId;
        }

        // 2. 再通过用户名模糊查找
        String userTable = userTable();
        String identityColumn = identityColumn(userTable);

        String searchSql = String.format(
            "SELECT id FROM %s WHERE nickname LIKE ? AND %s LIKE '%%artist%%' LIMIT 1",
            userTable, identityColumn
        );
        try {
            Long userId = jdbcTemplate.queryForObject(searchSql, Long.class, "%" + artistName + "%");
            if (userId != null) {
                // 确保该用户有艺术家身份
                ensureUserHasArtistIdentity(userId);
                return userId;
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 没有找到匹配的艺术家
        }

        // 3. 自动创建新艺术家
        return createNewArtist(artistName);
    }

    /**
     * 根据艺术家名称查找艺术家认证记录
     */
    private Long findArtistByName(String artistName) {
        try {
            // 尝试通过真实姓名查找（使用id排序避免字段不存在问题）
            String sql = 
                "SELECT ac.user_id FROM artist_certifications ac " +
                "JOIN sys_user u ON ac.user_id = u.id " +
                "WHERE ac.real_name = ? OR u.nickname = ? " +
                "ORDER BY ac.id DESC LIMIT 1";
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
        String userTable = userTable();
        String identityColumn = identityColumn(userTable);

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
        String userTable = userTable();
        LocalDateTime now = LocalDateTime.now();

        // 1. 构建用户SQL（避免identity列重复）
        String nicknameCol = schemaInspector.firstExistingColumn(userTable, "nickname");
        
        // 检查identity相关列的实际情况
        boolean hasIdentityCol = schemaInspector.hasColumn(userTable, "identity");
        boolean hasIdentityJsonCol = schemaInspector.hasColumn(userTable, "identity_json");
        
        org.slf4j.LoggerFactory.getLogger(getClass()).info("创建艺术家: hasIdentity={}, hasIdentityJson={}", hasIdentityCol, hasIdentityJsonCol);
        
        // 构建SQL
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(userTable)
            .append(" (").append(nicknameCol).append(", create_time, update_time");
        StringBuilder values = new StringBuilder("VALUES (?, ?, ?");
        
        List<Object> args = new ArrayList<>();
        args.add(artistName);  // nickname
        args.add(now);         // create_time
        args.add(now);         // update_time
        
        // 添加identity字段（如果存在）
        if (hasIdentityCol) {
            sql.append(", identity");
            values.append(", ?");
            args.add("artist");
        }
        
        // 添加identity_json字段（如果存在）
        if (hasIdentityJsonCol) {
            sql.append(", identity_json");
            values.append(", ?");
            args.add("[\"artist\"]");
        }
        
        sql.append(") ").append(values).append(")");
        String finalSql = sql.toString();
        org.slf4j.LoggerFactory.getLogger(getClass()).info("执行SQL: {}", finalSql);
        jdbcTemplate.update(finalSql, args.toArray());

        Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 2. 生成用户UID（仅当uid字段存在时）
        if (schemaInspector.hasColumn(userTable, "uid")) {
            String userUid = generateInlineUserUid();
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET uid = ? WHERE id = ?",
                userUid, userId
            );
            org.slf4j.LoggerFactory.getLogger(getClass()).info("生成用户UID: {}", userUid);
        }

        // 3. 尝试创建艺术家认证记录
        createArtistCertification(userId, artistName, now);

        org.slf4j.LoggerFactory.getLogger(getClass()).info("自动创建艺术家成功: userId={}, artistName={}",
            userId, artistName);

        return userId;
    }

    /**
     * 创建艺术家认证记录（安全实现，兼容不同表结构）
     */
    private void createArtistCertification(Long userId, String artistName, LocalDateTime now) {
        try {
            // 生成艺术家编号
            String artistCode = generateInlineArtistCode();

            // 检查字段并构建合适的SQL
            boolean hasArtistCode = schemaInspector.hasColumn("artist_certifications", "artist_code");
            boolean hasCertStatus = schemaInspector.hasColumn("artist_certifications", "cert_status");
            boolean hasCreateTime = schemaInspector.hasColumn("artist_certifications", "create_time");
            boolean hasUpdateTime = schemaInspector.hasColumn("artist_certifications", "update_time");

            StringBuilder sql = new StringBuilder("INSERT INTO artist_certifications (user_id, real_name");
            StringBuilder values = new StringBuilder("VALUES (?, ?");

            List<Object> args = new ArrayList<>();
            args.add(userId);
            args.add(artistName);

            if (hasArtistCode) {
                sql.append(", artist_code");
                values.append(", ?");
                args.add(artistCode);
            }

            if (hasCertStatus) {
                sql.append(", cert_status");
                values.append(", ?");
                args.add(1); // 默认认证通过
            }

            if (hasCreateTime) {
                sql.append(", create_time");
                values.append(", ?");
                args.add(now);
            }

            if (hasUpdateTime) {
                sql.append(", update_time");
                values.append(", ?");
                args.add(now);
            }

            sql.append(") ").append(values).append(")");
            jdbcTemplate.update(sql.toString(), args.toArray());

        } catch (Exception e) {
            // 记录但不要抛出异常，用户记录已创建成功
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("创建艺术家认证记录失败: {}", e.getMessage());
        }
    }

    /**
     * 生成用户UID（内联版本）
     * 格式: USR + yyyyMMdd + 4位序列号 + 4位随机码 = 19位
     */
    private String generateInlineUserUid() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 获取今日注册数量作为序列号基础
        Long todayCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + userTable() + " WHERE DATE(create_time) = CURDATE()",
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
        Long todayCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM artist_certifications WHERE DATE(create_time) = CURDATE()",
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
            if (schemaInspector.hasColumn("artist_certifications", "artist_code")) {
                return jdbcTemplate.queryForObject(
                    "SELECT artist_code FROM artist_certifications WHERE user_id = ? LIMIT 1",
                    String.class, userId
                );
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 没有找到
        }
        return null;
    }

    /**
     * 获取用户表名
     */
    private String userTable() {
        return schemaInspector.resolveTable("sys_user", "sys_user");
    }

    /**
     * 获取用户身份列名
     */
    private String identityColumn(String userTable) {
        return schemaInspector.firstExistingColumn(userTable, "identity", "identity_json");
    }
}
