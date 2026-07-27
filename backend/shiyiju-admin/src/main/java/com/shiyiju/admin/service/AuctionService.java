package com.shiyiju.admin.service;

import com.shiyiju.common.result.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * 拍卖管理服务 - 真实持久化（使用 JdbcTemplate）
 * 注意: 所有表名和列名必须与数据库实际定义一致。
 *   auction_session, auction_lot, auction_bid (均为单数)
 *   auction_lot 列: artist_name, start_price, estimate_price 等
 *   auction_bid 列: bid_price, bid_time 等
 */
@Service
public class AuctionService {

    private static final String TABLE_SESSION = "auction_session";
    private static final String TABLE_LOT = "auction_lot";
    private static final String TABLE_BID = "auction_bid";

    private final JdbcTemplate jdbcTemplate;

    public AuctionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 专场管理 ====================

    public PageResult<Map<String, Object>> getSessions(int page, int size, Integer status) {
        return getSessions(page, size, status, null);
    }

    public PageResult<Map<String, Object>> getSessions(int page, int size, Integer status, String name) {
        if (!tableExists(TABLE_SESSION)) return emptyResult();

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (status != null) { where.append(" AND status = ?"); args.add(status); }
        if (name != null && !name.isBlank()) { where.append(" AND title LIKE ?"); args.add("%" + name.trim() + "%"); }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + TABLE_SESSION + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, title, cover_image, description, start_time, end_time, status,"
            + " total_lots, total_bids, create_time, update_time"
            + " FROM " + TABLE_SESSION + where + " ORDER BY create_time DESC LIMIT ?, ?",
            queryArgs.toArray());

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer displayStatus = resolveSessionStatus(row.get("start_time"), row.get("end_time"), row.get("status"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("title"));
            item.put("name", row.get("title"));            // 前端兼容字段
            item.put("coverImage", row.get("cover_image"));
            item.put("cover", row.get("cover_image"));     // 前端兼容字段
            item.put("description", row.get("description"));
            item.put("startTime", toUtcString(row.get("start_time")));
            item.put("endTime", toUtcString(row.get("end_time")));
            item.put("status", displayStatus);
            item.put("statusText", getStatusText(displayStatus));
            item.put("totalLots", row.get("total_lots"));
            item.put("lotCount", row.get("total_lots"));   // 前端兼容字段
            item.put("totalBids", row.get("total_bids"));
            item.put("createTime", toUtcString(row.get("create_time")));
            item.put("updateTime", toUtcString(row.get("update_time")));
            records.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        fillPage(result, page, size, total);
        return result;
    }

    public Map<String, Object> getSessionById(Long id) {
        if (!tableExists(TABLE_SESSION)) return null;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM " + TABLE_SESSION + " WHERE id = ?", id);
        return rows.isEmpty() ? null : convertSession(rows.get(0));
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
        if (!tableExists(TABLE_SESSION)) return null;
        String title = (String) params.get("title");
        String name = title != null ? title : (String) params.get("name");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = parseDateTime(params.get("startTime"));
        LocalDateTime endTime = parseDateTime(params.get("endTime"));
        validateTimeRange(startTime, endTime);
        jdbcTemplate.update(
            "INSERT INTO " + TABLE_SESSION + " (title, description, cover_image, start_time, end_time, status, create_time, update_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            name, params.get("description"),
            params.get("coverImage") != null ? params.get("coverImage") : params.get("cover"),
            startTime, endTime,
            params.get("status") != null ? toInteger(params.get("status")) : 1, now, now);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void updateSession(Long id, Map<String, Object> params) {
        if (!tableExists(TABLE_SESSION)) return;
        StringBuilder sql = new StringBuilder("UPDATE " + TABLE_SESSION + " SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        if (params.containsKey("title")) { sql.append(", title = ?"); args.add(params.get("title")); }
        else if (params.containsKey("name")) { sql.append(", title = ?"); args.add(params.get("name")); }
        if (params.containsKey("description")) { sql.append(", description = ?"); args.add(params.get("description")); }
        if (params.containsKey("coverImage")) { sql.append(", cover_image = ?"); args.add(params.get("coverImage")); }
        else if (params.containsKey("cover")) { sql.append(", cover_image = ?"); args.add(params.get("cover")); }
        if (params.containsKey("startTime")) { sql.append(", start_time = ?"); args.add(parseDateTime(params.get("startTime"))); }
        if (params.containsKey("endTime")) { sql.append(", end_time = ?"); args.add(parseDateTime(params.get("endTime"))); }
        if (params.containsKey("status")) { sql.append(", status = ?"); args.add(params.get("status")); }
        LocalDateTime effectiveStart = params.containsKey("startTime") ? parseDateTime(params.get("startTime")) : null;
        LocalDateTime effectiveEnd = params.containsKey("endTime") ? parseDateTime(params.get("endTime")) : null;
        if (effectiveStart != null && effectiveEnd != null) validateTimeRange(effectiveStart, effectiveEnd);
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    @Transactional
    public boolean deleteSession(Long id) {
        if (!tableExists(TABLE_SESSION)) return false;
        Long lots = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_LOT + " WHERE session_id = ?", Long.class, id);
        if (lots != null && lots > 0) throw new IllegalStateException("专场下仍有拍品，请先移除拍品");
        return jdbcTemplate.update("DELETE FROM " + TABLE_SESSION + " WHERE id = ?", id) > 0;
    }

    // ==================== 拍品管理 ====================

    public Map<String, Object> getLotById(Long id) {
        if (!tableExists(TABLE_LOT)) return null;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM " + TABLE_LOT + " WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        return convertLotRow(rows.get(0));
    }

    public PageResult<Map<String, Object>> getLots(int page, int size, Long sessionId) {
        return getLots(page, size, sessionId, null);
    }

    public PageResult<Map<String, Object>> getLots(int page, int size, Long sessionId, Integer status) {
        if (!tableExists(TABLE_LOT)) return emptyResult();

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (sessionId != null) { where.append(" AND session_id = ?"); args.add(sessionId); }
        if (status != null) { where.append(" AND status = ?"); args.add(status); }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + TABLE_LOT + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, session_id, artwork_id, lot_no, title, cover_image,"
            + " artist_name, start_price, current_price, estimate_price,"
            + " reserve_price, increment, deposit_amount, bid_count, status,"
            + " start_time, end_time, create_time"
            + " FROM " + TABLE_LOT + where + " ORDER BY lot_no ASC LIMIT ?, ?",
            queryArgs.toArray());

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            records.add(convertLotRow(row));
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        fillPage(result, page, size, total);
        return result;
    }

    private Map<String, Object> convertLotRow(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.get("id"));
        item.put("sessionId", row.get("session_id"));
        item.put("artworkId", row.get("artwork_id"));
        item.put("lotNo", row.get("lot_no"));
        item.put("title", row.get("title"));
        item.put("coverImage", row.get("cover_image"));
        item.put("artistName", row.get("artist_name"));           // 与 Auction 模块字段名一致
        item.put("artist", row.get("artist_name"));               // 前端兼容字段
        item.put("startPrice", row.get("start_price"));           // 与 Auction 模块字段名一致
        item.put("startingPrice", row.get("start_price"));        // 前端兼容字段
        item.put("currentPrice", row.get("current_price"));
        item.put("estimatePrice", row.get("estimate_price"));     // 与 Auction 模块字段名一致
        item.put("bidCount", row.get("bid_count"));
        item.put("status", row.get("status"));
        item.put("reservePrice", row.get("reserve_price"));
        item.put("increment", row.get("increment"));
        item.put("depositAmount", row.get("deposit_amount"));
        item.put("startTime", toUtcString(row.get("start_time")));
        item.put("endTime", toUtcString(row.get("end_time")));
        item.put("createTime", toUtcString(row.get("create_time")));
        return item;
    }

    @Transactional
    public Long createLot(Map<String, Object> params) {
        if (!tableExists(TABLE_LOT)) return null;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = parseDateTime(params.get("startTime"));
        LocalDateTime endTime = parseDateTime(params.get("endTime"));
        validateTimeRange(startTime, endTime);
        jdbcTemplate.update(
            "INSERT INTO " + TABLE_LOT + " (session_id, artwork_id, lot_no, title, cover_image,"
            + " artist_name, start_price, current_price, estimate_price,"
            + " reserve_price, increment, deposit_amount, bid_count, status, start_time, end_time, create_time, update_time)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?)",
            params.get("sessionId"), params.get("artworkId"), params.get("lotNo"),
            params.get("title"),
            params.get("coverImage") != null ? params.get("coverImage") : params.get("cover"),
            params.get("artistName") != null ? params.get("artistName") : params.get("artist"),
            params.get("startPrice") != null ? params.get("startPrice") : params.get("startingPrice"),
            params.get("currentPrice") != null ? params.get("currentPrice") : params.get("startPrice"),
            params.get("estimatePrice") != null ? params.get("estimatePrice")
                : ((params.get("estimateLow") != null && params.get("estimateHigh") != null)
                   ? params.get("estimateLow") + " - " + params.get("estimateHigh") : null),
            params.get("reservePrice"), params.get("increment"), params.get("depositAmount"),
            params.get("status") != null ? params.get("status") : 0,
            startTime, endTime, now, now);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("UPDATE " + TABLE_SESSION + " SET total_lots=(SELECT COUNT(*) FROM " + TABLE_LOT + " WHERE session_id=?), update_time=? WHERE id=?", params.get("sessionId"), now, params.get("sessionId"));
        return id;
    }

    @Transactional
    public void updateLot(Long id, Map<String, Object> params) {
        if (!tableExists(TABLE_LOT)) return;
        Long previousSessionId = jdbcTemplate.query(
            "SELECT session_id FROM " + TABLE_LOT + " WHERE id=?",
            rs -> rs.next() ? rs.getLong(1) : null, id);
        StringBuilder sql = new StringBuilder("UPDATE " + TABLE_LOT + " SET update_time = ?");
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        if (params.containsKey("title")) { sql.append(", title = ?"); args.add(params.get("title")); }
        if (params.containsKey("sessionId")) { sql.append(", session_id = ?"); args.add(params.get("sessionId")); }
        if (params.containsKey("lotNo")) { sql.append(", lot_no = ?"); args.add(params.get("lotNo")); }
        if (params.containsKey("coverImage")) { sql.append(", cover_image = ?"); args.add(params.get("coverImage")); }
        if (params.containsKey("artistName") || params.containsKey("artist")) {
            sql.append(", artist_name = ?");
            args.add(params.getOrDefault("artistName", params.get("artist")));
        }
        if (params.containsKey("startPrice") || params.containsKey("startingPrice")) {
            sql.append(", start_price = ?");
            args.add(params.getOrDefault("startPrice", params.get("startingPrice")));
        }
        if (params.containsKey("currentPrice")) { sql.append(", current_price = ?"); args.add(params.get("currentPrice")); }
        if (params.containsKey("estimatePrice")) { sql.append(", estimate_price = ?"); args.add(params.get("estimatePrice")); }
        if (params.containsKey("reservePrice")) { sql.append(", reserve_price = ?"); args.add(params.get("reservePrice")); }
        if (params.containsKey("increment")) { sql.append(", increment = ?"); args.add(params.get("increment")); }
        if (params.containsKey("depositAmount")) { sql.append(", deposit_amount = ?"); args.add(params.get("depositAmount")); }
        if (params.containsKey("startTime")) { sql.append(", start_time = ?"); args.add(parseDateTime(params.get("startTime"))); }
        if (params.containsKey("endTime")) { sql.append(", end_time = ?"); args.add(parseDateTime(params.get("endTime"))); }
        if (params.containsKey("status")) { sql.append(", status = ?"); args.add(params.get("status")); }
        sql.append(" WHERE id = ?");
        args.add(id);
        jdbcTemplate.update(sql.toString(), args.toArray());
        Long currentSessionId = params.containsKey("sessionId")
            ? ((Number) params.get("sessionId")).longValue() : previousSessionId;
        recountSessionLots(previousSessionId);
        if (!Objects.equals(previousSessionId, currentSessionId)) recountSessionLots(currentSessionId);
    }

    @Transactional
    public boolean deleteLot(Long id) {
        if (!tableExists(TABLE_LOT)) return false;
        Long bids = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_BID + " WHERE lot_id = ?", Long.class, id);
        if (bids != null && bids > 0) throw new IllegalStateException("拍品已有出价记录，不能删除");
        Long sessionId = jdbcTemplate.query("SELECT session_id FROM " + TABLE_LOT + " WHERE id=?", rs -> rs.next() ? rs.getLong(1) : null, id);
        boolean deleted = jdbcTemplate.update("DELETE FROM " + TABLE_LOT + " WHERE id = ?", id) > 0;
        if (deleted) recountSessionLots(sessionId);
        return deleted;
    }

    // ==================== 出价记录 ====================

    public PageResult<Map<String, Object>> getBids(int page, int size, Long lotId) {
        return getBids(page, size, lotId, null, null);
    }

    public PageResult<Map<String, Object>> getBids(int page, int size, Long lotId, Long sessionId, Long userId) {
        if (!tableExists(TABLE_BID)) {
            PageResult<Map<String, Object>> result = new PageResult<>();
            result.setRecords(List.of());
            result.setTotal(0L);
            return result;
        }
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (lotId != null) { where.append(" AND lot_id = ?"); args.add(lotId); }
        if (sessionId != null) { where.append(" AND lot_id IN (SELECT id FROM " + TABLE_LOT + " WHERE session_id = ?)"); args.add(sessionId); }
        if (userId != null) { where.append(" AND user_id = ?"); args.add(userId); }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + TABLE_BID + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, lot_id, user_id, bid_price, bid_time, status"
            + " FROM " + TABLE_BID + where + " ORDER BY id DESC LIMIT ?, ?",
            queryArgs.toArray());

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("lotId", row.get("lot_id"));
            item.put("userId", row.get("user_id"));
            item.put("amount", row.get("bid_price"));         // 前端兼容字段
            item.put("bidPrice", row.get("bid_price"));        // 标准字段
            item.put("bidTime", toUtcString(row.get("bid_time"))); // 标准字段
            item.put("createTime", toUtcString(row.get("bid_time"))); // 前端兼容字段
            item.put("status", row.get("status"));
            Map<String, Object> lot = getLotById(((Number) row.get("lot_id")).longValue());
            if (lot != null) {
                item.put("lotNo", lot.get("lotNo"));
                item.put("lotTitle", lot.get("title"));
                item.put("artistName", lot.get("artistName"));
                item.put("sessionId", lot.get("sessionId"));
            }
            records.add(item);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total != null ? total : 0);
        fillPage(result, page, size, total);
        return result;
    }

    // ==================== 统计接口 ====================

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        if (!tableExists(TABLE_SESSION)) return defaultStats();

        try {
            Long totalSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLE_SESSION, Long.class);
            Long activeSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLE_SESSION + " WHERE status = 2", Long.class);
            Long totalBids = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_bids), 0) FROM " + TABLE_SESSION, Long.class);
            BigDecimal totalVolume = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_lots * total_bids), 0) FROM " + TABLE_SESSION, BigDecimal.class);

            stats.put("totalSessions", totalSessions != null ? totalSessions : 0);
            stats.put("activeSessions", activeSessions != null ? activeSessions : 0);
            stats.put("totalBids", totalBids != null ? totalBids : 0);
            stats.put("totalVolume", totalVolume != null ? totalVolume : BigDecimal.ZERO);
        } catch (Exception e) {
            return defaultStats();
        }
        return stats;
    }

    private Map<String, Object> defaultStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSessions", 0);
        stats.put("activeSessions", 0);
        stats.put("totalBids", 0);
        stats.put("totalVolume", BigDecimal.ZERO);
        return stats;
    }

    public Map<String, Object> getStats(String startDate, String endDate, Long sessionId) {
        LotFilter filter = buildLotFilter(startDate, endDate, sessionId, "l");
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT COALESCE(SUM(CASE WHEN l.status=2 THEN l.current_price ELSE 0 END),0) total_amount,"
            + " SUM(CASE WHEN l.status=2 THEN 1 ELSE 0 END) sold_lots,"
            + " SUM(CASE WHEN l.status=3 THEN 1 ELSE 0 END) unsold_lots,"
            + " COUNT(DISTINCT l.session_id) session_count FROM " + TABLE_LOT + " l" + filter.sql(),
            filter.args().toArray());
        Long bidderCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT b.user_id) FROM " + TABLE_BID + " b JOIN " + TABLE_LOT
            + " l ON l.id=b.lot_id" + filter.sql(), Long.class, filter.args().toArray());
        long sold = number(row.get("sold_lots")).longValue();
        long unsold = number(row.get("unsold_lots")).longValue();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAmount", number(row.get("total_amount")));
        stats.put("totalLots", sold);
        stats.put("unsoldLots", unsold);
        stats.put("soldRate", sold + unsold == 0 ? 0 : Math.round(sold * 1000.0 / (sold + unsold)) / 10.0);
        stats.put("sessionCount", number(row.get("session_count")));
        stats.put("bidderCount", bidderCount == null ? 0 : bidderCount);
        stats.put("activeBidders", bidderCount == null ? 0 : bidderCount);
        stats.put("amountGrowth", 0);
        return stats;
    }

    public List<Map<String, Object>> getSessionRank(String startDate, String endDate, Long sessionId) {
        LotFilter filter = buildLotFilter(startDate, endDate, sessionId, "l");
        return jdbcTemplate.queryForList(
            "SELECT s.id, s.title sessionName, DATE(s.start_time) startDate, COUNT(l.id) totalLots,"
            + " SUM(CASE WHEN l.status=2 THEN 1 ELSE 0 END) soldLots,"
            + " CASE WHEN COUNT(l.id)=0 THEN 0 ELSE ROUND(SUM(CASE WHEN l.status=2 THEN 1 ELSE 0 END)*100.0/COUNT(l.id),1) END soldRate,"
            + " COALESCE(SUM(CASE WHEN l.status=2 THEN l.current_price ELSE 0 END),0) totalAmount,"
            + " COALESCE(MAX(CASE WHEN l.status=2 THEN l.current_price ELSE 0 END),0) maxPrice"
            + " FROM " + TABLE_LOT + " l JOIN " + TABLE_SESSION + " s ON s.id=l.session_id"
            + filter.sql() + " GROUP BY s.id,s.title,s.start_time ORDER BY totalAmount DESC LIMIT 10",
            filter.args().toArray());
    }

    public List<Map<String, Object>> getArtistRank(String startDate, String endDate, Long sessionId) {
        LotFilter filter = buildLotFilter(startDate, endDate, sessionId, "l");
        return jdbcTemplate.queryForList(
            "SELECT COALESCE(NULLIF(l.artist_name,''),'未知艺术家') artistName, COUNT(l.id) lotCount,"
            + " SUM(CASE WHEN l.status=2 THEN 1 ELSE 0 END) soldCount,"
            + " CASE WHEN COUNT(l.id)=0 THEN 0 ELSE ROUND(SUM(CASE WHEN l.status=2 THEN 1 ELSE 0 END)*100.0/COUNT(l.id),1) END soldRate,"
            + " COALESCE(SUM(CASE WHEN l.status=2 THEN l.current_price ELSE 0 END),0) totalAmount,"
            + " COALESCE(MAX(CASE WHEN l.status=2 THEN l.current_price ELSE 0 END),0) maxPrice"
            + " FROM " + TABLE_LOT + " l" + filter.sql()
            + " GROUP BY l.artist_name ORDER BY totalAmount DESC, soldCount DESC LIMIT 10",
            filter.args().toArray());
    }

    public PageResult<Map<String, Object>> getDeals(int page, int size, String startDate, String endDate, Long sessionId) {
        LotFilter filter = buildLotFilter(startDate, endDate, sessionId, "l");
        String soldWhere = filter.sql() + " AND l.status=2";
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + TABLE_LOT + " l" + soldWhere, Long.class, filter.args().toArray());
        List<Object> args = new ArrayList<>(filter.args());
        args.add((page - 1) * size);
        args.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT l.id, l.lot_no, l.title, l.cover_image, l.artist_name, l.current_price, l.buyer_id,"
            + " l.end_time, s.title session_name FROM " + TABLE_LOT + " l JOIN " + TABLE_SESSION
            + " s ON s.id=l.session_id" + soldWhere + " ORDER BY l.end_time DESC LIMIT ?, ?", args.toArray());
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("dealNo", "AUC" + String.format("%012d", ((Number) row.get("id")).longValue()));
            item.put("lotNo", row.get("lot_no"));
            item.put("title", row.get("title"));
            item.put("image", row.get("cover_image"));
            item.put("artistName", row.get("artist_name"));
            item.put("sessionName", row.get("session_name"));
            item.put("dealPrice", row.get("current_price"));
            item.put("buyerId", row.get("buyer_id"));
            item.put("buyerName", row.get("buyer_id") == null ? "-" : "用户 " + row.get("buyer_id"));
            item.put("dealTime", toUtcString(row.get("end_time")));
            item.put("status", "completed");
            records.add(item);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total == null ? 0 : total);
        fillPage(result, page, size, total);
        return result;
    }

    public List<Map<String, Object>> getTrend(int days, Long sessionId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE l.status=2 AND l.end_time >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL ? DAY)");
        args.add(Math.max(1, Math.min(days, 365)));
        if (sessionId != null) { where.append(" AND l.session_id=?"); args.add(sessionId); }
        return jdbcTemplate.queryForList(
            "SELECT DATE(l.end_time) date, COUNT(*) count, COALESCE(SUM(l.current_price),0) amount"
            + " FROM " + TABLE_LOT + " l" + where + " GROUP BY DATE(l.end_time) ORDER BY date", args.toArray());
    }

    private LotFilter buildLotFilter(String startDate, String endDate, Long sessionId, String alias) {
        String prefix = alias == null || alias.isBlank() ? "" : alias + ".";
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND ").append(prefix).append("end_time >= ?");
            args.add(startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND ").append(prefix).append("end_time <= ?");
            args.add(endDate.trim() + " 23:59:59");
        }
        if (sessionId != null) {
            sql.append(" AND ").append(prefix).append("session_id = ?");
            args.add(sessionId);
        }
        return new LotFilter(sql.toString(), args);
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0;
    }

    private record LotFilter(String sql, List<Object> args) {}

    // ==================== 私有方法 ====================

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
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
        item.put("title", row.get("title"));
        item.put("name", row.get("title"));
        item.put("coverImage", row.get("cover_image"));
        item.put("cover", row.get("cover_image"));
        item.put("description", row.get("description"));
        item.put("startTime", toUtcString(row.get("start_time")));
        item.put("endTime", toUtcString(row.get("end_time")));
        item.put("status", resolveSessionStatus(row.get("start_time"), row.get("end_time"), row.get("status")));
        item.put("totalLots", row.get("total_lots"));
        item.put("totalBids", row.get("total_bids"));
        item.put("createTime", toUtcString(row.get("create_time")));
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

    private Integer resolveSessionStatus(Object startValue, Object endValue, Object statusValue) {
        Integer storedStatus = toInteger(statusValue);
        if (storedStatus != null && (storedStatus == 0 || storedStatus == 4)) return storedStatus;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = toLocalDateTime(startValue);
        LocalDateTime endTime = toLocalDateTime(endValue);
        if (startTime != null && now.isBefore(startTime)) return 1;
        if (endTime != null && now.isAfter(endTime)) return 3;
        if (startTime != null || endTime != null) return 2;
        return storedStatus;
    }

    private void recountSessionLots(Long sessionId) {
        if (sessionId == null) return;
        jdbcTemplate.update(
            "UPDATE " + TABLE_SESSION + " SET total_lots=(SELECT COUNT(*) FROM " + TABLE_LOT
            + " WHERE session_id=?), update_time=? WHERE id=?",
            sessionId, LocalDateTime.now(), sessionId);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof String text && !text.isBlank()) return LocalDateTime.parse(text.replace(" ", "T"));
        return null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        try {
            if (text.endsWith("Z")) {
                return LocalDateTime.ofInstant(Instant.parse(text), ZoneOffset.UTC);
            }
            if (text.matches(".*[+-]\\d{2}:?\\d{2}$")) {
                return OffsetDateTime.parse(text).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("时间格式不正确: " + text);
        }
    }

    private String toUtcString(Object value) {
        LocalDateTime time = toLocalDateTime(value);
        return time == null ? null : time.atOffset(ZoneOffset.UTC).toString();
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) throw new IllegalArgumentException("开始和结束时间不能为空");
        if (!endTime.isAfter(startTime)) throw new IllegalArgumentException("结束时间必须晚于开始时间");
    }

    private void fillPage(PageResult<Map<String, Object>> result, int page, int size, Long total) {
        long count = total == null ? 0 : total;
        result.setPage(page);
        result.setPageSize(size);
        result.setTotalPages(size > 0 ? (int) Math.ceil((double) count / size) : 0);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text && !text.isBlank()) return Integer.parseInt(text);
        return null;
    }
}
