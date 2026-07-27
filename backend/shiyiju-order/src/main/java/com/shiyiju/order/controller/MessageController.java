package com.shiyiju.order.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/message")
@RequiredArgsConstructor
public class MessageController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }

        ensureMessagesTable();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;
        boolean filterByType = type != null && !type.isBlank();

        String countSql = filterByType
                ? "SELECT COUNT(*) FROM messages WHERE user_id = ? AND type = ?"
                : "SELECT COUNT(*) FROM messages WHERE user_id = ?";
        Long total = filterByType
                ? jdbcTemplate.queryForObject(countSql, Long.class, userId, type)
                : jdbcTemplate.queryForObject(countSql, Long.class, userId);

        String listSql = """
                SELECT id, user_id AS userId, type, title, content, data,
                       is_read AS isRead, read_time AS readTime, create_time AS createTime
                FROM messages
                WHERE user_id = ?
                """ + (filterByType ? " AND type = ?" : "") + """
                ORDER BY create_time DESC, id DESC
                LIMIT ? OFFSET ?
                """;

        List<Map<String, Object>> records = filterByType
                ? jdbcTemplate.queryForList(listSql, userId, type, safePageSize, offset)
                : jdbcTemplate.queryForList(listSql, userId, safePageSize, offset);

        return Result.success(PageResult.of(total == null ? 0L : total, safePage, safePageSize, records));
    }

    @PutMapping("/read/{messageId}")
    public Result<Void> markRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long messageId
    ) {
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }

        ensureMessagesTable();
        jdbcTemplate.update(
                "UPDATE messages SET is_read = 1, read_time = NOW() WHERE id = ? AND user_id = ?",
                messageId, userId
        );
        return Result.success();
    }

    private void ensureMessagesTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                type VARCHAR(32) NOT NULL,
                title VARCHAR(255) NOT NULL,
                content VARCHAR(1000) NOT NULL,
                data TEXT DEFAULT NULL,
                is_read TINYINT DEFAULT 0,
                read_time DATETIME DEFAULT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                KEY idx_user_type (user_id, type),
                KEY idx_user_read (user_id, is_read)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表'
            """);
    }
}
