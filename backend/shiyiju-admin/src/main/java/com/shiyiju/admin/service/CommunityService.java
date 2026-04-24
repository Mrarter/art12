package com.shiyiju.admin.service;

import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 社区管理服务 - 真实持久化（使用 JdbcTemplate）
 */
@Service
public class CommunityService {

    private final JdbcTemplate jdbcTemplate;

    public CommunityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 帖子管理 ====================

    public PageResult<Map<String, Object>> getPosts(int page, int size, Integer status) {
        if (!tableExists("community_post")) {
            return emptyResult();
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM community_post" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, user_id, nickname, content, images, video_url, topic_id, artwork_id,
                   like_count, comment_count, share_count, status, audit_remark, create_time, update_time
            FROM community_post
            """ + where + " ORDER BY create_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("userId", row.get("user_id"));
            item.put("nickname", row.get("nickname"));
            item.put("content", row.get("content"));
            item.put("images", row.get("images"));
            item.put("videoUrl", row.get("video_url"));
            item.put("topicId", row.get("topic_id"));
            item.put("artworkId", row.get("artwork_id"));
            item.put("likeCount", row.get("like_count"));
            item.put("commentCount", row.get("comment_count"));
            item.put("shareCount", row.get("share_count"));
            item.put("status", row.get("status"));
            item.put("statusText", getPostStatusText((Integer) row.get("status")));
            item.put("auditRemark", row.get("audit_remark"));
            item.put("createTime", row.get("create_time"));
            item.put("updateTime", row.get("update_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public void approvePost(Long id) {
        if (!tableExists("community_post")) return;
        jdbcTemplate.update(
            "UPDATE community_post SET status = 1, update_time = ? WHERE id = ?",
            LocalDateTime.now(), id);
    }

    @Transactional
    public void rejectPost(Long id, String reason) {
        if (!tableExists("community_post")) return;
        jdbcTemplate.update(
            "UPDATE community_post SET status = 3, audit_remark = ?, update_time = ? WHERE id = ?",
            reason, LocalDateTime.now(), id);
    }

    @Transactional
    public void deletePost(Long id) {
        if (!tableExists("community_post")) return;
        jdbcTemplate.update("DELETE FROM community_post WHERE id = ?", id);
    }

    // ==================== 评论管理 ====================

    public PageResult<Map<String, Object>> getComments(int page, int size, Long postId, Integer status) {
        if (!tableExists("post_comment")) {
            return emptyResult();
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (postId != null) {
            where.append(" AND post_id = ?");
            args.add(postId);
        }
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM post_comment" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, post_id, user_id, nickname, content, parent_id, reply_to_id,
                   like_count, status, create_time
            FROM post_comment
            """ + where + " ORDER BY create_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("postId", row.get("post_id"));
            item.put("userId", row.get("user_id"));
            item.put("nickname", row.get("nickname"));
            item.put("content", row.get("content"));
            item.put("parentId", row.get("parent_id"));
            item.put("replyToId", row.get("reply_to_id"));
            item.put("likeCount", row.get("like_count"));
            item.put("status", row.get("status"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public void approveComment(Long id) {
        if (!tableExists("post_comment")) return;
        jdbcTemplate.update(
            "UPDATE post_comment SET status = 1 WHERE id = ?",
            id);
    }

    @Transactional
    public void rejectComment(Long id, String reason) {
        if (!tableExists("post_comment")) return;
        jdbcTemplate.update(
            "UPDATE post_comment SET status = 3 WHERE id = ?",
            id);
    }

    @Transactional
    public void deleteComment(Long id) {
        if (!tableExists("post_comment")) return;
        jdbcTemplate.update("DELETE FROM post_comment WHERE id = ?", id);
    }

    // ==================== 话题管理 ====================

    public List<Map<String, Object>> getTopics() {
        if (!tableExists("topic")) {
            return List.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, name, icon, description, post_count, sort, status, create_time FROM topic ORDER BY sort ASC"
        );
        return rows;
    }

    public PageResult<Map<String, Object>> getTopics(int page, int size) {
        if (!tableExists("topic")) {
            return emptyResult();
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM topic", Long.class);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, name, icon, description, post_count, sort, status, create_time FROM topic ORDER BY sort ASC LIMIT ?, ?",
            (page - 1) * size, size
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("name", row.get("name"));
            item.put("icon", row.get("icon"));
            item.put("description", row.get("description"));
            item.put("postCount", row.get("post_count"));
            item.put("sort", row.get("sort"));
            item.put("status", row.get("status"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public Long createTopic(String name, String icon, Integer sort) {
        if (!tableExists("topic")) return null;
        jdbcTemplate.update(
            "INSERT INTO topic (name, icon, description, sort, status, create_time) VALUES (?, ?, '', ?, 1, ?)",
            name, icon, sort, LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public Long createTopic(Map<String, Object> params) {
        if (!tableExists("topic")) return null;
        String name = (String) params.get("name");
        String icon = params.get("icon") != null ? (String) params.get("icon") : "";
        String description = params.get("description") != null ? (String) params.get("description") : "";
        Integer sort = params.get("sort") != null ? (Integer) params.get("sort") : 0;
        
        jdbcTemplate.update(
            "INSERT INTO topic (name, icon, description, sort, status, create_time) VALUES (?, ?, ?, ?, 1, ?)",
            name, icon, description, sort, LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void updateTopic(Long id, Map<String, Object> params) {
        if (!tableExists("topic")) return;
        StringBuilder sql = new StringBuilder("UPDATE topic SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        
        if (params.containsKey("name")) {
            sql.append(", name = ?");
            args.add(params.get("name"));
        }
        if (params.containsKey("icon")) {
            sql.append(", icon = ?");
            args.add(params.get("icon"));
        }
        if (params.containsKey("description")) {
            sql.append(", description = ?");
            args.add(params.get("description"));
        }
        if (params.containsKey("sort")) {
            sql.append(", sort = ?");
            args.add(params.get("sort"));
        }
        
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    @Transactional
    public void deleteTopic(Long id) {
        if (!tableExists("topic")) return;
        jdbcTemplate.update("DELETE FROM topic WHERE id = ?", id);
    }

    // ==================== 举报管理 ====================

    public PageResult<Map<String, Object>> getReports(int page, int size, Integer status) {
        if (!tableExists("community_report")) {
            return emptyResult();
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM community_report" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, reporter_id, reporter_nickname, reported_user_id, reported_nickname,
                   target_id, target_type, report_type, content, images, status,
                   handle_result, handler_id, handler_name, handle_time, create_time
            FROM community_report
            """ + where + " ORDER BY create_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("reporterId", row.get("reporter_id"));
            item.put("reporterNickname", row.get("reporter_nickname"));
            item.put("reportedUserId", row.get("reported_user_id"));
            item.put("reportedNickname", row.get("reported_nickname"));
            item.put("targetId", row.get("target_id"));
            item.put("targetType", row.get("target_type"));
            item.put("reportType", row.get("report_type"));
            item.put("content", row.get("content"));
            item.put("images", row.get("images"));
            item.put("status", row.get("status"));
            item.put("statusText", (Integer) row.get("status") == 0 ? "待处理" : "已处理");
            item.put("handleResult", row.get("handle_result"));
            item.put("handlerId", row.get("handler_id"));
            item.put("handlerName", row.get("handler_name"));
            item.put("handleTime", row.get("handle_time"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    @Transactional
    public void handleReport(Long id, String result) {
        if (!tableExists("community_report")) return;
        jdbcTemplate.update(
            "UPDATE community_report SET status = 1, handle_result = ?, handle_time = ? WHERE id = ?",
            result, LocalDateTime.now(), id);
    }

    // ==================== 统计 ====================

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        Long postCount = 0L;
        Long commentCount = 0L;
        Long topicCount = 0L;
        Long reportCount = 0L;
        Long todayPostCount = 0L;
        
        if (tableExists("community_post")) {
            try {
                postCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM community_post", Long.class);
                todayPostCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM community_post WHERE DATE(create_time) = CURDATE()", Long.class);
            } catch (Exception e) { /* ignore */ }
        }
        
        if (tableExists("post_comment")) {
            try {
                commentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post_comment", Long.class);
            } catch (Exception e) { /* ignore */ }
        }
        
        if (tableExists("topic")) {
            try {
                topicCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM topic", Long.class);
            } catch (Exception e) { /* ignore */ }
        }
        
        if (tableExists("community_report")) {
            try {
                reportCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM community_report WHERE status = 0", Long.class);
            } catch (Exception e) { /* ignore */ }
        }

        stats.put("postCount", postCount != null ? postCount : 0);
        stats.put("commentCount", commentCount != null ? commentCount : 0);
        stats.put("topicCount", topicCount != null ? topicCount : 0);
        stats.put("pendingReportCount", reportCount != null ? reportCount : 0);
        stats.put("todayPostCount", todayPostCount != null ? todayPostCount : 0);
        
        return stats;
    }

    // ==================== 私有方法 ====================

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'shiyiju' AND table_name = ?",
                Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private PageResult<Map<String, Object>> emptyResult() {
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(new ArrayList<>());
        result.setTotal(0L);
        return result;
    }

    private String getPostStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "草稿";
            case 3 -> "已拒绝";
            default -> "未知";
        };
    }
}
