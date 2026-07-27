package com.shiyiju.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Promotes due scheduled articles to the real published state.
 *
 * The public article API also guards scheduled rows by publish_time, but the
 * persisted status must be updated so the admin list, detail pages and any
 * other consumers see the same state after the scheduled time has arrived.
 */
@Component
public class ArticlePublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(ArticlePublishScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    public ArticlePublishScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${content.article.publish-scheduler-delay-ms:30000}")
    public void publishDueArticles() {
        int published = jdbcTemplate.update("""
                UPDATE content_article
                SET status = 'PUBLISHED', update_time = NOW()
                WHERE status = 'SCHEDULED'
                  AND publish_time IS NOT NULL
                  AND publish_time <= DATE_ADD(UTC_TIMESTAMP(), INTERVAL 8 HOUR)
                """);
        if (published > 0) {
            log.info("定时文章已自动发布: count={}", published);
        }
    }
}
