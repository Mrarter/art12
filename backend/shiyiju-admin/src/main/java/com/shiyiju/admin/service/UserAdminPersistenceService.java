package com.shiyiju.admin.service;

import com.shiyiju.admin.service.support.SchemaInspector;
import com.shiyiju.common.result.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserAdminPersistenceService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final SchemaInspector schemaInspector;

    public UserAdminPersistenceService(JdbcTemplate jdbcTemplate, SchemaInspector schemaInspector) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaInspector = schemaInspector;
    }

    public PageResult<Map<String, Object>> listUsers(
        int page,
        int size,
        String userId,
        String nickname,
        String phone,
        String identity,
        String startDate,
        String endDate
    ) {
        String userTable = userTable();
        String identityColumn = identityColumn(userTable);
        String createTimeColumn = createTimeColumn(userTable);
        String avatarCol = schemaInspector.firstExistingColumn(userTable, "avatar", "avatar_url");
        String phoneCol = schemaInspector.firstExistingColumn(userTable, "phone", "mobile");
        String uidSelect = "CAST(id AS CHAR)";
        if (schemaInspector.hasColumn(userTable, "uid")) {
            uidSelect = "uid";
        }
        if (schemaInspector.hasColumn(userTable, "user_uid")) {
            uidSelect = "COALESCE(" + uidSelect + ", user_uid)";
        }
        if (schemaInspector.hasColumn(userTable, "user_no")) {
            uidSelect = "COALESCE(" + uidSelect + ", user_no)";
        }

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (userId != null && !userId.isBlank()) {
            where.append(" AND id = ?");
            args.add(Long.parseLong(userId.trim()));
        }
        if (nickname != null && !nickname.isBlank()) {
            where.append(" AND nickname LIKE ?");
            args.add("%" + nickname.trim() + "%");
        }
        if (phone != null && !phone.isBlank()) {
            where.append(" AND ").append(phoneCol).append(" LIKE ?");
            args.add("%" + phone.trim() + "%");
        }
        appendIdentityFilter(where, args, identityColumn, identity);
        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND DATE(").append(createTimeColumn).append(") >= ?");
            args.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND DATE(").append(createTimeColumn).append(") <= ?");
            args.add(endDate);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + userTable + where,
            Long.class,
            args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        String baseSql = """
            SELECT id, %s AS uid, nickname, %s AS avatar, %s AS phone, status, %s AS register_time, %s AS identities_value,
                   %s AS promoter_level_value,
                   %s AS available_commission_value,
                   %s AS total_commission_value
            FROM %s
            """.formatted(
                uidSelect,
                avatarCol,
                phoneCol,
                createTimeColumn,
                identityColumn,
                columnOrNull(userTable, "promoter_level"),
                columnOrZero(userTable, "available_commission"),
                columnOrZero(userTable, "total_commission"),
                userTable
            );
        String orderByClause = " ORDER BY " + createTimeColumn + " DESC, id DESC LIMIT ?, ?";
        String finalSql = baseSql + where + orderByClause;
        log.info("【DEBUG】listUsers SQL: {}, args: {}", finalSql, queryArgs);
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            finalSql,
            queryArgs.toArray()
        );
        log.info("【DEBUG】listUsers result count: {}", rows.size());

        List<Map<String, Object>> records = rows.stream()
            .map(this::mapUserRow)
            .collect(Collectors.toList());

        return PageResult.of(total == null ? 0L : total, page, size, records);
    }

    /**
     * 获取艺术家用户列表（users表 - 作品作者）
     */
    public PageResult<Map<String, Object>> listArtistUsers(int page, int size, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        
        // 关联作品表查询作品数量
        String artworkJoin = "";
        String artistUidSelect = "COALESCE(u.uid, u.user_code, CONCAT('USR', DATE_FORMAT(COALESCE(u.register_time, u.create_time), '%Y%m%d'), LPAD(u.id, 4, '0')))";

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (u.nickname LIKE ? OR ").append(artistUidSelect).append(" LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        
        // 统计
        String countSql = "SELECT COUNT(*) FROM users u" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, args.toArray());
        
        // 分页查询
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        String sql = """
            SELECT u.id, %s AS uid, u.nickname, u.phone, u.avatar, 
                   COALESCE(art.artwork_count, 0) AS artwork_count,
                   COALESCE(art.total_views, 0) AS total_views,
                   COALESCE(art.total_favorites, 0) AS total_favorites,
                   COALESCE(u.register_time, u.create_time) AS register_time
            FROM users u
            LEFT JOIN (
                SELECT author_id, 
                       COUNT(*) AS artwork_count,
                       COALESCE(SUM(view_count), 0) AS total_views,
                       COALESCE(SUM(favorite_count), 0) AS total_favorites
                FROM artwork
                GROUP BY author_id
            ) art ON u.id = art.author_id
            """.formatted(artistUidSelect) + where + " ORDER BY u.id DESC LIMIT ?, ?";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, queryArgs.toArray());
        
        List<Map<String, Object>> records = rows.stream().map(row -> {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("userId", row.get("id"));
            record.put("uid", row.get("uid"));
            record.put("nickname", row.get("nickname"));
            record.put("phone", row.get("phone"));
            record.put("avatar", row.get("avatar"));
            record.put("artworkCount", row.get("artwork_count"));
            record.put("totalViews", row.get("total_views"));
            record.put("totalFavorites", row.get("total_favorites"));
            // 优先使用 register_time，没有则使用 create_time
            Object regTime = row.get("register_time");
            if (regTime == null) {
                regTime = row.get("create_time");
            }
            record.put("registerTime", regTime);
            return record;
        }).collect(Collectors.toList());
        
        return PageResult.of(total == null ? 0L : total, page, size, records);
    }

    public Map<String, Object> getUserStats() {
        String userTable = userTable();
        String identityColumn = identityColumn(userTable);
        String createTimeColumn = createTimeColumn(userTable);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + userTable, Long.class);
        Long artistCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + userTable + " WHERE " + identityColumn + " LIKE ?",
            Long.class,
            "%artist%"
        );
        Long promoterCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + userTable + " WHERE " + identityColumn + " LIKE ?",
            Long.class,
            "%promoter%"
        );
        Long todayNew = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + userTable + " WHERE DATE(" + createTimeColumn + ") = ?",
            Long.class,
            LocalDate.now()
        );

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total == null ? 0 : total);
        stats.put("artist", artistCount == null ? 0 : artistCount);
        stats.put("promoter", promoterCount == null ? 0 : promoterCount);
        stats.put("todayNew", todayNew == null ? 0 : todayNew);
        return stats;
    }

    /**
     * 批量更新用户UID
     * @param userIds 用户ID列表
     * @param uids 对应的UID列表
     */
    @Transactional
    public void batchUpdateUserUids(List<Long> userIds, List<String> uids) {
        String userTable = userTable();
        
        for (int i = 0; i < userIds.size(); i++) {
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET uid = ?, update_time = NOW() WHERE id = ?",
                uids.get(i),
                userIds.get(i)
            );
        }
        log.info("批量更新了 {} 个用户的UID", userIds.size());
    }
    
    /**
     * 更新单个用户UID
     * @param userId 用户ID
     * @param uid 新的UID
     */
    @Transactional
    public void updateUserUid(Long userId, String uid) {
        String userTable = userTable();
        jdbcTemplate.update(
            "UPDATE " + userTable + " SET uid = ?, update_time = NOW() WHERE id = ?",
            uid,
            userId
        );
        log.info("更新用户 {} 的UID为 {}", userId, uid);
    }
    
    @Transactional
    public void updateUser(Long userId, Map<String, Object> params) {
        String userTable = userTable();
        List<String> identities = normalizeIdentities(params.get("identities"));
        if (!identities.contains("collector") && identities.isEmpty()) {
            identities = List.of("collector");
        }

        StringBuilder sql = new StringBuilder("UPDATE " + userTable + " SET ");
        List<Object> args = new ArrayList<>();
        List<String> setClauses = new ArrayList<>();

        // nickname - 截断处理防止超过数据库字段限制
        String nickname = Objects.toString(params.get("nickname"), "");
        if (schemaInspector.hasColumn(userTable, "nickname")) {
            setClauses.add("nickname = ?");
            // users表50字符，user_account表100字符，sys_user表64字符
            int maxLen = "users".equals(userTable) ? 50 : ("user_account".equals(userTable) ? 100 : 64);
            args.add(nickname.length() > maxLen ? nickname.substring(0, maxLen) : nickname);
        }

        // phone
        String phone = Objects.toString(params.get("phone"), "");
        String phoneColumn = schemaInspector.firstExistingColumn(userTable, "phone", "mobile");
        if (schemaInspector.hasColumn(userTable, phoneColumn)) {
            setClauses.add(phoneColumn + " = ?");
            args.add(phone);
        }

        // email
        String email = Objects.toString(params.get("email"), null);
        if (email != null && schemaInspector.hasColumn(userTable, "email")) {
            setClauses.add("email = ?");
            args.add(email.isEmpty() ? null : email);
        }

        // avatar
        String avatar = Objects.toString(params.get("avatar"), null);
        if (avatar != null) {
            String avatarColumn = avatarColumn(userTable);
            if (schemaInspector.hasColumn(userTable, avatarColumn)) {
                setClauses.add(avatarColumn + " = ?");
                args.add(avatar.isEmpty() ? null : avatar);
            }
        }

        // identities
        if (schemaInspector.hasColumn(userTable, "identities")) {
            setClauses.add("identities = ?");
            args.add(String.join(",", identities));
        }
        if (schemaInspector.hasColumn(userTable, "identity_json")) {
            setClauses.add("identity_json = ?");
            args.add(toIdentityJson(identities));
        }
        if (schemaInspector.hasColumn(userTable, "identity")) {
            setClauses.add("identity = ?");
            args.add(primaryIdentity(identities));
        }

        // promoter_level
        if (schemaInspector.hasColumn(userTable, "promoter_level")) {
            setClauses.add("promoter_level = ?");
            if (identities.contains("promoter")) {
                try {
                    args.add("level_" + currentPromoterLevel(userId));
                } catch (Exception e) {
                    args.add(null);
                }
            } else {
                args.add(null);
            }
        }

        // artist_level
        if (schemaInspector.hasColumn(userTable, "artist_level")) {
            if (identities.contains("artist")) {
                // Keep existing artist_level
            } else {
                setClauses.add("artist_level = NULL");
            }
        }

        // update_time
        setClauses.add(updateTimeAssignment(userTable));
        args.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));

        // Build final SQL
        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE id = ?");
        args.add(userId);

        jdbcTemplate.update(sql.toString(), args.toArray());
        syncPromoterIdentity(userId, identities);
        updateArtistCertification(userId, params);
    }

    private void updateArtistCertification(Long userId, Map<String, Object> params) {
        // 检查是否有艺术家信息需要更新
        String realName = Objects.toString(params.get("realName"), null);
        String idCard = Objects.toString(params.get("idCard"), null);
        String resume = Objects.toString(params.get("resume"), null);

        // 如果没有需要更新的艺术家字段，直接返回
        if (realName == null && idCard == null && resume == null) {
            return;
        }

        String artistTable = artistTable();
        // 检查艺术家认证记录是否存在
        Long certId = null;
        try {
            certId = jdbcTemplate.queryForObject(
                "SELECT id FROM " + artistTable + " WHERE user_id = ? LIMIT 1",
                Long.class,
                userId
            );
        } catch (Exception e) {
            // 记录不存在，不需要更新
            return;
        }

        if (certId == null) {
            return;
        }

        // 构建更新语句
        List<String> setClauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        String realNameCol = schemaInspector.firstExistingColumn(artistTable, "real_name", "realName", "name");
        String idCardCol = schemaInspector.firstExistingColumn(artistTable, "id_card", "idCard", "idcard");
        String resumeCol = artistResumeColumn(artistTable);

        if (realName != null && schemaInspector.hasColumn(artistTable, realNameCol)) {
            setClauses.add(realNameCol + " = ?");
            args.add(realName.isEmpty() ? null : realName);
        }
        if (idCard != null && schemaInspector.hasColumn(artistTable, idCardCol)) {
            setClauses.add(idCardCol + " = ?");
            args.add(idCard.isEmpty() ? null : idCard);
        }
        if (resume != null && schemaInspector.hasColumn(artistTable, resumeCol)) {
            setClauses.add(resumeCol + " = ?");
            args.add(resume.isEmpty() ? null : resume);
        }

        if (setClauses.isEmpty()) {
            return;
        }

        // 添加更新时间
        String updateTimeCol = schemaInspector.firstExistingColumn(artistTable, "update_time", "updated_at");
        if (schemaInspector.hasColumn(artistTable, updateTimeCol)) {
            setClauses.add(updateTimeCol + " = NOW()");
        }

        String updateSql = "UPDATE " + artistTable + " SET " + String.join(", ", setClauses) + " WHERE id = ?";
        args.add(certId);

        jdbcTemplate.update(updateSql, args.toArray());
    }

    @Transactional
    public void updateUserStatus(Long userId, int status) {
        String userTable = userTable();
        jdbcTemplate.update(
            "UPDATE " + userTable + " SET status = ?, " + updateTimeAssignment(userTable) + " WHERE id = ?",
            status,
            userId
        );
    }

    /**
     * 批量更新用户状态
     * @param userIds 用户ID列表
     * @param status 目标状态（0=禁用，1=正常）
     */
    @Transactional
    public void batchUpdateUserStatus(List<Long> userIds, int status) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String userTable = userTable();
        String updateTimeCol = updateTimeAssignment(userTable);
        String sql = "UPDATE " + userTable + " SET status = ?, " + updateTimeCol + " WHERE id IN (";
        String placeholders = userIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        sql += placeholders + ")";
        
        List<Object> args = new ArrayList<>();
        args.add(status);
        args.add(LocalDateTime.now()); // updateTimeAssignment 的 ? 占位符
        args.addAll(userIds);
        jdbcTemplate.update(sql, args.toArray());
        log.info("批量更新用户状态：{} 个用户 -> status={}", userIds.size(), status);
    }

    /**
     * 批量删除用户（逻辑删除）
     * @param userIds 用户ID列表
     */
    @Transactional
    public void batchDeleteUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String userTable = userTable();
        String placeholders = userIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        
        if (schemaInspector.hasColumn(userTable, "deleted")) {
            // 逻辑删除
            String sql = "UPDATE " + userTable + " SET deleted = 1, " + updateTimeAssignment(userTable) + " WHERE id IN (" + placeholders + ")";
            List<Object> args = new ArrayList<>();
            args.add(LocalDateTime.now()); // updateTimeAssignment 的 ? 占位符
            args.addAll(userIds);
            jdbcTemplate.update(sql, args.toArray());
        } else {
            // 物理删除
            String sql = "DELETE FROM " + userTable + " WHERE id IN (" + placeholders + ")";
            jdbcTemplate.update(sql, userIds.toArray());
        }
        log.info("批量删除用户：{} 个", userIds.size());
    }

    /**
     * 批量分配用户身份
     * @param userIds 用户ID列表
     * @param identities 要设置的身份列表（如 ["artist", "promoter"]）
     */
    @Transactional
    public void batchAssignIdentities(List<Long> userIds, List<String> identities) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String userTable = userTable();
        
        // 获取实际的身份列名
        String identityCol = identityColumn(userTable);
        if ("NULL".equals(identityCol)) {
            log.warn("用户表 {} 没有身份列，跳过批量分配身份", userTable);
            return;
        }
        
        // 确保至少有 collector 身份
        List<String> newIdentities = new ArrayList<>(identities);
        if (!newIdentities.contains("collector")) {
            newIdentities.add("collector");
        }
        
        String identitiesStr = String.join(",", newIdentities);
        String placeholders = userIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        String sql = "UPDATE " + userTable + " SET " + identityCol + " = ?, " + updateTimeAssignment(userTable) + " WHERE id IN (" + placeholders + ")";
        
        List<Object> args = new ArrayList<>();
        args.add(identitiesStr);
        args.add(LocalDateTime.now()); // updateTimeAssignment 的 ? 占位符
        args.addAll(userIds);
        jdbcTemplate.update(sql, args.toArray());
        log.info("批量分配用户身份：{} 个用户 -> identities={}", userIds.size(), identitiesStr);
    }

    public Map<String, Object> getUserDetail(Long userId) {
        PageResult<Map<String, Object>> pageResult = listUsers(1, 1, String.valueOf(userId), null, null, null, null, null);
        return pageResult.getRecords().isEmpty() ? null : pageResult.getRecords().get(0);
    }

    public Map<String, Object> listArtists(int page, int size, String status,
                                           String keyword, String phone, String userId, String badge,
                                           String startDate, String endDate, String sortField, String sortOrder) {
        // 移除自动同步：避免每次查看列表时从作品表重新创建艺术家记录
        // syncArtworkArtists();

        String artistTable = artistTable();
        String userTable = userTable();
        String artistStatusColumn = artistStatusColumn(artistTable);
        String artistResumeColumn = artistResumeColumn(artistTable);
        String artistWorksColumn = artistWorksColumn(artistTable);
        String artistExhibitsColumn = artistExhibitsColumn(artistTable);
        String reviewTimeColumn = reviewTimeColumn(artistTable);

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (status != null && !status.isBlank() && !"all".equals(status)) {
            where.append(" AND a.").append(artistStatusColumn).append(" = ?");
            args.add(mapArtistStatus(status));
        }
        // 关键词搜索（昵称/真实姓名）
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (u.nickname LIKE ? OR " + artistColumnOrNull("real_name") + " LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
        }
        // 手机号精确匹配
        if (phone != null && !phone.isBlank()) {
            String phoneCol = schemaInspector.firstExistingColumn(userTable, "mobile", "phone");
            where.append(" AND u.").append(phoneCol).append(" = ?");
            args.add(phone.trim());
        }
        // 用户ID/UID
        if (userId != null && !userId.isBlank()) {
            if (schemaInspector.hasColumn(artistTable, "user_uid")) {
                where.append(" AND (a.user_uid = ? OR a.user_id = ?)");
                args.add(userId.trim());
                try { args.add(Long.parseLong(userId.trim())); } catch (Exception e) { args.add(-1L); }
            } else {
                where.append(" AND a.user_id = ?");
                try { args.add(Long.parseLong(userId.trim())); } catch (Exception e) { args.add(-1L); }
            }
        }
        // 认证等级
        if (badge != null && !badge.isBlank()) {
            String badgeCol = userColumnOrNull("artist_level");
            if (!"NULL".equals(badgeCol)) {
                where.append(" AND " + badgeCol + " = ?");
                args.add(badge);
            }
        }
        // 时间范围
        String timeCol = schemaInspector.firstExistingColumn(artistTable, "created_at", "create_time", "register_time");
        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND a.").append(timeCol).append(" >= ?");
            args.add(startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND a.").append(timeCol).append(" <= ?");
            args.add(endDate.trim() + " 23:59:59");
        }

        // user_uid 关联 user_account.uid，user_id (数字) 关联 user_account.id（需要提前定义供COUNT查询使用）
        String userJoinCondition = schemaInspector.hasColumn(artistTable, "user_uid") 
            ? "((a.user_uid IS NOT NULL AND a.user_uid = u.uid) OR (a.user_uid IS NULL AND a.user_id = u.id))"
            : "a.user_id = u.id";
        
        // COUNT查询需要同样的表连接
        String countSql = "SELECT COUNT(*) FROM " + artistTable + " a LEFT JOIN " + userTable + " u ON " + userJoinCondition + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        // 显示字段：优先用 user_uid (字符串格式)，否则用 user_account.uid/user_uid/id 兜底
        String userIdField = schemaInspector.hasColumn(artistTable, "user_uid") 
            ? "COALESCE(a.user_uid, u.uid, u.user_uid, CAST(u.id AS CHAR))" : "u.uid";
        String realNameSelect = artistColumnOrNull("real_name");
        if ("NULL".equals(realNameSelect)) {
            realNameSelect = artistColumnOrNull("artist_name");
        }
        if ("NULL".equals(realNameSelect)) {
            realNameSelect = artistColumnOrNull("name");
        }
        String idCardSelect = artistColumnOrNull("id_card");
        String artistCodeSelect = artistColumnOrNull("artist_code");
        String artworkTable = schemaInspector.resolveTable("artist_count_artwork", "artwork", "artworks", "products", "product");
        boolean canJoinArtworkCount = artworkTable != null
            && schemaInspector.hasColumn(artistTable, "user_id")
            && schemaInspector.hasColumn(artworkTable, "author_id");
        String artworkCountSelect = canJoinArtworkCount ? ", COALESCE(art.artwork_count, 0) AS artwork_count" : ", 0 AS artwork_count";
        String artworkCountJoin = canJoinArtworkCount
            ? " LEFT JOIN (SELECT author_id, COUNT(*) AS artwork_count FROM " + artworkTable + " GROUP BY author_id) art ON a.user_id = art.author_id"
            : "";
        
        String sql = """
            SELECT a.id, %s AS user_id, %s AS user_uid, %s AS real_name, %s AS id_card, %s AS artist_resume,
                   %s AS artist_works, %s AS artist_exhibits,
                   %s AS artist_status, %s AS review_time,
                   %s AS reject_reason,
                   u.nickname, %s AS phone, %s AS avatar, %s AS artist_level_value,
                   a.%s AS create_time, %s AS artist_code%s
            FROM %s a
            LEFT JOIN %s u ON %s
            """.formatted(
                userIdField,
                artistColumnOrNull("user_uid"),
                realNameSelect,
                idCardSelect,
                artistResumeColumn(artistTable),
                artistWorksColumn(artistTable),
                artistExhibitsColumn(artistTable),
                artistStatusColumn(artistTable),
                reviewTimeColumn(artistTable),
                rejectReasonColumn(artistTable),
                "u." + schemaInspector.firstExistingColumn(userTable, "mobile", "phone"),
                "u." + schemaInspector.firstExistingColumn(userTable, "avatar_url", "avatar"),
                userColumnOrNull("artist_level"),
                schemaInspector.firstExistingColumn(artistTable, "created_at", "create_time", "register_time"),
                artistCodeSelect,
                artworkCountSelect,
                artistTable,
                userTable,
                userJoinCondition
            ) + artworkCountJoin + where;

        // 动态排序
        String orderBy = buildArtistOrderBy(sortField, sortOrder, timeCol);
        
        // 构建最终SQL（where条件已经在sql中）
        String finalSql = sql + orderBy + " LIMIT ?, ?";
        
        // 添加分页参数
        List<Object> finalArgs = new ArrayList<>(args);
        finalArgs.add((page - 1) * size);
        finalArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            finalSql,
            finalArgs.toArray()
        );

        Long pendingCount = countArtistByStatus(0);
        Long approvedCount = countArtistByStatus(1);
        Long rejectedCount = countArtistByStatus(2);
        Long hiddenCount = countArtistByStatus(3);

        List<Map<String, Object>> list = rows.stream()
            .map(this::mapArtistRow)
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total == null ? 0 : total);
        result.put("page", page);
        result.put("size", size);
        result.put("pendingCount", pendingCount == null ? 0 : pendingCount);
        result.put("approvedCount", approvedCount == null ? 0 : approvedCount);
        result.put("rejectedCount", rejectedCount == null ? 0 : rejectedCount);
        result.put("hiddenCount", hiddenCount == null ? 0 : hiddenCount);
        return result;
    }

    /**
     * 构建艺术家列表的动态排序子句
     */
    private String buildArtistOrderBy(String sortField, String sortOrder, String defaultTimeCol) {
        // 白名单：允许的排序字段
        Set<String> allowedFields = Set.of("id", "create_time", "artist_level");
        String field = (sortField != null && allowedFields.contains(sortField)) ? sortField : "id";
        String order = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        
        return switch (field) {
            case "create_time" -> " ORDER BY a." + defaultTimeCol + " " + order;
            case "artist_level" -> {
                String col = userColumnOrNull("artist_level");
                yield " ORDER BY " + ("NULL".equals(col) ? "1" : col) + " " + order;
            }
            default -> " ORDER BY a.id " + order;
        };
    }

    @Transactional
    public void approveArtist(Long id, String badge) {
        String artistTable = artistTable();
        String userTable = userTable();
        Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM " + artistTable + " WHERE id = ?", Long.class, id);
        
        // 构建SET子句，只包含实际存在的列
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        // 设置认证状态为已通过
        String statusCol = artistStatusColumn(artistTable);
        if (!"NULL".equals(statusCol)) {
            setClauses.add(statusCol + " = 1");
        }
        
        // 拒绝原因（清空）
        String rejectCol = rejectReasonAssignment(artistTable);
        if (!rejectCol.isEmpty()) {
            setClauses.add(rejectCol);
            params.add(null);
        }
        
        // 审核时间
        String reviewCol = reviewTimeAssignment(artistTable);
        if (!reviewCol.isEmpty()) {
            setClauses.add(reviewCol);
            params.add(LocalDateTime.now());
        }
        
        // 更新时间
        String updateCol = updateTimeAssignment(artistTable);
        if (!updateCol.isEmpty()) {
            setClauses.add(updateCol);
            params.add(LocalDateTime.now());
        }
        
        if (!setClauses.isEmpty()) {
            String sql = "UPDATE " + artistTable + " SET " + String.join(", ", setClauses) + " WHERE id = ?";
            params.add(id);
            jdbcTemplate.update(sql, params.toArray());
        }
        
        appendIdentity(userId, "artist");
        if (schemaInspector.hasColumn(userTable, "artist_level")) {
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET artist_level = ?, " + updateTimeAssignment(userTable) + " WHERE id = ?",
                nullableText(badge),
                LocalDateTime.now(),
                userId
            );
        }
    }

    /**
     * 批量审核艺术家 - 通过
     * @param ids 艺术家记录ID列表
     * @param badge 认证等级（可选）
     */
    @Transactional
    public void batchApproveArtist(List<Long> ids, String badge) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String artistTable = artistTable();
        String userTable = userTable();
        
        // 获取这些艺术家记录对应的用户ID
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        List<Long> userIds = jdbcTemplate.queryForList(
            "SELECT user_id FROM " + artistTable + " WHERE id IN (" + placeholders + ")",
            Long.class,
            ids.toArray()
        );
        
        // 构建更新SQL，只更新实际存在的列
        List<String> setClauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        
        // 状态列
        setClauses.add(artistStatusColumn(artistTable) + " = ?");
        args.add(1); // 已认证
        
        // 更新时间（如果列存在）
        String updateTimeCol = updateTimeAssignment(artistTable);
        if (!"NULL".equals(updateTimeCol)) {
            setClauses.add(updateTimeCol);
            args.add(LocalDateTime.now());
        }
        
        String updateSql = "UPDATE " + artistTable + " SET " + String.join(", ", setClauses) + " WHERE id IN (" + placeholders + ")";
        args.addAll(ids);
        jdbcTemplate.update(updateSql, args.toArray());
        
        // 为用户添加 artist 身份
        for (Long userId : userIds) {
            appendIdentity(userId, "artist");
        }
        
        // 更新用户表的 artist_level
        if (schemaInspector.hasColumn(userTable, "artist_level")) {
            String userPlaceholders = userIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET artist_level = ?, " + updateTimeAssignment(userTable) + " WHERE id IN (" + userPlaceholders + ")",
                nullableText(badge),
                LocalDateTime.now(),
                userIds.toArray()
            );
        }
        
        log.info("批量通过艺术家认证：{} 个", ids.size());
    }

    @Transactional
    public void rejectArtist(Long id, String reason) {
        String artistTable = artistTable();
        jdbcTemplate.update(
            "UPDATE " + artistTable + " SET " + artistStatusColumn(artistTable) + " = 2, " +
                rejectReasonAssignment(artistTable) + ", " + reviewTimeAssignment(artistTable) + ", " +
                updateTimeAssignment(artistTable) + " WHERE id = ?",
            reason,
            LocalDateTime.now(),
            LocalDateTime.now(),
            id
        );
    }

    /**
     * 批量审核艺术家 - 拒绝
     * @param ids 艺术家记录ID列表
     * @param reason 拒绝原因
     */
    @Transactional
    public void batchRejectArtist(List<Long> ids, String reason) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String artistTable = artistTable();
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        
        // 构建更新SQL，只更新实际存在的列
        List<String> setClauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        
        // 状态列
        setClauses.add(artistStatusColumn(artistTable) + " = ?");
        args.add(2); // 已拒绝
        
        // 拒绝原因（如果列存在）
        String rejectCol = rejectReasonColumn(artistTable);
        if (!"NULL".equals(rejectCol)) {
            setClauses.add(rejectCol + " = ?");
            args.add(reason);
        }
        
        // 更新时间（如果列存在）
        String updateTimeCol = updateTimeAssignment(artistTable);
        if (!"NULL".equals(updateTimeCol)) {
            setClauses.add(updateTimeCol);
            args.add(LocalDateTime.now());
        }
        
        String updateSql = "UPDATE " + artistTable + " SET " + String.join(", ", setClauses) + " WHERE id IN (" + placeholders + ")";
        args.addAll(ids);
        jdbcTemplate.update(updateSql, args.toArray());
        
        log.info("批量拒绝艺术家认证：{} 个", ids.size());
    }

    @Transactional
    public void revokeArtist(Long id) {
        String artistTable = artistTable();
        String userTable = userTable();
        Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM " + artistTable + " WHERE id = ?", Long.class, id);
        jdbcTemplate.update(
            "UPDATE " + artistTable + " SET " + artistStatusColumn(artistTable) + " = 0, " +
                rejectReasonAssignment(artistTable) + ", " + reviewTimeAssignment(artistTable) + ", " +
                updateTimeAssignment(artistTable) + " WHERE id = ?",
            null,
            null,
            LocalDateTime.now(),
            id
        );
        removeIdentity(userId, "artist");
        if (schemaInspector.hasColumn(userTable, "artist_level")) {
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET artist_level = NULL, " + updateTimeAssignment(userTable) + " WHERE id = ?",
                LocalDateTime.now(),
                userId
            );
        }
    }

    @Transactional
    public void hideArtist(Long id) {
        String artistTable = artistTable();
        jdbcTemplate.update(
            "UPDATE " + artistTable + " SET " + artistStatusColumn(artistTable) + " = 3, " +
                updateTimeAssignment(artistTable) + " WHERE id = ?",
            LocalDateTime.now(),
            id
        );
    }

    /**
     * 批量隐藏艺术家
     * @param ids 艺术家记录ID列表
     */
    @Transactional
    public void batchHideArtist(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String artistTable = artistTable();
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        
        String updateSql = "UPDATE " + artistTable + " SET " + artistStatusColumn(artistTable) + " = 3, " +
            updateTimeAssignment(artistTable) + " WHERE id IN (" + placeholders + ")";
        
        List<Object> args = new ArrayList<>();
        args.add(LocalDateTime.now());
        args.addAll(ids);
        jdbcTemplate.update(updateSql, args.toArray());
        
        log.info("批量隐藏艺术家：{} 个", ids.size());
    }

    @Transactional
    public void reapplyArtist(Long id) {
        String artistTable = artistTable();
        jdbcTemplate.update(
            "UPDATE " + artistTable + " SET " + artistStatusColumn(artistTable) + " = 0, " +
                rejectReasonAssignment(artistTable) + ", " + reviewTimeAssignment(artistTable) + ", " +
                updateTimeAssignment(artistTable) + " WHERE id = ?",
            null,
            null,
            LocalDateTime.now(),
            id
        );
    }

    @Transactional
    public Map<String, Object> addArtist(Map<String, Object> params) {
        String userTable = userTable();
        String artistTable = artistTable();
        String phone = Objects.toString(params.get("phone"), "").trim();
        String nickname = Objects.toString(params.get("nickname"), "").trim();
        String realName = Objects.toString(params.get("realName"), "").trim();
        String avatar = Objects.toString(params.get("avatar"), "").trim();
        
        if (realName.isEmpty()) {
            throw new IllegalArgumentException("真实姓名不能为空");
        }

        Long userId = null;
        boolean isNewUser = false;
        
        // 如果提供了手机号，尝试查找现有用户
        if (!phone.isEmpty()) {
            userId = findUserIdByPhone(phone);
        }
        
        // 如果找到用户，更新头像
        if (userId != null && !avatar.isEmpty()) {
            updateUserAvatar(userId, avatar);
        }
        
        // 如果用户不存在，创建新用户
        if (userId == null) {
            // 使用真实姓名作为昵称（如果昵称为空）
            String finalNickname = nickname.isEmpty() ? realName : nickname;
            userId = createUserForAdmin(phone, finalNickname, avatar, List.of("artist", "collector"));
            isNewUser = true;
        } else {
            appendIdentity(userId, "artist");
        }

        // 获取用户UID
        String userUid = getUserUidById(userId);
        String artistCode = generateArtistCode();

        // 根据表类型选择不同的INSERT语句
        if ("artist_profile".equals(artistTable)) {
            // artist_profile 表结构
            jdbcTemplate.update(
                """
                INSERT INTO artist_profile (user_id, user_uid, artist_name, avatar_url, bio, style_tags, slogan, status, created_at, updated_at, artist_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                """,
                userId,
                userUid,
                realName,
                avatar.isEmpty() ? null : avatar,
                nullableText(params.get("resume")),
                null,
                nullableText(params.get("slogan")),
                LocalDateTime.now(),
                LocalDateTime.now(),
                artistCode
            );
        } else {
            // 其他艺术家表结构（如 artist_certifications）
            jdbcTemplate.update(
                """
                INSERT INTO %s (user_id, real_name, id_card, %s, %s, %s, %s, %s)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(
                    artistTable,
                    artistResumeColumn(artistTable),
                    artistWorksColumn(artistTable),
                    artistExhibitsColumn(artistTable),
                    artistStatusColumn(artistTable),
                    createTimeColumn(artistTable)
                ),
                userId,
                realName,
                nullableText(params.get("idCard")),
                nullableText(params.get("resume")),
                null,
                null,
                1,
                LocalDateTime.now()
            );
            // 如果有 artist_code 字段，更新它
            if (schemaInspector.hasColumn(artistTable, "artist_code")) {
                jdbcTemplate.update("UPDATE " + artistTable + " SET artist_code = ? WHERE id = LAST_INSERT_ID()", artistCode);
            }
        }

        if (schemaInspector.hasColumn(artistTable, "update_time")) {
            jdbcTemplate.update("UPDATE " + artistTable + " SET update_time = ? WHERE id = LAST_INSERT_ID()", LocalDateTime.now());
        }
        setArtistBadge(userId, nullableText(params.get("badge")));

        // 返回用户ID信息
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("userUid", userUid);
        result.put("artistCode", artistCode);
        result.put("isNewUser", isNewUser);
        result.put("message", isNewUser ? "新用户已创建，用户ID：" + userId + "，用户UID：" + userUid + "，艺术家编号：" + artistCode : "已添加到现有用户，用户ID：" + userId + "，用户UID：" + userUid);
        return result;
    }

    public Map<String, Object> listPromoters(int page, int size, String userId, String level, String status,
                                                String keyword, String phone,
                                                String startDate, String endDate, String sortField, String sortOrder) {
        String userTable = userTable();
        String promoterTable = promoterTable();
        
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status", "cert_status");
        String levelCol = schemaInspector.firstExistingColumn(promoterTable, "team_level", "level", "promoter_level");
        boolean hasStatusCol = schemaInspector.hasColumn(promoterTable, statusCol);
        
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (userId != null && !userId.isBlank()) {
            where.append(" AND p.user_id = ?");
            args.add(Long.parseLong(userId.trim()));
        }
        if (level != null && !level.isBlank()) {
            where.append(" AND p.").append(levelCol).append(" = ?");
            args.add(Integer.parseInt(level));
        }
        // 状态筛选
        if (status != null && !status.isBlank() && !"all".equals(status) && schemaInspector.hasColumn(promoterTable, statusCol)) {
            int statusValue = mapPromoterStatus(status);
            where.append(" AND p.").append(statusCol).append(" = ?");
            args.add(statusValue);
        }
        // 关键词搜索（昵称）
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND u.nickname LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
        // 手机号精确匹配
        if (phone != null && !phone.isBlank()) {
            String phoneCol = schemaInspector.firstExistingColumn(userTable, "mobile", "phone");
            where.append(" AND u.").append(phoneCol).append(" = ?");
            args.add(phone.trim());
        }
        // 时间范围
        String signTimeCol = schemaInspector.firstExistingColumn(promoterTable, "sign_time", "agreement_time", "created_at", "create_time");
        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND p.").append(signTimeCol).append(" >= ?");
            args.add(startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND p.").append(signTimeCol).append(" <= ?");
            args.add(endDate.trim() + " 23:59:59");
        }

        List<Object> countArgs = new ArrayList<>(args);
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + promoterTable + " p" + where,
            Long.class,
            countArgs.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        // 优先使用用户UID关联；历史数据 user_uid 为空时使用数字 user_id 兜底。
        boolean hasUserUid = schemaInspector.hasColumn(promoterTable, "user_uid");
        String uidExpr = userUidExpression("u", "p", hasUserUid);
        String userJoinCondition = hasUserUid
            ? "(p.user_id = u.id OR p.user_uid = u.uid OR p.user_uid = u.user_uid OR p.user_uid = u.user_no)"
            : "p.user_id = u.id";
        String userIdField = "CAST(p.user_id AS CHAR)";
        String inviterSelect = schemaInspector.hasColumn(promoterTable, "inviter_uid") ? ", p.inviter_uid AS inviter_uid" : "";
        String directCountCondition = schemaInspector.hasColumn(promoterTable, "inviter_uid")
            ? "child.inviter_uid = " + uidExpr
            : (schemaInspector.hasColumn(promoterTable, "inviter_id") ? "child.inviter_id = p.user_id" : "1 = 0");
        
        // 动态构建 SELECT 语句
        String statusSelect = hasStatusCol ? ", p." + statusCol + " AS status" : "";
        boolean hasSignTimeCol = schemaInspector.hasColumn(promoterTable, signTimeCol);
        String signTimeSelect = hasSignTimeCol ? ", p." + signTimeCol + " AS sign_time" : "";
        String sql = """
            SELECT p.id, %s AS user_id, %s AS uid_value, p.%s AS level, p.subordinate_count AS team_size%s%s%s,
                   u.nickname, u.%s AS phone, u.%s AS avatar, p.promoter_code,
                   %s AS promoter_level_value,
                   p.total_commission AS total_commission_value,
                   p.withdrawable_commission AS available_commission_value,
                   parent.display_name AS parent_name,
                   parent.user_uid AS parent_uid,
                   (SELECT COUNT(*) FROM %s child WHERE %s) AS direct_count
            FROM %s p
            LEFT JOIN %s u ON %s
            LEFT JOIN %s parent ON %s
            """.formatted(
                userIdField,
                uidExpr,
                levelCol, statusSelect, signTimeSelect, inviterSelect,
                schemaInspector.firstExistingColumn(userTable, "mobile", "phone"),
                schemaInspector.firstExistingColumn(userTable, "avatar_url", "avatar"),
                columnOrNull(userTable, "promoter_level"), 
                promoterTable,
                directCountCondition,
                promoterTable, userTable, userJoinCondition,
                promoterTable,
                schemaInspector.hasColumn(promoterTable, "inviter_uid")
                    ? "parent.user_uid = p.inviter_uid"
                    : "1 = 0"
            )
            + where;

        // 动态排序
        String promoterOrderBy = buildPromoterOrderBy(sortField, sortOrder, signTimeCol);
        
        // 构建最终SQL和参数（避免重复添加分页参数）
        String finalSql = sql + promoterOrderBy + " LIMIT ?, ?";
        List<Object> finalArgs = new ArrayList<>(args);
        finalArgs.add((page - 1) * size);
        finalArgs.add(size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            finalSql,
            finalArgs.toArray()
        );

        List<Map<String, Object>> list = rows.stream()
            .map(this::mapPromoterRow)
            .collect(Collectors.toList());

        // 统计各状态数量
        Long pendingCountVal = countPromoterByStatus(0);
        Long approvedCountVal = countPromoterByStatus(1);
        Long rejectedCountVal = countPromoterByStatus(-1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("records", list);
        result.put("total", total == null ? 0 : total);
        result.put("page", page);
        result.put("size", size);
        result.put("pendingCount", pendingCountVal == null ? 0 : pendingCountVal);
        result.put("approvedCount", approvedCountVal == null ? 0 : approvedCountVal);
        result.put("rejectedCount", rejectedCountVal == null ? 0 : rejectedCountVal);
        return result;
    }

    /**
     * 构建艺荐官列表的动态排序子句
     */
    private String buildPromoterOrderBy(String sortField, String sortOrder, String signTimeCol) {
        Set<String> allowedFields = Set.of("id", "sign_time", "total_commission", "team_size");
        String field = (sortField != null && allowedFields.contains(sortField)) ? sortField : "id";
        String order = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";

        return switch (field) {
            case "sign_time" -> " ORDER BY p." + signTimeCol + " " + order;
            case "total_commission" -> " ORDER BY p.total_commission " + order;
            case "team_size" -> " ORDER BY p.subordinate_count " + order;
            default -> " ORDER BY p.id " + order;
        };
    }

    private int mapPromoterStatus(String status) {
        return switch (status) {
            case "approved" -> 1;
            case "rejected" -> -1;
            default -> 0; // pending
        };
    }

    private Long countPromoterByStatus(int status) {
        String promoterTable = promoterTable();
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status", "cert_status");
        if (!schemaInspector.hasColumn(promoterTable, statusCol)) {
            return 0L;
        }
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + promoterTable + " WHERE " + statusCol + " = ?",
            Long.class,
            status
        );
    }

    // ==================== 艺荐官认证管理 ====================

    @Transactional
    public void approvePromoter(Long id) {
        String promoterTable = promoterTable();
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status");
        if (!schemaInspector.hasColumn(promoterTable, statusCol)) return;
        String sql = "UPDATE " + promoterTable + " SET " + statusCol + " = 1, " +
            updateTimeAssignment(promoterTable) + " WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now().format(DATE_TIME_FORMATTER), id);
    }

    @Transactional
    public void rejectPromoter(Long id, String reason) {
        String promoterTable = promoterTable();
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status");
        String rejectReasonCol = schemaInspector.firstExistingColumn(promoterTable, "reject_reason", "reason", "remark");
        List<String> setClauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (schemaInspector.hasColumn(promoterTable, statusCol)) {
            setClauses.add(statusCol + " = -1");
        }
        if (schemaInspector.hasColumn(promoterTable, rejectReasonCol)) {
            setClauses.add(rejectReasonCol + " = ?");
            args.add(reason);
        }
        setClauses.add(updateTimeAssignment(promoterTable));
        args.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        args.add(id);
        jdbcTemplate.update("UPDATE " + promoterTable + " SET " + String.join(", ", setClauses) + " WHERE id = ?", args.toArray());
    }

    @Transactional
    public void revokePromoter(Long id) {
        String promoterTable = promoterTable();
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status");
        if (!schemaInspector.hasColumn(promoterTable, statusCol)) return;
        String sql = "UPDATE " + promoterTable + " SET " + statusCol + " = -1, " +
            updateTimeAssignment(promoterTable) + " WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now().format(DATE_TIME_FORMATTER), id);
    }

    @Transactional
    public void reapplyPromoter(Long id) {
        String promoterTable = promoterTable();
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status");
        if (!schemaInspector.hasColumn(promoterTable, statusCol)) return;
        String sql = "UPDATE " + promoterTable + " SET " + statusCol + " = 0, " +
            updateTimeAssignment(promoterTable) + " WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now().format(DATE_TIME_FORMATTER), id);
    }

    @Transactional
    public void setPromoterLevelById(Long id, int level) {
        String promoterTable = promoterTable();
        String levelCol = schemaInspector.firstExistingColumn(promoterTable, "level", "promoter_level");
        if (schemaInspector.hasColumn(promoterTable, levelCol)) {
            String sql = "UPDATE " + promoterTable + " SET " + levelCol + " = ?, " +
                updateTimeAssignment(promoterTable) + " WHERE id = ?";
            jdbcTemplate.update(sql, level, LocalDateTime.now().format(DATE_TIME_FORMATTER), id);
        }
        // 同时更新用户的 promoter_level 字段
        String userIdSql = "SELECT user_id FROM " + promoterTable + " WHERE id = ?";
        Long userId = jdbcTemplate.queryForObject(userIdSql, Long.class, id);
        if (userId != null) {
            String userTable = userTable();
            if (schemaInspector.hasColumn(userTable, "promoter_level")) {
                jdbcTemplate.update("UPDATE " + userTable + " SET promoter_level = ? WHERE id = ?", "level_" + level, userId);
            }
        }
    }

    // ==================== 艺荐官批量操作 ====================

    @Transactional
    public void batchApprovePromoters(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        String promoterTable = promoterTable();
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status");
        if (!schemaInspector.hasColumn(promoterTable, statusCol)) return;
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        String sql = "UPDATE " + promoterTable + " SET " + statusCol + " = 1, " +
            updateTimeAssignment(promoterTable) + " WHERE id IN (" + placeholders + ")";
        jdbcTemplate.update(sql, LocalDateTime.now().format(DATE_TIME_FORMATTER), ids.toArray());
    }

    @Transactional
    public void batchRejectPromoters(List<Long> ids, String reason) {
        if (ids == null || ids.isEmpty()) return;
        String promoterTable = promoterTable();
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status");
        String rejectReasonCol = schemaInspector.firstExistingColumn(promoterTable, "reject_reason", "reason", "remark");
        if (!schemaInspector.hasColumn(promoterTable, statusCol)) return;
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        List<String> setClauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        setClauses.add(statusCol + " = -1");
        if (schemaInspector.hasColumn(promoterTable, rejectReasonCol)) {
            setClauses.add(rejectReasonCol + " = ?");
            args.add(reason);
        }
        setClauses.add(updateTimeAssignment(promoterTable));
        args.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        args.addAll(ids);
        String sql = "UPDATE " + promoterTable + " SET " + String.join(", ", setClauses) + " WHERE id IN (" + placeholders + ")";
        jdbcTemplate.update(sql, args.toArray());
    }

    @Transactional
    public void batchDeletePromoters(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        String promoterTable = promoterTable();
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        jdbcTemplate.update("DELETE FROM " + promoterTable + " WHERE id IN (" + placeholders + ")", ids.toArray());
    }

    @Transactional
    public void updatePromoterRelation(Long userId, Map<String, Object> params) {
        String promoterTable = promoterTable();
        List<String> setClauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (schemaInspector.hasColumn(promoterTable, "inviter_uid") && params.containsKey("inviterUid")) {
            String inviterUid = Objects.toString(params.get("inviterUid"), "").trim();
            setClauses.add("inviter_uid = ?");
            args.add(inviterUid.isEmpty() ? null : inviterUid);
        }
        if (schemaInspector.hasColumn(promoterTable, "subordinate_count") && params.containsKey("teamCount")) {
            setClauses.add("subordinate_count = ?");
            args.add(toInt(params.get("teamCount"), 0));
        }
        String realNameCol = schemaInspector.firstExistingColumn(promoterTable, "real_name", "display_name", "name");
        if (schemaInspector.hasColumn(promoterTable, realNameCol) && params.containsKey("realName")) {
            String realName = Objects.toString(params.get("realName"), "").trim();
            setClauses.add(realNameCol + " = ?");
            args.add(realName.isEmpty() ? null : realName);
        }
        if (setClauses.isEmpty()) {
            return;
        }
        setClauses.add(updateTimeAssignment(promoterTable));
        args.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        args.add(userId);
        jdbcTemplate.update(
            "UPDATE " + promoterTable + " SET " + String.join(", ", setClauses) + " WHERE user_id = ?",
            args.toArray()
        );
    }

    @Transactional
    public Map<String, Object> addPromoter(Map<String, Object> params) {
        String userTable = userTable();
        String promoterTable = promoterTable();
        Map<String, Object> result = new LinkedHashMap<>();
        
        String phone = Objects.toString(params.get("phone"), "").trim();
        String nickname = Objects.toString(params.get("nickname"), "").trim();
        String avatar = Objects.toString(params.get("avatar"), "").trim();
        int level = params.get("level") instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(params.get("level")));
        String remark = Objects.toString(params.get("remark"), "");
        
        // 新增字段
        String realName = Objects.toString(params.get("realName"), "").trim();
        String alipayAccount = Objects.toString(params.get("alipayAccount"), "").trim();
        String wechatAccount = Objects.toString(params.get("wechatAccount"), "").trim();
        String bankCard = Objects.toString(params.get("bankCard"), "").trim();
        
        // 姓名必填验证
        if (realName.isEmpty()) {
            throw new IllegalArgumentException("真实姓名不能为空");
        }
        
        Long userId = null;
        boolean isNewUser = false;
        
        // 如果提供了手机号，尝试查找现有用户
        if (!phone.isEmpty()) {
            userId = findUserIdByPhone(phone);
        }
        
        // 如果提供了用户ID，也尝试查找
        if (userId == null && params.get("userId") != null) {
            try {
                Long inputUserId = Long.parseLong(String.valueOf(params.get("userId")));
                if (findUserRow(inputUserId) != null) {
                    userId = inputUserId;
                }
            } catch (NumberFormatException ignored) {}
        }
        
        // 如果用户不存在且提供了必要信息，创建新用户
        if (userId == null) {
            if (phone.isEmpty()) {
                throw new IllegalArgumentException("手机号不能为空，用于创建用户");
            }
            if (nickname.isEmpty()) {
                // 安全处理：如果手机号长度不足4位，直接使用手机号作为后缀
                String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
                nickname = "用户" + suffix;
            }
            userId = createUserForAdmin(phone, nickname, avatar, List.of("promoter", "collector"));
            isNewUser = true;
        } else {
            appendIdentity(userId, "promoter");
            // 更新用户头像
            if (!avatar.isEmpty() && schemaInspector.hasColumn(userTable, "avatar")) {
                jdbcTemplate.update("UPDATE " + userTable + " SET avatar = ? WHERE id = ?", avatar, userId);
            }
        }

        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + promoterTable + " WHERE user_id = ?",
            Integer.class,
            userId
        );
        if (exists != null && exists > 0) {
            // 更新已有记录
            List<String> setClauses = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (schemaInspector.hasColumn(promoterTable, "level")) {
                setClauses.add("level = ?");
                args.add(level);
            }
            if (schemaInspector.hasColumn(promoterTable, "status")) {
                setClauses.add("status = ?");
                args.add(1);
            }
            // 更新新增字段：真实姓名
            if (schemaInspector.hasColumn(promoterTable, "real_name") || schemaInspector.hasColumn(promoterTable, "realName")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "real_name", "realName", "name");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    setClauses.add(col + " = ?");
                    args.add(realName);
                }
            }
            // 更新新增字段：支付宝账户
            if (schemaInspector.hasColumn(promoterTable, "alipay_account") || schemaInspector.hasColumn(promoterTable, "alipayAccount")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "alipay_account", "alipayAccount");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    setClauses.add(col + " = ?");
                    args.add(nullableText(alipayAccount));
                }
            }
            // 更新新增字段：微信账户
            if (schemaInspector.hasColumn(promoterTable, "wechat_account") || schemaInspector.hasColumn(promoterTable, "wechatAccount")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "wechat_account", "wechatAccount");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    setClauses.add(col + " = ?");
                    args.add(nullableText(wechatAccount));
                }
            }
            // 更新新增字段：银行账户
            if (schemaInspector.hasColumn(promoterTable, "bank_card") || schemaInspector.hasColumn(promoterTable, "bankCard")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "bank_card", "bankCard");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    setClauses.add(col + " = ?");
                    args.add(nullableText(bankCard));
                }
            }
            setClauses.add(updateTimeAssignment(promoterTable));
            args.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            args.add(userId);
            jdbcTemplate.update("UPDATE " + promoterTable + " SET " + String.join(", ", setClauses) + " WHERE user_id = ?", args.toArray());
            result.put("message", "更新成功，已设置为艺荐官，用户ID：" + userId);
        } else {
            // 插入新记录 - 动态构建列名
            List<String> columns = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            columns.add("user_id");
            args.add(userId);
            if (schemaInspector.hasColumn(promoterTable, "invite_code")) {
                columns.add("invite_code");
                args.add("SYJ" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            }
            if (schemaInspector.hasColumn(promoterTable, "level")) {
                columns.add("level");
                args.add(level);
            }
            if (schemaInspector.hasColumn(promoterTable, "status")) {
                columns.add("status");
                args.add(1);
            }
            // 新增字段：真实姓名
            if (schemaInspector.hasColumn(promoterTable, "real_name") || schemaInspector.hasColumn(promoterTable, "realName")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "real_name", "realName", "name");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    columns.add(col);
                    args.add(realName);
                }
            }
            // 新增字段：支付宝账户
            if (schemaInspector.hasColumn(promoterTable, "alipay_account") || schemaInspector.hasColumn(promoterTable, "alipayAccount")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "alipay_account", "alipayAccount");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    columns.add(col);
                    args.add(nullableText(alipayAccount));
                }
            }
            // 新增字段：微信账户
            if (schemaInspector.hasColumn(promoterTable, "wechat_account") || schemaInspector.hasColumn(promoterTable, "wechatAccount")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "wechat_account", "wechatAccount");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    columns.add(col);
                    args.add(nullableText(wechatAccount));
                }
            }
            // 新增字段：银行账户
            if (schemaInspector.hasColumn(promoterTable, "bank_card") || schemaInspector.hasColumn(promoterTable, "bankCard")) {
                String col = schemaInspector.firstExistingColumn(promoterTable, "bank_card", "bankCard");
                if (schemaInspector.hasColumn(promoterTable, col)) {
                    columns.add(col);
                    args.add(nullableText(bankCard));
                }
            }
            LocalDateTime now = LocalDateTime.now();
            if (schemaInspector.hasColumn(promoterTable, "sign_time")) {
                columns.add("sign_time");
                args.add(now);
            }
            if (schemaInspector.hasColumn(promoterTable, "create_time")) {
                columns.add("create_time");
                args.add(now);
            }
            if (schemaInspector.hasColumn(promoterTable, "update_time")) {
                columns.add("update_time");
                args.add(now);
            }
            // distributor_profile 表特殊处理：添加 user_uid、distributor_code 和 display_name
            if ("distributor_profile".equals(promoterTable)) {
                String userUid = getUserUidById(userId);
                if (schemaInspector.hasColumn(promoterTable, "user_uid")) {
                    columns.add("user_uid");
                    args.add(userUid);
                }
                // 添加 display_name 字段（必填）
                if (schemaInspector.hasColumn(promoterTable, "display_name")) {
                    columns.add("display_name");
                    args.add(realName.isEmpty() ? nickname : realName);
                }
                // 添加 team_level 字段
                if (schemaInspector.hasColumn(promoterTable, "team_level")) {
                    columns.add("team_level");
                    args.add(level);
                }
                String distributorCode = generateDistributorCode();
                if (schemaInspector.hasColumn(promoterTable, "distributor_code")) {
                    columns.add("distributor_code");
                    args.add(distributorCode);
                }
                result.put("distributorCode", distributorCode);
            }
            String sql = "INSERT INTO " + promoterTable + " (" + String.join(", ", columns) + ") VALUES (" +
                columns.stream().map(c -> "?").collect(Collectors.joining(", ")) + ")";
            jdbcTemplate.update(sql, args.toArray());
            result.put("message", isNewUser ? "新用户已创建并设为艺荐官，用户ID：" + userId : "已设为艺荐官，用户ID：" + userId);
        }
        
        appendIdentity(userId, "promoter");
        setPromoterLevel(userId, level);
        
        result.put("userId", userId);
        result.put("isNewUser", isNewUser);
        return result;
    }

    @Transactional
    public void updatePromoterStatus(Long userId, int status) {
        String promoterTable = promoterTable();
        List<String> setClauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (schemaInspector.hasColumn(promoterTable, "status")) {
            setClauses.add("status = ?");
            args.add(status);
        }
        setClauses.add(updateTimeAssignment(promoterTable));
        args.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        args.add(userId);
        jdbcTemplate.update("UPDATE " + promoterTable + " SET " + String.join(", ", setClauses) + " WHERE user_id = ?", args.toArray());
    }

    public Map<String, Object> getPromoterDetail(Long userId) {
        List<Map<String, Object>> rows = promoterListRows(1, 1, String.valueOf(userId), null);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> getPromoterTeam(Long userId, int page, int size) {
        String userTable = userTable();
        String promoterTable = promoterTable();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT child.user_id, child.level, child.sign_time,
                   u.nickname, u.phone,
                   (SELECT COUNT(*) FROM %s grandchild WHERE grandchild.inviter_id = child.user_id) AS team_count
            FROM %s child
            LEFT JOIN %s u ON child.user_id = u.id
            WHERE child.inviter_id = ?
            ORDER BY child.id DESC
            LIMIT ?, ?
            """.formatted(promoterTable, promoterTable, userTable),
            userId,
            (page - 1) * size,
            size
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", String.valueOf(row.get("user_id")));
            item.put("nickname", row.get("nickname"));
            item.put("phone", row.get("phone"));
            item.put("level", toInt(row.get("level"), 1));
            item.put("joinTime", formatDate(row.get("sign_time")));
            item.put("teamCount", toInt(row.get("team_count"), 0));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getPromoterCommission(Long userId, int page, int size) {
        String commissionTable = commissionTable();
        if (commissionTable == null || schemaInspector.getColumns(commissionTable).isEmpty()) {
            return List.of();
        }
        String promoterRefColumn = schemaInspector.firstExistingColumn(commissionTable, "promoter_id", "user_id");
        String orderNoColumn = schemaInspector.firstExistingColumn(commissionTable, "order_no", "order_no");
        String amountColumn = schemaInspector.firstExistingColumn(commissionTable, "order_amount", "order_amount");
        String rateColumn = schemaInspector.firstExistingColumn(commissionTable, "commission_rate");
        String commissionAmountColumn = schemaInspector.firstExistingColumn(commissionTable, "commission_amount");
        String timeColumn = schemaInspector.firstExistingColumn(commissionTable, "create_time", "settle_time");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT id, %s AS order_no, %s AS order_amount, %s AS commission_rate,
                   %s AS commission_amount, status, %s AS record_time
            FROM %s
            WHERE %s = ?
            ORDER BY id DESC
            LIMIT ?, ?
            """.formatted(orderNoColumn, amountColumn, rateColumn, commissionAmountColumn, timeColumn, commissionTable, promoterRefColumn),
            userId,
            (page - 1) * size,
            size
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("orderNo", row.get("order_no"));
            item.put("amount", row.get("order_amount"));
            item.put("rate", row.get("commission_rate"));
            item.put("commission", row.get("commission_amount"));
            item.put("status", toInt(row.get("status"), 0) == 1 ? "settled" : "pending");
            item.put("time", formatDateTime(row.get("record_time")));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> promoterListRows(int page, int size, String userId, String level) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = listPromoters(page, size, userId, level, null, null, null, null, null, null, null);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) payload.get("list");
        return list;
    }

    private Map<String, Object> promoterStats() {
        String promoterTable = promoterTable();
        Map<String, Object> stats = new LinkedHashMap<>();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + promoterTable, Long.class);
        Long monthlyNew = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + promoterTable + " WHERE DATE_FORMAT(sign_time, '%Y-%m') = DATE_FORMAT(?, '%Y-%m')",
            Long.class,
            LocalDate.now()
        );
        stats.put("total", total == null ? 0 : total);
        stats.put("monthlyNew", monthlyNew == null ? 0 : monthlyNew);
        stats.put("pendingCommission", pendingCommission());
        return stats;
    }

    private Number pendingCommission() {
        String commissionTable = commissionTable();
        if (commissionTable == null || schemaInspector.getColumns(commissionTable).isEmpty()) {
            return 0;
        }
        String statusColumn = schemaInspector.firstExistingColumn(commissionTable, "status");
        String amountColumn = schemaInspector.firstExistingColumn(commissionTable, "commission_amount");
        Number value = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(" + amountColumn + "), 0) FROM " + commissionTable + " WHERE " + statusColumn + " = 0",
            Number.class
        );
        return value == null ? 0 : value;
    }

    private Map<String, Object> mapUserRow(Map<String, Object> row) {
        List<String> identities = normalizeIdentities(row.get("identities_value"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("userId", toLong(row.get("id")));
        item.put("uid", row.get("uid")); // 标准化用户UID
        item.put("nickname", row.get("nickname"));
        // 支持 avatar 或 avatar_url 列名
        item.put("avatar", row.get("avatar") != null ? row.get("avatar") : row.get("avatar_url"));
        // 支持 phone 或 mobile 列名
        item.put("phone", row.get("phone") != null ? row.get("phone") : row.get("mobile"));
        item.put("email", null);
        item.put("isVip", false);
        item.put("isArtist", identities.contains("artist"));
        item.put("isPromoter", identities.contains("promoter"));
        item.put("balance", row.get("available_commission_value"));
        item.put("couponCount", 0);
        item.put("totalConsume", row.get("total_commission_value"));
        item.put("orderCount", 0);
        // 支持 register_time 或 created_at 列名
        String registerTime = row.get("register_time") != null ? formatDateTime(row.get("register_time")) : formatDateTime(row.get("created_at"));
        item.put("registerTime", registerTime);
        item.put("source", "wechat");
        item.put("status", toInt(row.get("status"), 1) == 1 ? "normal" : "disabled");
        item.put("identities", identities);
        return item;
    }

    private Map<String, Object> mapArtistRow(Map<String, Object> row) {
        int status = toInt(row.get("artist_status"), 0);
        // 处理 user_id 可能是字符串 uid 或数字 id 的情况
        Object userIdValue = row.get("user_id");
        String userIdStr = userIdValue != null ? userIdValue.toString() : "0";
        String artistCode = String.valueOf(row.get("artist_code"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", toLong(row.get("id")));
        // 返回字符串形式的 uid
        item.put("userId", userIdStr);
        item.put("uid", userIdStr);
        // 显示 user_uid（用户UID）
        item.put("displayId", userIdStr);
        item.put("nickname", row.get("nickname"));
        item.put("phone", row.get("phone"));
        // 支持 avatar 或 avatar_url 列名
        item.put("avatar", row.get("avatar") != null ? row.get("avatar") : row.get("avatar_url"));
        // 支持 real_name 或 artist_name 列名
        item.put("realName", row.get("real_name") != null ? row.get("real_name") : row.get("artist_name"));
        item.put("idCard", row.get("id_card"));
        item.put("status", status);
        item.put("badge", normalizeBadge(row.get("artist_level_value")));
        item.put("resume", row.get("artist_resume"));
        item.put("bio", row.get("artist_resume"));
        item.put("images", normalizeAttachmentField(row.get("artist_works")));
        item.put("artworks", normalizeAttachmentField(row.get("artist_works")));
        item.put("exhibits", normalizeAttachmentField(row.get("artist_exhibits")));
        item.put("artworkCount", toInt(row.get("artwork_count"), 0));
        item.put("rejectReason", row.get("reject_reason"));
        item.put("createTime", formatDateTime(row.get("create_time")));
        item.put("createdAt", formatDateTime(row.get("create_time")));
        item.put("certified", status == 1);
        item.put("hidden", status == 3);
        return item;
    }

    private Map<String, Object> mapPromoterRow(Map<String, Object> row) {
        int status = toInt(row.get("status"), 1);
        // 处理 user_id 可能是字符串 uid 或数字 id 的情况
        Object userIdValue = row.get("user_id");
        String userIdStr = userIdValue != null ? userIdValue.toString() : "0";
        String promoterCode = String.valueOf(row.get("promoter_code"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", toLong(row.get("id")));
        item.put("userId", userIdStr);
        item.put("uid", row.get("uid_value"));
        // 显示 user_uid（用户UID）
        item.put("displayId", row.get("uid_value") != null ? row.get("uid_value") : userIdStr);
        item.put("userNickname", row.get("nickname"));
        item.put("nickname", row.get("nickname"));
        // 支持 phone 或 mobile 列名
        item.put("userPhone", row.get("phone") != null ? row.get("phone") : row.get("mobile"));
        item.put("phone", row.get("phone") != null ? row.get("phone") : row.get("mobile"));
        // 支持 avatar 或 avatar_url 列名
        item.put("userAvatar", row.get("avatar") != null ? row.get("avatar") : row.get("avatar_url"));
        item.put("avatar", row.get("avatar") != null ? row.get("avatar") : row.get("avatar_url"));
        item.put("level", toInt(row.get("level"), 1));
        item.put("teamCount", toInt(row.get("team_size"), 0));
        item.put("directCount", toInt(row.get("direct_count"), 0));
        item.put("inviterUid", row.get("inviter_uid"));
        item.put("parentUid", row.get("parent_uid") != null ? row.get("parent_uid") : row.get("inviter_uid"));
        item.put("parentName", row.get("parent_name"));
        item.put("totalCommission", row.get("total_commission_value"));
        item.put("withdrawable", row.get("available_commission_value"));
        // 动态获取 sign_time 或 agreement_time 或 create_time
        Object signTime = row.get("sign_time");
        if (signTime == null) {
            signTime = row.get("agreement_time");
        }
        if (signTime == null) {
            signTime = row.get("created_at");
        }
        if (signTime == null) {
            signTime = row.get("create_time");
        }
        item.put("becomeTime", formatDateTime(signTime));
        item.put("createTime", formatDateTime(signTime));
        item.put("status", status);
        item.put("certified", status == 1);
        return item;
    }

    private String userUidExpression(String userAlias, String promoterAlias, boolean hasPromoterUid) {
        String expr = "CAST(" + promoterAlias + ".user_id AS CHAR)";
        if (schemaInspector.hasColumn(userTable(), "user_no")) {
            expr = "COALESCE(" + userAlias + ".user_no, " + expr + ")";
        }
        if (schemaInspector.hasColumn(userTable(), "user_uid")) {
            expr = "COALESCE(" + userAlias + ".user_uid, " + expr + ")";
        }
        if (schemaInspector.hasColumn(userTable(), "uid")) {
            expr = "COALESCE(" + userAlias + ".uid, " + expr + ")";
        }
        if (hasPromoterUid) {
            expr = "COALESCE(" + promoterAlias + ".user_uid, " + expr + ")";
        }
        return expr;
    }

    private Long countArtistByStatus(int status) {
        String artistTable = artistTable();
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + artistTable + " WHERE " + artistStatusColumn(artistTable) + " = ?",
            Long.class,
            status
        );
    }

    private void syncArtworkArtists() {
        String artworkTable = schemaInspector.resolveTable("artist_sync_artwork", "artwork", "artworks", "products", "product");
        String artistTable = artistTable();
        if (artworkTable == null || schemaInspector.getColumns(artworkTable).isEmpty()
            || artistTable == null || schemaInspector.getColumns(artistTable).isEmpty()
            || !schemaInspector.hasColumn(artworkTable, "author_name")) {
            return;
        }

        String authorIdSelect = schemaInspector.hasColumn(artworkTable, "author_id") ? "author_id" : "NULL";
        String authorUidSelect = schemaInspector.hasColumn(artworkTable, "author_uid") ? "author_uid" : "NULL";
        String avatarSelect = schemaInspector.hasColumn(artworkTable, "author_avatar") ? "MAX(author_avatar)" : "NULL";
        String bioSelect = schemaInspector.hasColumn(artworkTable, "author_bio") ? "MAX(author_bio)" : "NULL";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT %s AS author_id, %s AS author_uid, author_name, %s AS author_avatar, %s AS author_bio
            FROM %s
            WHERE author_name IS NOT NULL AND TRIM(author_name) <> ''
            GROUP BY %s, %s, author_name
            """.formatted(
                authorIdSelect,
                authorUidSelect,
                avatarSelect,
                bioSelect,
                artworkTable,
                authorIdSelect,
                authorUidSelect
            ));

        for (Map<String, Object> row : rows) {
            Long authorId = row.get("author_id") == null ? null : toLong(row.get("author_id"));
            String authorUid = Objects.toString(row.get("author_uid"), "").trim();
            String authorName = Objects.toString(row.get("author_name"), "").trim();
            if (authorName.isEmpty() || artistExistsForArtworkAuthor(authorId, authorUid, authorName)) {
                continue;
            }
            insertSyncedArtist(authorId, authorUid, authorName, Objects.toString(row.get("author_avatar"), ""), Objects.toString(row.get("author_bio"), ""));
            if (authorId != null && userExists(authorId)) {
                appendIdentity(authorId, "artist");
            }
        }
    }

    private boolean artistExistsForArtworkAuthor(Long authorId, String authorUid, String authorName) {
        String artistTable = artistTable();
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (authorId != null && schemaInspector.hasColumn(artistTable, "user_id")) {
            conditions.add("user_id = ?");
            args.add(authorId);
        }
        if (authorUid != null && !authorUid.isBlank() && schemaInspector.hasColumn(artistTable, "user_uid")) {
            conditions.add("user_uid = ?");
            args.add(authorUid);
        }
        String realNameCol = schemaInspector.firstExistingColumn(artistTable, "real_name", "realName", "artist_name", "name");
        if (schemaInspector.hasColumn(artistTable, realNameCol)) {
            conditions.add("BINARY " + realNameCol + " = BINARY ?");
            args.add(authorName);
        }
        if (conditions.isEmpty()) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + artistTable + " WHERE " + String.join(" OR ", conditions),
            Long.class,
            args.toArray()
        );
        return count != null && count > 0;
    }

    private void insertSyncedArtist(Long authorId, String authorUid, String authorName, String avatar, String bio) {
        String artistTable = artistTable();
        LocalDateTime now = LocalDateTime.now();
        String artistCode = generateArtistCode();
        Long userId = authorId;
        if (userId == null || !userExists(userId)) {
            userId = createUserForAdmin("", authorName, avatar, List.of("artist", "collector"));
            authorUid = getUserUidById(userId);
        }

        if ("artist_profile".equals(artistTable)) {
            jdbcTemplate.update("""
                INSERT INTO artist_profile (user_id, user_uid, artist_name, avatar_url, bio, style_tags, slogan, status, created_at, updated_at, artist_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                """,
                userId,
                nullableText(authorUid),
                authorName,
                nullableText(avatar),
                nullableText(bio),
                null,
                null,
                now,
                now,
                artistCode
            );
            return;
        }

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        addInsertValue(columns, values, artistTable, "user_id", userId);
        addInsertValue(columns, values, artistTable, "user_uid", nullableText(authorUid));
        addInsertValue(columns, values, artistTable, "real_name", authorName);
        addInsertValue(columns, values, artistTable, "artist_name", authorName);
        addInsertValue(columns, values, artistTable, "name", authorName);
        addInsertValue(columns, values, artistTable, "id_card", null);
        addInsertValue(columns, values, artistTable, "bio", nullableText(bio));
        addInsertValue(columns, values, artistTable, "resume", nullableText(bio));
        addInsertValue(columns, values, artistTable, "artist_resume", nullableText(bio));
        addInsertValue(columns, values, artistTable, "artist_code", artistCode);
        addInsertValue(columns, values, artistTable, "avatar_url", nullableText(avatar));
        addInsertValue(columns, values, artistTable, "avatar", nullableText(avatar));
        addInsertValue(columns, values, artistTable, artistStatusColumn(artistTable), 1);
        addInsertValue(columns, values, artistTable, createTimeColumn(artistTable), now);
        addInsertValue(columns, values, artistTable, "update_time", now);
        addInsertValue(columns, values, artistTable, "updated_at", now);
        addInsertValue(columns, values, artistTable, "cert_time", now);
        addInsertValue(columns, values, artistTable, "review_time", now);

        String placeholders = columns.stream().map(col -> "?").collect(Collectors.joining(", "));
        jdbcTemplate.update(
            "INSERT INTO " + artistTable + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")",
            values.toArray()
        );
    }

    private void addInsertValue(List<String> columns, List<Object> values, String table, String column, Object value) {
        if (column != null && !columns.contains(column) && schemaInspector.hasColumn(table, column)) {
            columns.add(column);
            values.add(value);
        }
    }

    private boolean userExists(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + userTable() + " WHERE id = ?", Long.class, userId);
        return count != null && count > 0;
    }

    private Long createUserForAdmin(String phone, String nickname, String avatar, List<String> identities) {
        String userTable = userTable();
        LocalDateTime now = LocalDateTime.now();
        Long userId;
        
        // nickname 截断处理，防止超过数据库字段长度限制
        String finalNickname = nickname.length() > 50 ? nickname.substring(0, 50) : nickname;
        
        if ("users".equals(userTable)) {
            String avatarColumn = avatarColumn(userTable);
            jdbcTemplate.update("""
                INSERT INTO users (nickname, phone, %s, identities, status, register_time, create_time, update_time)
                VALUES (?, ?, ?, ?, 1, ?, ?, ?)
                """.formatted(avatarColumn),
                finalNickname,
                phone,
                avatar.isEmpty() ? null : avatar,
                String.join(",", identities),
                now,
                now,
                now
            );
            userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else if ("user_account".equals(userTable)) {
            // user_account 表：生成 user_uid 和 user_no，nickname 限制100字符
            String userUid = generateUserUid();
            // 添加4位随机后缀避免时间戳冲突
            String userNo = "U" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
            String avatarColumn = avatarColumn(userTable);
            String safeNickname = nickname.length() > 100 ? nickname.substring(0, 100) : nickname;
            jdbcTemplate.update("""
                INSERT INTO user_account (nickname, mobile, %s, status, register_source, created_at, updated_at, user_uid, user_no)
                VALUES (?, ?, ?, 'ENABLED', 'ADMIN', ?, ?, ?, ?)
                """.formatted(avatarColumn),
                safeNickname,
                phone,
                avatar.isEmpty() ? null : avatar,
                now,
                now,
                userUid,
                userNo
            );
            userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            // sys_user 表：nickname 限制64字符
            String safeNickname64 = nickname.length() > 64 ? nickname.substring(0, 64) : nickname;
            jdbcTemplate.update("""
                INSERT INTO sys_user (nickname, phone, avatar, status, identity, identity_json, create_time, update_time)
                VALUES (?, ?, ?, 1, ?, ?, ?, ?)
                """,
                safeNickname64,
                phone,
                avatar.isEmpty() ? null : avatar,
                primaryIdentity(identities),
                toIdentityJson(identities),
                now,
                now
            );
            userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
        return userId;
    }
    
    private void updateUserAvatar(Long userId, String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return;
        }
        String userTable = userTable();
        String avatarColumn = avatarColumn(userTable);
        if (schemaInspector.hasColumn(userTable, avatarColumn)) {
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET " + avatarColumn + " = ?, " + updateTimeAssignment(userTable) + " WHERE id = ?",
                avatar,
                LocalDateTime.now(),
                userId
            );
        }
    }

    public Long findUserIdByPhone(String phone) {
        String userTable = userTable();
        String phoneCol = schemaInspector.firstExistingColumn(userTable, "phone", "mobile");
        if (phoneCol == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id FROM " + userTable + " WHERE " + phoneCol + " = ? LIMIT 1", 
            phone
        );
        return rows.isEmpty() ? null : toLong(rows.get(0).get("id"));
    }

    public Long createUser(String phone, String nickname, List<String> identities) {
        return createUserForAdmin(phone, nickname, "", identities);
    }

    private Map<String, Object> findUserRow(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id FROM " + userTable() + " WHERE id = ? LIMIT 1", userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void setArtistBadge(Long userId, String badge) {
        String userTable = userTable();
        if (schemaInspector.hasColumn(userTable, "artist_level")) {
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET artist_level = ?, " + updateTimeAssignment(userTable) + " WHERE id = ?",
                badge,
                LocalDateTime.now(),
                userId
            );
        }
    }

    private void setPromoterLevel(Long userId, int level) {
        String userTable = userTable();
        if (schemaInspector.hasColumn(userTable, "promoter_level")) {
            jdbcTemplate.update(
                "UPDATE " + userTable + " SET promoter_level = ?, " + updateTimeAssignment(userTable) + " WHERE id = ?",
                "level_" + level,
                LocalDateTime.now(),
                userId
            );
        }
    }

    private void appendIdentity(Long userId, String identity) {
        String userTable = userTable();
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT " + identityColumn(userTable) + " AS identities_value FROM " + userTable + " WHERE id = ?",
            userId
        );
        List<String> identities = normalizeIdentities(row.get("identities_value"));
        if (!identities.contains(identity)) {
            identities = new ArrayList<>(identities);
            identities.add(identity);
            writeIdentities(userId, identities);
        }
    }

    private void removeIdentity(Long userId, String identity) {
        String userTable = userTable();
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT " + identityColumn(userTable) + " AS identities_value FROM " + userTable + " WHERE id = ?",
            userId
        );
        List<String> identities = new ArrayList<>(normalizeIdentities(row.get("identities_value")));
        identities.removeIf(identity::equals);
        if (identities.isEmpty()) {
            identities.add("collector");
        }
        writeIdentities(userId, identities);
    }

    private void writeIdentities(Long userId, List<String> identities) {
        String userTable = userTable();
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("UPDATE " + userTable + " SET ");
        List<String> assignments = new ArrayList<>();
        if (schemaInspector.hasColumn(userTable, "identities")) {
            assignments.add("identities = ?");
            args.add(String.join(",", identities));
        }
        if (schemaInspector.hasColumn(userTable, "identity_json")) {
            assignments.add("identity_json = ?");
            args.add(toIdentityJson(identities));
        }
        if (schemaInspector.hasColumn(userTable, "identity")) {
            assignments.add("identity = ?");
            args.add(primaryIdentity(identities));
        }
        assignments.add(updateTimeAssignment(userTable));
        args.add(LocalDateTime.now());
        sql.append(String.join(", ", assignments)).append(" WHERE id = ?");
        args.add(userId);
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    private void syncPromoterIdentity(Long userId, List<String> identities) {
        String promoterTable = promoterTable();
        boolean hasPromoter = identities.contains("promoter");
        Integer existing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + promoterTable + " WHERE user_id = ?",
            Integer.class,
            userId
        );
        if (hasPromoter) {
            if (existing == null || existing == 0) {
                // 创建艺荐官记录 - 动态构建列名
                LocalDateTime now = LocalDateTime.now();
                List<String> columns = new ArrayList<>();
                List<Object> args = new ArrayList<>();
                columns.add("user_id");
                args.add(userId);
                if (schemaInspector.hasColumn(promoterTable, "level")) {
                    columns.add("level");
                    args.add(1);
                }
                if (schemaInspector.hasColumn(promoterTable, "status")) {
                    columns.add("status");
                    args.add(1);
                }
                if (schemaInspector.hasColumn(promoterTable, "sign_time")) {
                    columns.add("sign_time");
                    args.add(now);
                }
                if (schemaInspector.hasColumn(promoterTable, "create_time")) {
                    columns.add("create_time");
                    args.add(now);
                }
                if (schemaInspector.hasColumn(promoterTable, "update_time")) {
                    columns.add("update_time");
                    args.add(now);
                }
                String sql = "INSERT INTO " + promoterTable + " (" + String.join(", ", columns) + ") VALUES (" +
                    columns.stream().map(c -> "?").collect(Collectors.joining(", ")) + ")";
                jdbcTemplate.update(sql, args.toArray());
            } else {
                // 更新艺荐官状态
                List<String> setClauses = new ArrayList<>();
                List<Object> updateArgs = new ArrayList<>();
                if (schemaInspector.hasColumn(promoterTable, "status")) {
                    setClauses.add("status = ?");
                    updateArgs.add(1);
                }
                setClauses.add(updateTimeAssignment(promoterTable));
                updateArgs.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));
                String sql = "UPDATE " + promoterTable + " SET " + String.join(", ", setClauses) + " WHERE user_id = ?";
                updateArgs.add(userId);
                jdbcTemplate.update(sql, updateArgs.toArray());
            }
        } else if (existing != null && existing > 0) {
            // 禁用艺荐官状态
            List<String> setClauses = new ArrayList<>();
            List<Object> updateArgs = new ArrayList<>();
            if (schemaInspector.hasColumn(promoterTable, "status")) {
                setClauses.add("status = ?");
                updateArgs.add(0);
            }
            setClauses.add(updateTimeAssignment(promoterTable));
            updateArgs.add(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            String sql = "UPDATE " + promoterTable + " SET " + String.join(", ", setClauses) + " WHERE user_id = ?";
            updateArgs.add(userId);
            jdbcTemplate.update(sql, updateArgs.toArray());
        }
    }

    private void appendIdentityFilter(StringBuilder where, List<Object> args, String identityColumn, String identity) {
        if (identity == null || identity.isBlank()) {
            return;
        }
        if ("user".equals(identity)) {
            where.append(" AND ").append(identityColumn).append(" NOT LIKE ? AND ").append(identityColumn).append(" NOT LIKE ?");
            args.add("%artist%");
            args.add("%promoter%");
            return;
        }
        where.append(" AND ").append(identityColumn).append(" LIKE ?");
        args.add("%" + identity + "%");
    }

    private List<String> normalizeIdentities(Object rawValue) {
        String raw = Objects.toString(rawValue, "").trim();
        if (raw.isEmpty()) {
            return new ArrayList<>(List.of("collector"));
        }
        String cleaned = raw.replace("[", "").replace("]", "").replace("\"", "");
        Set<String> identities = new LinkedHashSet<>(Arrays.stream(cleaned.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toList()));
        if (identities.isEmpty()) {
            identities.add("collector");
        }
        return new ArrayList<>(identities);
    }

    private String toIdentityJson(List<String> identities) {
        return identities.stream()
            .map(identity -> "\"" + identity + "\"")
            .collect(Collectors.joining(",", "[", "]"));
    }

    private String primaryIdentity(List<String> identities) {
        if (identities.contains("artist")) {
            return "artist";
        }
        if (identities.contains("promoter")) {
            return "promoter";
        }
        if (identities.contains("agent")) {
            return "agent";
        }
        return "collector";
    }

    private String normalizeBadge(Object badgeValue) {
        String badge = Objects.toString(badgeValue, "").trim();
        return badge.isEmpty() ? "" : badge;
    }

    /**
     * 生成19位用户UID
     * 格式: USR + YYYYMMDD + 4位序列号 + 4位随机码
     */
    private String generateUserUid() {
        String userTable = userTable();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 动态获取uid列名
        String uidCol = schemaInspector.firstExistingColumn(userTable, "uid", "user_uid", "user_no");
        if (uidCol == null) {
            uidCol = "uid";
        }
        
        // 获取当前最大序列号
        Long maxSeq = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(" + uidCol + ", 13, 4) AS UNSIGNED)), 0) FROM " + userTable + " WHERE " + uidCol + " LIKE ?",
            Long.class,
            "USR" + date + "%"
        );
        String seq = String.format("%04d", (maxSeq != null ? maxSeq + 1 : 1));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "USR" + date + seq + random;
    }

    /**
     * 根据用户ID获取用户UID
     */
    private String getUserUidById(Long userId) {
        if (userId == null) return null;
        String userTable = userTable();
        if (schemaInspector.hasColumn(userTable, "uid")) {
            return jdbcTemplate.queryForObject("SELECT uid FROM " + userTable + " WHERE id = ?", String.class, userId);
        } else if (schemaInspector.hasColumn(userTable, "user_uid")) {
            return jdbcTemplate.queryForObject("SELECT user_uid FROM " + userTable + " WHERE id = ?", String.class, userId);
        } else {
            // 如果没有uid字段，使用旧式user_no
            return jdbcTemplate.queryForObject("SELECT user_no FROM " + userTable + " WHERE id = ?", String.class, userId);
        }
    }

    /**
     * 根据用户UID获取用户ID
     * @param uid 用户UID
     * @return 用户数字ID
     */
    public Long getUserIdByUid(String uid) {
        if (uid == null || uid.isEmpty()) {
            return null;
        }
        String userTable = userTable();
        try {
            // 尝试 uid 列
            if (schemaInspector.hasColumn(userTable, "uid")) {
                List<Long> results = jdbcTemplate.queryForList(
                    "SELECT id FROM " + userTable + " WHERE uid = ?", Long.class, uid);
                if (!results.isEmpty()) {
                    return results.get(0);
                }
            }
            // 尝试 user_uid 列
            if (schemaInspector.hasColumn(userTable, "user_uid")) {
                List<Long> results = jdbcTemplate.queryForList(
                    "SELECT id FROM " + userTable + " WHERE user_uid = ?", Long.class, uid);
                if (!results.isEmpty()) {
                    return results.get(0);
                }
            }
            // 尝试 user_no 列
            if (schemaInspector.hasColumn(userTable, "user_no")) {
                List<Long> results = jdbcTemplate.queryForList(
                    "SELECT id FROM " + userTable + " WHERE user_no = ?", Long.class, uid);
                if (!results.isEmpty()) {
                    return results.get(0);
                }
            }
        } catch (Exception e) {
            log.warn("根据UID {} 查询用户ID失败: {}", uid, e.getMessage());
        }
        return null;
    }

    /**
     * 生成19位艺术家编号
     * 格式: ART + YYYYMMDD + 4位序列号 + 4位随机码
     */
    private String generateArtistCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String table = schemaInspector.resolveTable("artist", "artist_profile", "artist_certifications");
        String codeCol = schemaInspector.firstExistingColumn(table, "artist_code");
        if (codeCol == null) {
            return null;
        }
        Long maxSeq = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(" + codeCol + ", 13, 4) AS UNSIGNED)), 0) FROM " + table + " WHERE " + codeCol + " LIKE ?",
            Long.class,
            "ART" + date + "%"
        );
        String seq = String.format("%04d", (maxSeq != null ? maxSeq + 1 : 1));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "ART" + date + seq + random;
    }

    /**
     * 生成19位艺荐官编号
     * 格式: DST + YYYYMMDD + 4位序列号 + 4位随机码
     */
    private String generateDistributorCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String table = schemaInspector.resolveTable("promoter", "distributor_profile", "promoter_record");
        String codeCol = schemaInspector.firstExistingColumn(table, "distributor_code", "promoter_code");
        if (codeCol == null) {
            return null;
        }
        String prefix = "DST";
        Long maxSeq = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(" + codeCol + ", 13, 4) AS UNSIGNED)), 0) FROM " + table + " WHERE " + codeCol + " LIKE ?",
            Long.class,
            prefix + date + "%"
        );
        String seq = String.format("%04d", (maxSeq != null ? maxSeq + 1 : 1));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return prefix + date + seq + random;
    }

    private String normalizeAttachmentField(Object value) {
        String raw = Objects.toString(value, "").trim();
        if (raw.isEmpty()) {
            return "";
        }
        return raw.replace("[", "").replace("]", "").replace("\"", "");
    }

    private String userTable() {
        return schemaInspector.resolveTable("user", "user_account", "users");
    }

    private String artistTable() {
        return schemaInspector.resolveTable("artist", "artist_profile", "artist_certifications", "artist_certification");
    }

    private String promoterTable() {
        return schemaInspector.resolveTable("promoter", "distributor_profile", "promoter_record");
    }

    private String commissionTable() {
        String table = schemaInspector.resolveTable("commission", "commission_logs", "commission_log");
        return table;
    }

    private String identityColumn(String tableName) {
        String col = schemaInspector.firstExistingColumn(tableName, "identities", "identity_json", "identity");
        return schemaInspector.hasColumn(tableName, col) ? col : "NULL";
    }

    private String createTimeColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "created_at", "create_time", "register_time", "createDate");
    }

    private String avatarColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "avatar", "avatar_url", "head_url", "icon");
    }

    private String artistStatusColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "is_signed", "status", "cert_status");
    }

    private String artistResumeColumn(String tableName) {
        String col = schemaInspector.firstExistingColumn(tableName, "bio", "resume", "artist_resume");
        return schemaInspector.hasColumn(tableName, col) ? "a." + col : "NULL";
    }

    private String artistWorksColumn(String tableName) {
        String col = schemaInspector.firstExistingColumn(tableName, "work_count", "artworks", "portfolio");
        return schemaInspector.hasColumn(tableName, col) ? "a." + col : "0";
    }

    private String artistExhibitsColumn(String tableName) {
        String col = schemaInspector.firstExistingColumn(tableName, "sale_count", "exhibits", "exhibition_proof");
        return schemaInspector.hasColumn(tableName, col) ? "a." + col : "0";
    }

    private String rejectReasonColumn(String tableName) {
        String col = schemaInspector.firstExistingColumn(tableName, "reject_reason", "rejection_reason", "reason");
        return schemaInspector.hasColumn(tableName, col) ? "a." + col : "NULL";
    }

    private String reviewTimeColumn(String tableName) {
        String col = schemaInspector.firstExistingColumn(tableName, "cert_time", "review_time", "approved_at");
        return schemaInspector.hasColumn(tableName, col) ? "a." + col : "NULL";
    }

    private int mapArtistStatus(String status) {
        return switch (status) {
            case "approved" -> 1;
            case "rejected" -> 2;
            case "hidden" -> 3;
            default -> 0;
        };
    }

    private String rejectReasonAssignment(String tableName) {
        String column = rejectReasonColumn(tableName);
        return "NULL".equals(column) ? "" : column + " = ?";
    }

    private String reviewTimeAssignment(String tableName) {
        String column = reviewTimeColumn(tableName);
        return "NULL".equals(column) ? "" : column + " = ?";
    }

    private String updateTimeAssignment(String tableName) {
        return schemaInspector.hasColumn(tableName, "update_time") ? "update_time = ?" : createTimeColumn(tableName) + " = ?";
    }

    private String columnOrNull(String tableName, String columnName) {
        return schemaInspector.hasColumn(tableName, columnName) ? columnName : "NULL";
    }
    
    private String artistColumnOrNull(String columnName) {
        String artistTable = artistTable();
        return schemaInspector.hasColumn(artistTable, columnName) ? "a." + columnName : "NULL";
    }
    
    private String userColumnOrNull(String columnName) {
        String userTable = userTable();
        return schemaInspector.hasColumn(userTable, columnName) ? "u." + columnName : "NULL";
    }

    private String columnOrZero(String tableName, String columnName) {
        return schemaInspector.hasColumn(tableName, columnName) ? columnName : "0";
    }

    private int currentPromoterLevel(Long userId) {
        Integer level = jdbcTemplate.queryForObject(
            "SELECT level FROM " + promoterTable() + " WHERE user_id = ? LIMIT 1",
            Integer.class,
            userId
        );
        return level == null ? 1 : level;
    }

    private String nullableText(Object value) {
        String text = Objects.toString(value, "").trim();
        return text.isEmpty() ? null : text;
    }

    private String formatDate(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate().toString();
        }
        return Objects.toString(value, "");
    }

    private String formatDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME_FORMATTER);
        }
        return Objects.toString(value, "");
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.toString(value, "0"));
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

    @Transactional
    public void deleteUser(Long userId) {
        String userTable = userTable();
        // 删除用户（逻辑删除）
        if (schemaInspector.hasColumn(userTable, "deleted")) {
            jdbcTemplate.update("UPDATE " + userTable + " SET deleted = 1, " + updateTimeAssignment(userTable) + " WHERE id = ?",
                LocalDateTime.now(), userId);
        } else {
            jdbcTemplate.update("DELETE FROM " + userTable + " WHERE id = ?", userId);
        }
    }

    @Transactional
    public void deleteArtist(Long id) {
        String artistTable = artistTable();
        // 获取艺术家关联的用户ID
        String userIdSelect = schemaInspector.hasColumn(artistTable, "user_id") ? "user_id" : "NULL";
        String userUidSelect = schemaInspector.hasColumn(artistTable, "user_uid") ? "user_uid" : "NULL";
        String realNameCol = schemaInspector.firstExistingColumn(artistTable, "real_name", "realName", "artist_name", "name");
        String realNameSelect = schemaInspector.hasColumn(artistTable, realNameCol) ? realNameCol : "NULL";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, " + userIdSelect + " AS user_id, " + userUidSelect + " AS user_uid, " +
                realNameSelect + " AS real_name FROM " + artistTable + " WHERE id = ? LIMIT 1",
            id
        );
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> artist = rows.get(0);
        Long artistRecordId = toLong(artist.get("id"));
        Long userId = artist.get("user_id") == null ? null : toLong(artist.get("user_id"));
        String userUid = Objects.toString(artist.get("user_uid"), "").trim();
        String realName = Objects.toString(artist.get("real_name"), "").trim();

        long artworkCount = countArtistArtworks(artistRecordId, userId, userUid, realName);
        if (artworkCount > 0) {
            throw new IllegalStateException("该艺术家名下还有 " + artworkCount + " 件作品，请先删除艺术家名下的作品");
        }

        // 删除艺术家认证记录
        jdbcTemplate.update("DELETE FROM " + artistTable + " WHERE id = ?", id);

        // 从用户身份中移除 artist
        if (userId != null) {
            removeIdentity(userId, "artist");
        }
    }

    /**
     * 批量删除艺术家
     * @param ids 艺术家记录ID列表
     */
    @Transactional
    public void batchDeleteArtist(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String artistTable = artistTable();
        
        // 获取这些艺术家记录对应的用户ID
        String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, " + 
            (schemaInspector.hasColumn(artistTable, "user_id") ? "user_id" : "NULL") + " AS user_id, " +
            (schemaInspector.hasColumn(artistTable, "user_uid") ? "user_uid" : "NULL") + " AS user_uid, " +
            (schemaInspector.hasColumn(artistTable, "real_name") ? "real_name" : 
                (schemaInspector.hasColumn(artistTable, "realName") ? "realName" : 
                    (schemaInspector.hasColumn(artistTable, "artist_name") ? "artist_name" : 
                        (schemaInspector.hasColumn(artistTable, "name") ? "name" : "NULL")))) + " AS real_name " +
            "FROM " + artistTable + " WHERE id IN (" + placeholders + ")",
            ids.toArray()
        );
        
        // 检查每个艺术家是否有作品
        for (Map<String, Object> row : rows) {
            Long artistRecordId = toLong(row.get("id"));
            Long userId = row.get("user_id") == null ? null : toLong(row.get("user_id"));
            String userUid = Objects.toString(row.get("user_uid"), "").trim();
            String realName = Objects.toString(row.get("real_name"), "").trim();
            
            long artworkCount = countArtistArtworks(artistRecordId, userId, userUid, realName);
            if (artworkCount > 0) {
                throw new IllegalStateException("艺术家 " + realName + " 名下还有 " + artworkCount + " 件作品，请先删除艺术家名下的作品");
            }
        }
        
        // 批量删除艺术家认证记录
        jdbcTemplate.update("DELETE FROM " + artistTable + " WHERE id IN (" + placeholders + ")", ids.toArray());
        
        // 从用户身份中移除 artist
        for (Map<String, Object> row : rows) {
            Long userId = row.get("user_id") == null ? null : toLong(row.get("user_id"));
            if (userId != null) {
                removeIdentity(userId, "artist");
            }
        }
        
        log.info("批量删除艺术家：{} 个", ids.size());
    }

    private long countArtistArtworks(Long artistRecordId, Long userId, String userUid, String realName) {
        String artworkTable = schemaInspector.resolveTable("artist_delete_artwork", "artwork", "artworks", "products", "product");
        if (artworkTable == null || schemaInspector.getColumns(artworkTable).isEmpty()) {
            return 0L;
        }

        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (artistRecordId != null && schemaInspector.hasColumn(artworkTable, "author_id")) {
            conditions.add("author_id = ?");
            args.add(artistRecordId);
        }
        if (userId != null && !Objects.equals(userId, artistRecordId) && schemaInspector.hasColumn(artworkTable, "author_id")) {
            conditions.add("author_id = ?");
            args.add(userId);
        }
        if (userUid != null && !userUid.isBlank() && schemaInspector.hasColumn(artworkTable, "author_uid")) {
            conditions.add("author_uid = ?");
            args.add(userUid);
        }
        if (realName != null && !realName.isBlank() && schemaInspector.hasColumn(artworkTable, "author_name")) {
            conditions.add("BINARY author_name = BINARY ?");
            args.add(realName);
        }
        if (conditions.isEmpty()) {
            return 0L;
        }

        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + artworkTable + " WHERE " + String.join(" OR ", conditions),
            Long.class,
            args.toArray()
        );
        return count == null ? 0L : count;
    }

    @Transactional
    public void deletePromoter(Long id) {
        String promoterTable = promoterTable();
        // 获取艺荐官关联的用户ID
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT user_id FROM " + promoterTable + " WHERE id = ? LIMIT 1", id);
        if (rows.isEmpty()) {
            return;
        }
        Long userId = toLong(rows.get(0).get("user_id"));

        // 删除艺荐官记录
        jdbcTemplate.update("DELETE FROM " + promoterTable + " WHERE id = ?", id);

        // 从用户身份中移除 promoter
        removeIdentity(userId, "promoter");
    }

    public Map<String, Object> listUserArtworks(Long userId, int page, int size) {
        // 获取作品表名
        String artworkTable = schemaInspector.resolveTable("user_artworks_table", "artwork", "artworks", "products", "product");
        if (artworkTable == null || schemaInspector.getColumns(artworkTable).isEmpty()) {
            return Map.of("list", List.of(), "total", 0);
        }

        // 动态获取列名
        String titleCol = schemaInspector.firstExistingColumn(artworkTable, "title", "name");
        String coverCol = schemaInspector.firstExistingColumn(artworkTable, "cover_image", "cover", "image", "thumbnail");
        String priceCol = schemaInspector.firstExistingColumn(artworkTable, "price", "reserve_price");
        String statusCol = schemaInspector.firstExistingColumn(artworkTable, "status", "audit_status");
        String createCol = createTimeColumn(artworkTable);

        // 获取作品总数
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + artworkTable + " WHERE author_id = ?",
            Long.class, userId);

        // 分页查询作品
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, %s AS title, %s AS cover, %s AS price, COALESCE(original_price, 0) AS original_price, 
                   author_name, category_id, art_type, size, year, ownership_type, stock, description,
                   %s AS status, %s AS create_time
            FROM %s
            WHERE author_id = ?
            ORDER BY id DESC
            LIMIT ?, ?
            """.formatted(titleCol, coverCol, priceCol, statusCol, createCol, artworkTable),
            userId, (page - 1) * size, size);

        List<Map<String, Object>> list = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("title"));
            item.put("cover", row.get("cover"));
            // 价格从分转换为元
            Long priceInFen = row.get("price") != null ? ((Number) row.get("price")).longValue() : 0L;
            item.put("price", priceInFen / 100.0);
            // 原价也从分转换为元
            Object originalPriceObj = row.get("original_price");
            if (originalPriceObj != null) {
                Long originalPriceInFen = ((Number) originalPriceObj).longValue();
                item.put("originalPrice", originalPriceInFen / 100.0);
            } else {
                item.put("originalPrice", null);
            }
            item.put("authorName", row.get("author_name"));
            item.put("categoryId", row.get("category_id"));
            item.put("artType", row.get("art_type"));
            item.put("size", row.get("size"));
            item.put("year", row.get("year"));
            item.put("ownershipType", row.get("ownership_type"));
            item.put("stock", row.get("stock"));
            item.put("description", row.get("description"));
            item.put("status", toInt(row.get("status"), 0));
            item.put("createTime", formatDateTime(row.get("create_time")));
            return item;
        }).toList();

        return Map.of("list", list, "total", total);
    }

    public Map<String, Object> listUserSales(Long userId, int page, int size) {
        // 获取订单表和佣金表
        String orderTable = schemaInspector.resolveTable("user_sales_order_table", "orders", "order");
        String commissionTable = commissionTable();

        if (orderTable == null || schemaInspector.getColumns(orderTable).isEmpty()) {
            return Map.of("list", List.of(), "total", 0);
        }

        // 动态获取列名
        String orderNoCol = schemaInspector.firstExistingColumn(orderTable, "order_no", "orderNo");
        String amountCol = schemaInspector.firstExistingColumn(orderTable, "order_amount", "amount", "total_amount");
        String timeCol = createTimeColumn(orderTable);
        String artworkIdCol = schemaInspector.firstExistingColumn(orderTable, "artwork_id", "product_id");
        String buyerCol = schemaInspector.firstExistingColumn(orderTable, "buyer_user_id", "user_id");

        // 获取销售总数（艺荐官作为推广人获得的订单）
        String promoterRefCol = schemaInspector.firstExistingColumn(commissionTable, "promoter_id", "user_id");
        Long total = 0L;
        if (commissionTable != null && !schemaInspector.getColumns(commissionTable).isEmpty()) {
            total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + commissionTable + " WHERE " + promoterRefCol + " = ?",
                Long.class, userId);
        }

        // 查询销售记录
        List<Map<String, Object>> rows;
        if (commissionTable != null && !schemaInspector.getColumns(commissionTable).isEmpty()) {
            String commissionCol = schemaInspector.firstExistingColumn(commissionTable, "commission_amount", "commission");
            String statusCol = schemaInspector.firstExistingColumn(commissionTable, "status");

            rows = jdbcTemplate.queryForList("""
                SELECT c.id, c.%s AS order_no, c.%s AS amount, c.%s AS commission,
                       c.%s AS status, c.create_time AS order_time,
                       o.%s AS artwork_id
                FROM %s c
                LEFT JOIN %s o ON c.order_id = o.id
                WHERE c.%s = ?
                ORDER BY c.id DESC
                LIMIT ?, ?
                """.formatted(orderNoCol, amountCol, commissionCol, statusCol, artworkIdCol,
                    commissionTable, orderTable, promoterRefCol),
                userId, (page - 1) * size, size);
        } else {
            // 如果没有佣金表，直接查订单
            rows = jdbcTemplate.queryForList("""
                SELECT id, %s AS order_no, %s AS amount, %s AS artwork_id, %s AS buyer_id, %s AS order_time
                FROM %s
                WHERE promoter_id = ?
                ORDER BY id DESC
                LIMIT ?, ?
                """.formatted(orderNoCol, amountCol, artworkIdCol, buyerCol, timeCol, orderTable),
                userId, (page - 1) * size, size);
        }

        List<Map<String, Object>> list = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("orderNo", row.get("order_no"));
            item.put("amount", row.get("amount"));
            item.put("commission", row.get("commission"));
            item.put("artworkId", row.get("artwork_id"));
            item.put("buyerId", row.get("buyer_id"));
            item.put("orderTime", formatDateTime(row.get("order_time")));
            return item;
        }).toList();

        return Map.of("list", list, "total", total);
    }

    public Map<String, Object> listUserSharing(Long userId, int page, int size) {
        // 获取分享记录表（可能是 artworks 表中的 share_count 或独立的 share 表）
        String shareTable = schemaInspector.resolveTable("user_sharing_table", "share_records", "artwork_shares", "shares");
        if (shareTable == null || schemaInspector.getColumns(shareTable).isEmpty()) {
            // 如果没有分享表，返回空
            return Map.of("list", List.of(), "total", 0);
        }

        String userCol = schemaInspector.firstExistingColumn(shareTable, "user_id", "promoter_id", "sharer_id");
        String artworkCol = schemaInspector.firstExistingColumn(shareTable, "artwork_id", "product_id");
        String timeCol = createTimeColumn(shareTable);
        String shareCol = schemaInspector.firstExistingColumn(shareTable, "share_count", "view_count", "click_count");

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + shareTable + " WHERE " + userCol + " = ?",
            Long.class, userId);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, %s AS artwork_id, %s AS share_time, %s AS share_count
            FROM %s
            WHERE %s = ?
            ORDER BY id DESC
            LIMIT ?, ?
            """.formatted(artworkCol, timeCol, shareCol, shareTable, userCol),
            userId, (page - 1) * size, size);

        List<Map<String, Object>> list = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("artworkId", row.get("artwork_id"));
            item.put("shareTime", formatDateTime(row.get("share_time")));
            item.put("shareCount", row.get("share_count"));
            return item;
        }).toList();

        return Map.of("list", list, "total", total);
    }
}
