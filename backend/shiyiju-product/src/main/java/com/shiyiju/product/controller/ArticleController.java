package com.shiyiju.product.controller;

import com.shiyiju.common.result.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/product/article")
public class ArticleController {

    private final JdbcTemplate jdbcTemplate;

    public ArticleController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) {
        int pageNum = Math.max(page == null ? 1 : page, 1);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), 50);
        int offset = (pageNum - 1) * pageSize;

        StringBuilder where = new StringBuilder(
                " WHERE ((status = 'PUBLISHED') OR (status = 'SCHEDULED' AND publish_time IS NOT NULL AND publish_time <= DATE_ADD(UTC_TIMESTAMP(), INTERVAL 8 HOUR)))"
        );
        List<Object> args = new ArrayList<>();
        String categoryText = normalizeCategory(category);
        if (!categoryText.isEmpty()) {
            where.append(" AND category = ?");
            args.add(categoryText);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM content_article" + where,
                Long.class,
                args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(pageSize);
        queryArgs.add(offset);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT id, title, subtitle, author, cover_image, cover_original_image, body_image, category, summary, content, tags, publish_time, update_time
                FROM content_article
                """ + where + """
                 ORDER BY COALESCE(publish_time, update_time) DESC, id DESC
                 LIMIT ? OFFSET ?
                """,
                queryArgs.toArray()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", rows.stream().map(this::toArticle).toList());
        result.put("total", total == null ? 0 : total);
        result.put("page", pageNum);
        result.put("size", pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT id, title, subtitle, author, cover_image, cover_original_image, body_image, category, summary, content, tags, publish_time, update_time
                FROM content_article
                WHERE id = ?
                  AND ((status = 'PUBLISHED') OR (status = 'SCHEDULED' AND publish_time IS NOT NULL AND publish_time <= DATE_ADD(UTC_TIMESTAMP(), INTERVAL 8 HOUR)))
                LIMIT 1
                """,
                id
        );
        if (rows.isEmpty()) {
            return Result.fail("文章不存在或未发布");
        }
        return Result.success(toArticle(rows.get(0)));
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
        article.put("publishTime", row.get("publish_time"));
        article.put("updateTime", row.get("update_time"));
        return article;
    }

    private String normalizeCategory(Object value) {
        String text = Objects.toString(value, "").trim().toUpperCase(Locale.ROOT);
        if ("APPRECIATION".equals(text)) {
            return text;
        }
        return "";
    }
}
