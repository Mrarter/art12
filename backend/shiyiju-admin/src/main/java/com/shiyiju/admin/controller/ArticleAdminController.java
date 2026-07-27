package com.shiyiju.admin.controller;

import com.shiyiju.common.result.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 管理员 - 文章发布管理
 */
@RestController
@RequestMapping("/admin/content/article")
public class ArticleAdminController {

    private static final String TABLE = "content_article";

    private final JdbcTemplate jdbcTemplate;

    public ArticleAdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        int pageNum = Math.max(page == null ? 1 : page, 1);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), 100);
        int offset = (pageNum - 1) * pageSize;

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        appendFilters(where, args, keyword, status, category);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE + where, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(pageSize);
        queryArgs.add(offset);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, subtitle, author, cover_image, cover_original_image, body_image, category, summary, tags, sort_no, status, publish_time, create_time, update_time "
                        + "FROM " + TABLE + where + " ORDER BY sort_no DESC, COALESCE(publish_time, update_time) DESC, id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", rows.stream().map(this::toArticle).toList());
        result.put("total", total == null ? 0 : total);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + TABLE + " WHERE id = ?",
                id
        );
        if (rows.isEmpty()) {
            return Result.fail("文章不存在");
        }
        return Result.success(toArticle(rows.get(0)));
    }

    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        String title = trim(params.get("title"));
        String content = trim(params.get("content"));
        if (title.isEmpty()) {
            return Result.fail("文章标题不能为空");
        }
        if (content.isEmpty()) {
            return Result.fail("文章正文不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        String status = normalizeStatus(params.get("status"));
        LocalDateTime publishTime = resolvePublishTime(status, params.get("publishTime"), now);
        if ("SCHEDULED".equals(status) && publishTime == null) {
            return Result.fail("请选择定时发布日期");
        }

        jdbcTemplate.update(
                "INSERT INTO " + TABLE + " (title, subtitle, author, cover_image, cover_original_image, body_image, category, summary, content, tags, sort_no, status, publish_time, create_time, update_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                title,
                nullableText(params.get("subtitle")),
                nullableText(params.get("author")),
                nullableText(params.get("coverImage")),
                nullableText(params.get("coverOriginalImage")),
                nullableText(params.get("bodyImage")),
                category(params.get("category")),
                nullableText(params.get("summary")),
                content,
                nullableText(params.get("tags")),
                intValue(params.get("sortNo")),
                status,
                publishTime,
                now,
                now
        );

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id != null) {
            syncArticleBanner(
                    id,
                    title,
                    nullableText(params.get("coverImage")),
                    status,
                    publishTime,
                    intValue(params.get("sortNo"))
            );
        }
        return Result.success(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        if (exists(id) == 0) {
            return Result.fail("文章不存在");
        }

        String title = trim(params.get("title"));
        String content = trim(params.get("content"));
        if (title.isEmpty()) {
            return Result.fail("文章标题不能为空");
        }
        if (content.isEmpty()) {
            return Result.fail("文章正文不能为空");
        }

        String status = normalizeStatus(params.get("status"));
        LocalDateTime publishTime = resolvePublishTime(
                status,
                params.containsKey("publishTime") ? params.get("publishTime") : null,
                LocalDateTime.now()
        );
        if ("SCHEDULED".equals(status) && publishTime == null) {
            return Result.fail("请选择定时发布日期");
        }

        jdbcTemplate.update(
                "UPDATE " + TABLE + " SET title = ?, subtitle = ?, author = ?, cover_image = ?, cover_original_image = ?, body_image = ?, category = ?, summary = ?, content = ?, "
                        + "tags = ?, sort_no = ?, status = ?, publish_time = ?, update_time = ? WHERE id = ?",
                title,
                nullableText(params.get("subtitle")),
                nullableText(params.get("author")),
                nullableText(params.get("coverImage")),
                nullableText(params.get("coverOriginalImage")),
                nullableText(params.get("bodyImage")),
                category(params.get("category")),
                nullableText(params.get("summary")),
                content,
                nullableText(params.get("tags")),
                intValue(params.get("sortNo")),
                status,
                publishTime,
                LocalDateTime.now(),
                id
        );
        syncArticleBanner(
                id,
                title,
                nullableText(params.get("coverImage")),
                status,
                publishTime,
                intValue(params.get("sortNo"))
        );
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        if (exists(id) == 0) {
            return Result.fail("文章不存在");
        }
        String status = normalizeStatus(params.get("status"));
        LocalDateTime now = LocalDateTime.now();
        if (published(status)) {
            jdbcTemplate.update(
                    "UPDATE " + TABLE + " SET status = ?, publish_time = ?, update_time = ? WHERE id = ?",
                    status,
                    now,
                    now,
                    id
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE " + TABLE + " SET status = ?, update_time = ? WHERE id = ?",
                    status,
                    now,
                    id
            );
        }
        syncArticleBannerByArticleId(id);
        return Result.success();
    }

    @PutMapping("/{id}/publish-time")
    public Result<Void> updatePublishTime(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        if (exists(id) == 0) {
            return Result.fail("文章不存在");
        }

        LocalDateTime publishTime = parseDateTime(params.get("publishTime"));
        if (publishTime == null) {
            return Result.fail("请选择有效的发布时间");
        }

        jdbcTemplate.update(
                "UPDATE " + TABLE + " SET publish_time = ?, update_time = ? WHERE id = ?",
                publishTime,
                LocalDateTime.now(),
                id
        );
        syncArticleBannerByArticleId(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM " + TABLE + " WHERE id = ?", id);
        deleteArticleBanner(id);
        return Result.success();
    }

    private void appendFilters(StringBuilder where, List<Object> args, String keyword, String status, String category) {
        String keywordText = trim(keyword);
        if (!keywordText.isEmpty()) {
            where.append(" AND (title LIKE ? OR subtitle LIKE ? OR summary LIKE ? OR tags LIKE ?)");
            String like = "%" + keywordText + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        String statusText = trim(status);
        if (!statusText.isEmpty()) {
            where.append(" AND status = ?");
            args.add(normalizeStatus(statusText));
        }
        String categoryText = trim(category);
        if (!categoryText.isEmpty()) {
            where.append(" AND category = ?");
            args.add(category(categoryText));
        }
    }

    private Map<String, Object> toArticle(Map<String, Object> row) {
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("id", row.get("id"));
        article.put("title", row.get("title"));
        article.put("subtitle", row.get("subtitle"));
        article.put("author", row.get("author"));
        article.put("coverImage", row.get("cover_image"));
        article.put("coverOriginalImage", row.get("cover_original_image"));
        article.put("bodyImage", row.get("body_image"));
        article.put("category", row.get("category"));
        article.put("summary", row.get("summary"));
        article.put("content", row.get("content"));
        article.put("tags", row.get("tags"));
        article.put("sortNo", row.get("sort_no"));
        article.put("status", row.get("status"));
        article.put("publishTime", row.get("publish_time"));
        article.put("createTime", row.get("create_time"));
        article.put("updateTime", row.get("update_time"));
        return article;
    }

    private int exists(Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE + " WHERE id = ?", Integer.class, id);
        return count == null ? 0 : count;
    }

    private String normalizeStatus(Object value) {
        String status = trim(value).toUpperCase(Locale.ROOT);
        if ("PUBLISHED".equals(status) || "OFFLINE".equals(status) || "SCHEDULED".equals(status)) {
            return status;
        }
        return "DRAFT";
    }

    private boolean published(String status) {
        return "PUBLISHED".equalsIgnoreCase(status);
    }

    private LocalDateTime resolvePublishTime(String status, Object publishTime, LocalDateTime now) {
        if (published(status)) {
            LocalDateTime parsed = parseDateTime(publishTime);
            return parsed != null ? parsed : now;
        }
        if ("SCHEDULED".equalsIgnoreCase(status)) {
            return parseDateTime(publishTime);
        }
        return null;
    }

    private String category(Object value) {
        String text = trim(value).toUpperCase(Locale.ROOT);
        return text.isEmpty() ? "APPRECIATION" : text;
    }

    private String nullableText(Object value) {
        String text = trim(value);
        return text.isEmpty() ? null : text;
    }

    private String trim(Object value) {
        return Objects.toString(value, "").trim();
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(trim(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        String text = trim(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void syncArticleBannerByArticleId(Long articleId) {
        if (articleId == null) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, title, cover_image, status, publish_time, sort_no FROM " + TABLE + " WHERE id = ? LIMIT 1",
                articleId
        );
        if (rows.isEmpty()) {
            deleteArticleBanner(articleId);
            return;
        }
        Map<String, Object> row = rows.get(0);
        syncArticleBanner(
                articleId,
                Objects.toString(row.get("title"), ""),
                trim(row.get("cover_image")).isEmpty() ? null : Objects.toString(row.get("cover_image"), null),
                Objects.toString(row.get("status"), "DRAFT"),
                toLocalDateTime(row.get("publish_time")),
                intValue(row.get("sort_no"))
        );
    }

    private void syncArticleBanner(Long articleId, String title, String coverImage, String articleStatus, LocalDateTime publishTime, int sortNo) {
        if (articleId == null) {
            return;
        }

        Long bannerId = findArticleBannerId(articleId);
        boolean shouldExpose = ("SCHEDULED".equalsIgnoreCase(articleStatus) || "PUBLISHED".equalsIgnoreCase(articleStatus))
                && !trim(title).isEmpty()
                && !trim(coverImage).isEmpty();

        if (!shouldExpose) {
            if (bannerId != null) {
                disableArticleBanner(bannerId);
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bannerStartTime = "SCHEDULED".equalsIgnoreCase(articleStatus)
                ? publishTime
                : (publishTime != null ? publishTime : now);

        if (bannerId == null) {
            createArticleBanner(articleId, title, coverImage, bannerStartTime, sortNo, now);
            return;
        }
        updateArticleBanner(bannerId, articleId, title, coverImage, bannerStartTime, sortNo, now);
    }

    private Long findArticleBannerId(Long articleId) {
        String typeColumn = firstExistingBannerColumn("link_type", "type");
        String valueColumn = firstExistingBannerColumn("link_value", "target");
        if (typeColumn == null || valueColumn == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM banner WHERE " + typeColumn + " = ? AND " + valueColumn + " = ? ORDER BY id DESC LIMIT 1",
                "ARTICLE",
                String.valueOf(articleId)
        );
        if (rows.isEmpty()) {
            return null;
        }
        Object value = rows.get(0).get("id");
        return value instanceof Number number ? number.longValue() : null;
    }

    private void createArticleBanner(Long articleId, String title, String coverImage, LocalDateTime startTime, int sortNo, LocalDateTime now) {
        List<String> columns = new ArrayList<>(List.of("title", "image_url"));
        List<Object> args = new ArrayList<>(List.of(title, coverImage));
        addBannerColumnValue(columns, args, firstExistingBannerColumn("link_type", "type"), "ARTICLE");
        addBannerColumnValue(columns, args, firstExistingBannerColumn("link_value", "target"), String.valueOf(articleId));
        addBannerColumnValue(columns, args, firstExistingBannerColumn("sort_no", "sort"), sortNo);
        if (bannerColumnExists("status")) {
            addBannerColumnValue(columns, args, "status", bannerStatusValue(true));
        }
        addBannerColumnValue(columns, args, "start_time", startTime);
        addBannerColumnValue(columns, args, "create_time", now);
        addBannerColumnValue(columns, args, "update_time", now);
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        jdbcTemplate.update(
                "INSERT INTO banner (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")",
                args.toArray()
        );
    }

    private void updateArticleBanner(Long bannerId, Long articleId, String title, String coverImage, LocalDateTime startTime, int sortNo, LocalDateTime now) {
        StringBuilder sql = new StringBuilder("UPDATE banner SET title = ?, image_url = ?");
        List<Object> args = new ArrayList<>(List.of(title, coverImage));
        String typeColumn = firstExistingBannerColumn("link_type", "type");
        String valueColumn = firstExistingBannerColumn("link_value", "target");
        String sortColumn = firstExistingBannerColumn("sort_no", "sort");
        if (typeColumn != null) {
            sql.append(", ").append(typeColumn).append(" = ?");
            args.add("ARTICLE");
        }
        if (valueColumn != null) {
            sql.append(", ").append(valueColumn).append(" = ?");
            args.add(String.valueOf(articleId));
        }
        if (sortColumn != null) {
            sql.append(", ").append(sortColumn).append(" = ?");
            args.add(sortNo);
        }
        if (bannerColumnExists("status")) {
            sql.append(", status = ?");
            args.add(bannerStatusValue(true));
        }
        if (bannerColumnExists("start_time")) {
            sql.append(", start_time = ?");
            args.add(startTime);
        }
        if (bannerColumnExists("update_time")) {
            sql.append(", update_time = ?");
            args.add(now);
        }
        sql.append(" WHERE id = ?");
        args.add(bannerId);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    private void disableArticleBanner(Long bannerId) {
        StringBuilder sql = new StringBuilder("UPDATE banner SET ");
        List<Object> args = new ArrayList<>();
        boolean appended = false;
        if (bannerColumnExists("status")) {
            sql.append("status = ?");
            args.add(bannerStatusValue(false));
            appended = true;
        }
        if (bannerColumnExists("update_time")) {
            if (appended) {
                sql.append(", ");
            }
            sql.append("update_time = ?");
            args.add(LocalDateTime.now());
            appended = true;
        }
        if (!appended) {
            return;
        }
        sql.append(" WHERE id = ?");
        args.add(bannerId);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    private void deleteArticleBanner(Long articleId) {
        String typeColumn = firstExistingBannerColumn("link_type", "type");
        String valueColumn = firstExistingBannerColumn("link_value", "target");
        if (typeColumn == null || valueColumn == null) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM banner WHERE " + typeColumn + " = ? AND " + valueColumn + " = ?",
                "ARTICLE",
                String.valueOf(articleId)
        );
    }

    private String firstExistingBannerColumn(String... candidates) {
        for (String candidate : candidates) {
            if (bannerColumnExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void addBannerColumnValue(List<String> columns, List<Object> args, String column, Object value) {
        if (column != null && bannerColumnExists(column) && !columns.contains(column)) {
            columns.add(column);
            args.add(value);
        }
    }

    private boolean bannerColumnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'banner'
                  AND column_name = ?
                """,
                Integer.class,
                columnName
        );
        return count != null && count > 0;
    }

    private Object bannerStatusValue(boolean enabled) {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'banner' AND column_name = 'status'",
                String.class
        );
        if (dataType != null && Set.of("tinyint", "smallint", "int", "bigint", "bit").contains(dataType.toLowerCase(Locale.ROOT))) {
            return enabled ? 1 : 0;
        }
        return enabled ? "ENABLED" : "DISABLED";
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime time) {
            return time;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return parseDateTime(value);
    }
}
