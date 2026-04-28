package com.shiyiju.admin.service;

import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 拍卖管理服务 - 真实持久化（使用 JdbcTemplate）
 * 注意: 拍卖表 (auction_session, auction_lot) 需要在数据库中创建
 */
@Service
public class AuctionService {

    private final JdbcTemplate jdbcTemplate;

    public AuctionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 专场管理 ====================

    public PageResult<Map<String, Object>> getSessions(int page, int size, Integer status) {
        // 检查表是否存在
        if (!tableExists("auction_sessions")) {
            return emptyResult();
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM auction_sessions" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, name, cover_image, description, start_time, end_time, status,
                   lot_count, total_bids, create_time, update_time
            FROM auction_sessions
            """ + where + " ORDER BY create_time DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("name"));  // 数据库是name，前端用title
            item.put("name", row.get("name"));
            item.put("coverImage", row.get("cover_image"));
            item.put("cover", row.get("cover_image"));  // 前端用cover
            item.put("description", row.get("description"));
            item.put("startTime", row.get("start_time"));
            item.put("endTime", row.get("end_time"));
            item.put("status", row.get("status"));
            item.put("statusText", getStatusText((Integer) row.get("status")));
            item.put("totalLots", row.get("lot_count"));  // 数据库是lot_count
            item.put("lotCount", row.get("lot_count"));
            item.put("totalBids", row.get("total_bids"));
            item.put("createTime", row.get("create_time"));
            item.put("updateTime", row.get("update_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    public Map<String, Object> getSessionById(Long id) {
        if (!tableExists("auction_sessions")) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM auction_sessions WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        return convertSession(rows.get(0));
    }

    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSessions", 0);
        stats.put("activeSessions", 0);
        stats.put("totalLots", 0);
        stats.put("totalBids", 0);
        stats.put("totalVolume", BigDecimal.ZERO);
        return stats;
    }

    @Transactional
    public Long createSession(Map<String, Object> params) {
        if (!tableExists("auction_sessions")) return null;
        String title = (String) params.get("title");
        String name = title != null ? title : (String) params.get("name");
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO auction_sessions (name, description, cover_image, start_time, end_time, status, create_time, update_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            name, params.get("description"), params.get("coverImage") != null ? params.get("coverImage") : params.get("cover"),
            params.get("startTime"), params.get("endTime"), params.get("status") != null ? 1 : 0,
            now, now);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void updateSession(Long id, Map<String, Object> params) {
        if (!tableExists("auction_sessions")) return;
        StringBuilder sql = new StringBuilder("UPDATE auction_sessions SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        if (params.containsKey("title")) { sql.append(", name = ?"); args.add(params.get("title")); }
        else if (params.containsKey("name")) { sql.append(", name = ?"); args.add(params.get("name")); }
        if (params.containsKey("description")) { sql.append(", description = ?"); args.add(params.get("description")); }
        if (params.containsKey("coverImage")) { sql.append(", cover_image = ?"); args.add(params.get("coverImage")); }
        else if (params.containsKey("cover")) { sql.append(", cover_image = ?"); args.add(params.get("cover")); }
        if (params.containsKey("startTime")) { sql.append(", start_time = ?"); args.add(params.get("startTime")); }
        if (params.containsKey("endTime")) { sql.append(", end_time = ?"); args.add(params.get("endTime")); }
        if (params.containsKey("status")) { sql.append(", status = ?"); args.add(params.get("status")); }
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    @Transactional
    public boolean deleteSession(Long id) {
        if (!tableExists("auction_sessions")) return false;
        return jdbcTemplate.update("DELETE FROM auction_sessions WHERE id = ?", id) > 0;
    }

    public Map<String, Object> getLotById(Long id) {
        if (!tableExists("auction_lots")) return null;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM auction_lots WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> item = new LinkedHashMap<>();
        Map<String, Object> row = rows.get(0);
        item.put("id", row.get("id"));
        item.put("sessionId", row.get("session_id"));
        item.put("artworkId", row.get("artwork_id"));
        item.put("lotNo", row.get("lot_no"));
        item.put("title", row.get("title"));
        item.put("artist", row.get("artist"));
        item.put("startingPrice", row.get("starting_price"));
        item.put("currentPrice", row.get("current_price"));
        item.put("bidCount", row.get("bid_count"));
        item.put("status", row.get("status"));
        return item;
    }

    @Transactional
    public Long createLot(Map<String, Object> params) {
        if (!tableExists("auction_lots")) return null;
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO auction_lots (session_id, artwork_id, lot_no, title, artist, estimate_low, estimate_high, " +
            "starting_price, status, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            params.get("sessionId"), params.get("artworkId"), params.get("lotNo"),
            params.get("title"), params.get("artist"),
            params.get("estimateLow"), params.get("estimateHigh"),
            params.get("startingPrice"), params.get("status") != null ? 1 : 0, now);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void updateLot(Long id, Map<String, Object> params) {
        if (!tableExists("auction_lots")) return;
        StringBuilder sql = new StringBuilder("UPDATE auction_lots SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        if (params.containsKey("title")) { sql.append(", title = ?"); args.add(params.get("title")); }
        if (params.containsKey("artist")) { sql.append(", artist = ?"); args.add(params.get("artist")); }
        if (params.containsKey("startingPrice")) { sql.append(", starting_price = ?"); args.add(params.get("startingPrice")); }
        if (params.containsKey("status")) { sql.append(", status = ?"); args.add(params.get("status")); }
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    @Transactional
    public boolean deleteLot(Long id) {
        if (!tableExists("auction_lots")) return false;
        return jdbcTemplate.update("DELETE FROM auction_lots WHERE id = ?", id) > 0;
    }

    public PageResult<Map<String, Object>> getBids(int page, int size, Long lotId) {
        if (!tableExists("auction_bids")) {
            PageResult<Map<String, Object>> result = new PageResult<>();
            result.setRecords(List.of());
            result.setTotal(0L);
            return result;
        }
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (lotId != null) { where.append(" AND lot_id = ?"); args.add(lotId); }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auction_bids" + where, Long.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM auction_bids" + where + " ORDER BY id DESC LIMIT ?, ?", queryArgs.toArray());
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("lotId", row.get("lot_id"));
            item.put("userId", row.get("user_id"));
            item.put("amount", row.get("amount"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total != null ? total : 0);
        return result;
    }

    // ==================== 拍品管理 ====================

    public PageResult<Map<String, Object>> getLots(int page, int size, Long sessionId) {
        return getLots(page, size, sessionId, null);
    }

    public PageResult<Map<String, Object>> getLots(int page, int size, Long sessionId, Integer status) {
        if (!tableExists("auction_lots")) {
            return emptyResult();
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        
        if (sessionId != null) {
            where.append(" AND session_id = ?");
            args.add(sessionId);
        }
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM auction_lots" + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, session_id, artwork_id, lot_no, title, artist, estimate_low, estimate_high,
                   starting_price, current_price, bid_count, status, create_time
            FROM auction_lot
            """ + where + " ORDER BY lot_no ASC LIMIT ?, ?",
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("sessionId", row.get("session_id"));
            item.put("artworkId", row.get("artwork_id"));
            item.put("lotNo", row.get("lot_no"));
            item.put("title", row.get("title"));
            item.put("artist", row.get("artist"));
            item.put("estimateLow", row.get("estimate_low"));
            item.put("estimateHigh", row.get("estimate_high"));
            item.put("startingPrice", row.get("starting_price"));
            item.put("currentPrice", row.get("current_price"));
            item.put("bidCount", row.get("bid_count"));
            item.put("status", row.get("status"));
            item.put("createTime", row.get("create_time"));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        return result;
    }

    // ==================== 统计接口 ====================

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // 如果表不存在，返回默认统计
        if (!tableExists("auction_sessions")) {
            stats.put("totalSessions", 0);
            stats.put("activeSessions", 0);
            stats.put("totalBids", 0);
            stats.put("totalVolume", BigDecimal.ZERO);
            return stats;
        }

        try {
            Long totalSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auction_sessions", Long.class);
            Long activeSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auction_sessions WHERE status = 1", Long.class);
            Long totalBids = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_bids), 0) FROM auction_sessions", Long.class);
            BigDecimal totalVolume = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(lot_count * total_bids), 0) FROM auction_sessions", BigDecimal.class);

            stats.put("totalSessions", totalSessions != null ? totalSessions : 0);
            stats.put("activeSessions", activeSessions != null ? activeSessions : 0);
            stats.put("totalBids", totalBids != null ? totalBids : 0);
            stats.put("totalVolume", totalVolume != null ? totalVolume : BigDecimal.ZERO);
        } catch (Exception e) {
            stats.put("totalSessions", 0);
            stats.put("activeSessions", 0);
            stats.put("totalBids", 0);
            stats.put("totalVolume", BigDecimal.ZERO);
        }
        
        return stats;
    }

    public Map<String, Object> getStats(String startDate, String endDate) {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            String whereClause = buildDateWhereClause(startDate, endDate, "create_time");
            Long totalSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auction_sessions WHERE " + whereClause, Long.class);
            stats.put("totalSessions", totalSessions != null ? totalSessions : 0);
            stats.put("activeSessions", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auction_sessions WHERE status = 1 AND " + whereClause, Long.class));
            stats.put("totalBids", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_bids), 0) FROM auction_sessions WHERE " + whereClause, Long.class));
            stats.put("totalVolume", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(lot_count * total_bids), 0) FROM auction_sessions WHERE " + whereClause, BigDecimal.class));
        } catch (Exception e) {
            stats.put("totalSessions", 0);
            stats.put("activeSessions", 0);
            stats.put("totalBids", 0);
            stats.put("totalVolume", BigDecimal.ZERO);
        }
        return stats;
    }

    public List<Map<String, Object>> getSessionRank(String startDate, String endDate) {
        try {
            String whereClause = buildDateWhereClause(startDate, endDate, "create_time");
            return jdbcTemplate.queryForList(
                "SELECT name AS title, lot_count AS total_lots, total_bids, 0 AS total_volume FROM auction_sessions WHERE " + whereClause + " ORDER BY lot_count DESC LIMIT 10");
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> getArtistRank(String startDate, String endDate) {
        try {
            String whereClause = buildDateWhereClause(startDate, endDate, "create_time");
            return jdbcTemplate.queryForList(
                "SELECT a.real_name AS artist_name, COUNT(DISTINCT s.id) AS session_count, SUM(s.total_lots) AS total_lots " +
                "FROM auction_sessions s JOIN artist a ON s.artist_id = a.id WHERE " + whereClause + " GROUP BY a.id, a.real_name ORDER BY total_lots DESC LIMIT 10");
        } catch (Exception e) {
            return List.of();
        }
    }

    public PageResult<Map<String, Object>> getDeals(int page, int size, String startDate, String endDate) {
        try {
            String whereClause = buildDateWhereClause(startDate, endDate, "create_time");
            Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auction_deal WHERE " + whereClause, Long.class);
            List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM auction_deal WHERE " + whereClause + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (page - 1) * size, size);
            PageResult<Map<String, Object>> result = new PageResult<>();
            result.setRecords(records);
            result.setTotal(total != null ? total : 0);
            return result;
        } catch (Exception e) {
            PageResult<Map<String, Object>> result = new PageResult<>();
            result.setRecords(List.of());
            result.setTotal(0L);
            return result;
        }
    }

    public List<Map<String, Object>> getTrend(int days) {
        try {
            return jdbcTemplate.queryForList(
                "SELECT DATE(create_time) AS date, COUNT(*) AS count, COALESCE(SUM(total_lots), 0) AS volume " +
                "FROM auction_sessions WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "GROUP BY DATE(create_time) ORDER BY date");
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildDateWhereClause(String startDate, String endDate, String column) {
        if (startDate == null && endDate == null) return "1 = 1";
        StringBuilder sb = new StringBuilder("1 = 1");
        if (startDate != null && !startDate.isBlank()) {
            sb.append(" AND ").append(column).append(" >= '").append(startDate).append("'");
        }
        if (endDate != null && !endDate.isBlank()) {
            sb.append(" AND ").append(column).append(" <= '").append(endDate).append(" 23:59:59'");
        }
        return sb.toString();
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

    private Map<String, Object> convertSession(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.get("id"));
        item.put("title", row.get("name"));
        item.put("name", row.get("name"));
        item.put("coverImage", row.get("cover_image"));
        item.put("cover", row.get("cover_image"));
        item.put("description", row.get("description"));
        item.put("startTime", row.get("start_time"));
        item.put("endTime", row.get("end_time"));
        item.put("status", row.get("status"));
        item.put("totalLots", row.get("lot_count"));
        item.put("totalBids", row.get("total_bids"));
        item.put("createTime", row.get("create_time"));
        return item;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "预展中";
            case 2 -> "拍卖中";
            case 3 -> "已结束";
            case 4 -> "已结算";
            default -> "未知";
        };
    }
}
