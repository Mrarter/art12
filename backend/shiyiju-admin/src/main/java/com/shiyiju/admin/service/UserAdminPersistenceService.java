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
            where.append(" AND phone LIKE ?");
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
            SELECT id, nickname, avatar, phone, status, %s AS register_time, %s AS identities_value,
                   %s AS promoter_level_value,
                   %s AS available_commission_value,
                   %s AS total_commission_value
            FROM %s
            """.formatted(
                createTimeColumn,
                identityColumn,
                columnOrNull(userTable, "promoter_level"),
                columnOrZero(userTable, "available_commission"),
                columnOrZero(userTable, "total_commission"),
                userTable
            );
        String orderByClause = " ORDER BY " + createTimeColumn + " DESC, id DESC LIMIT ?, ?";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            baseSql + where + orderByClause,
            queryArgs.toArray()
        );

        List<Map<String, Object>> records = rows.stream()
            .map(this::mapUserRow)
            .collect(Collectors.toList());

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

        // nickname
        String nickname = Objects.toString(params.get("nickname"), "");
        if (schemaInspector.hasColumn(userTable, "nickname")) {
            setClauses.add("nickname = ?");
            args.add(nickname);
        }

        // phone
        String phone = Objects.toString(params.get("phone"), "");
        if (schemaInspector.hasColumn(userTable, "phone")) {
            setClauses.add("phone = ?");
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

    public Map<String, Object> getUserDetail(Long userId) {
        PageResult<Map<String, Object>> pageResult = listUsers(1, 1, String.valueOf(userId), null, null, null, null, null);
        return pageResult.getRecords().isEmpty() ? null : pageResult.getRecords().get(0);
    }

    public Map<String, Object> listArtists(int page, int size, String status) {
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

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + artistTable + " a" + where,
            Long.class,
            args.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT a.id, a.user_id, a.real_name, a.id_card, a.%s AS artist_resume,
                   a.%s AS artist_works, a.%s AS artist_exhibits,
                   a.%s AS artist_status, a.%s AS review_time,
                   %s AS reject_reason,
                   u.nickname, u.phone, u.avatar, %s AS artist_level_value,
                   a.%s AS create_time, a.artist_code
            FROM %s a
            LEFT JOIN %s u ON a.user_id = u.id
            """.formatted(
                artistResumeColumn,
                artistWorksColumn,
                artistExhibitsColumn,
                artistStatusColumn,
                reviewTimeColumn,
                columnOrNull(artistTable, rejectReasonColumn(artistTable)),
                columnOrNull(userTable, "artist_level"),
                schemaInspector.firstExistingColumn(artistTable, "create_time", "register_time"),
                artistTable,
                userTable
            ) + where + " ORDER BY a.id DESC LIMIT ?, ?",
            queryArgs.toArray()
        );

        Long pendingCount = countArtistByStatus(0);
        Long approvedCount = countArtistByStatus(1);
        Long rejectedCount = countArtistByStatus(2);

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
        return result;
    }

    @Transactional
    public void approveArtist(Long id, String badge) {
        String artistTable = artistTable();
        String userTable = userTable();
        Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM " + artistTable + " WHERE id = ?", Long.class, id);
        jdbcTemplate.update(
            "UPDATE " + artistTable + " SET " + artistStatusColumn(artistTable) + " = 1, " +
                rejectReasonAssignment(artistTable) + ", " + reviewTimeAssignment(artistTable) + ", " +
                updateTimeAssignment(artistTable) + " WHERE id = ?",
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            id
        );
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

        if (schemaInspector.hasColumn(artistTable, "update_time")) {
            jdbcTemplate.update("UPDATE " + artistTable + " SET update_time = ? WHERE id = LAST_INSERT_ID()", LocalDateTime.now());
        }
        setArtistBadge(userId, nullableText(params.get("badge")));

        // 返回用户ID信息
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("isNewUser", isNewUser);
        result.put("message", isNewUser ? "新用户已创建，用户ID：" + userId : "已添加到现有用户，用户ID：" + userId);
        return result;
    }

    public List<Map<String, Object>> listPromoters(int page, int size, String userId, String level, String status) {
        String userTable = userTable();
        String promoterTable = promoterTable();
        
        String statusCol = schemaInspector.firstExistingColumn(promoterTable, "status", "agreement_status", "cert_status");
        String levelCol = schemaInspector.firstExistingColumn(promoterTable, "level", "promoter_level");
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

        List<Object> countArgs = new ArrayList<>(args);
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + promoterTable + " p" + where,
            Long.class,
            countArgs.toArray()
        );

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        
        // 动态构建 SELECT 语句
        String statusSelect = hasStatusCol ? ", p." + statusCol + " AS status" : "";
        String signTimeCol = schemaInspector.firstExistingColumn(promoterTable, "sign_time", "agreement_time");
        boolean hasSignTimeCol = schemaInspector.hasColumn(promoterTable, signTimeCol);
        String signTimeSelect = hasSignTimeCol ? ", p." + signTimeCol + " AS sign_time" : "";
        String sql = """
            SELECT p.id, p.user_id, p.%s AS level, p.subordinate_count AS team_size%s%s,
                   u.nickname, u.phone, u.avatar, p.promoter_code,
                   %s AS promoter_level_value,
                   p.total_commission AS total_commission_value,
                   p.withdrawable_commission AS available_commission_value,
                   (SELECT COUNT(*) FROM %s child WHERE child.inviter_id = p.user_id) AS direct_count
            FROM %s p
            LEFT JOIN %s u ON p.user_id = u.id
            """.formatted(levelCol, statusSelect, signTimeSelect, columnOrNull(userTable, "promoter_level"), promoterTable, promoterTable, userTable)
            + where + " ORDER BY p.id DESC LIMIT ?, ?";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, queryArgs.toArray());

        List<Map<String, Object>> list = rows.stream()
            .map(this::mapPromoterRow)
            .collect(Collectors.toList());

        // 统计各状态数量
        Long pendingCountVal = countPromoterByStatus(0);
        Long approvedCountVal = countPromoterByStatus(1);
        Long rejectedCountVal = countPromoterByStatus(2);

        return List.of(
            Map.of(
                "list", list,
                "records", list,
                "total", total == null ? 0 : total,
                "pendingCount", pendingCountVal == null ? 0 : pendingCountVal,
                "approvedCount", approvedCountVal == null ? 0 : approvedCountVal,
                "rejectedCount", rejectedCountVal == null ? 0 : rejectedCountVal
            )
        );
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
                nickname = "用户" + phone.substring(phone.length() - 4);
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
        Map<String, Object> payload = listPromoters(page, size, userId, level, null).get(0);
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
        item.put("nickname", row.get("nickname"));
        item.put("avatar", row.get("avatar"));
        item.put("phone", row.get("phone"));
        item.put("email", null);
        item.put("isVip", false);
        item.put("isArtist", identities.contains("artist"));
        item.put("isPromoter", identities.contains("promoter"));
        item.put("balance", row.get("available_commission_value"));
        item.put("couponCount", 0);
        item.put("totalConsume", row.get("total_commission_value"));
        item.put("orderCount", 0);
        item.put("registerTime", formatDateTime(row.get("register_time")));
        item.put("source", "wechat");
        item.put("status", toInt(row.get("status"), 1) == 1 ? "normal" : "disabled");
        item.put("identities", identities);
        return item;
    }

    private Map<String, Object> mapArtistRow(Map<String, Object> row) {
        int status = toInt(row.get("artist_status"), 0);
        Long userId = toLong(row.get("user_id"));
        String artistCode = String.valueOf(row.get("artist_code"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", toLong(row.get("id")));
        item.put("userId", userId);
        // 优先使用新的artist_code，否则使用userId格式化
        item.put("displayId", (artistCode != null && !artistCode.equals("null") && !artistCode.isEmpty()) 
            ? artistCode 
            : String.format("%04d", userId));
        item.put("nickname", row.get("nickname"));
        item.put("phone", row.get("phone"));
        item.put("avatar", row.get("avatar"));
        item.put("realName", row.get("real_name"));
        item.put("idCard", row.get("id_card"));
        item.put("status", status);
        item.put("badge", normalizeBadge(row.get("artist_level_value")));
        item.put("resume", row.get("artist_resume"));
        item.put("bio", row.get("artist_resume"));
        item.put("images", normalizeAttachmentField(row.get("artist_works")));
        item.put("artworks", normalizeAttachmentField(row.get("artist_works")));
        item.put("exhibits", normalizeAttachmentField(row.get("artist_exhibits")));
        item.put("rejectReason", row.get("reject_reason"));
        item.put("createTime", formatDateTime(row.get("create_time")));
        item.put("createdAt", formatDateTime(row.get("create_time")));
        item.put("certified", status == 1);
        return item;
    }

    private Map<String, Object> mapPromoterRow(Map<String, Object> row) {
        int status = toInt(row.get("status"), 1);
        Long userId = toLong(row.get("user_id"));
        String promoterCode = String.valueOf(row.get("promoter_code"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", toLong(row.get("id")));
        item.put("userId", String.valueOf(userId));
        // 优先使用新的promoter_code，否则使用userId格式化
        item.put("displayId", (promoterCode != null && !promoterCode.equals("null") && !promoterCode.isEmpty()) 
            ? promoterCode 
            : String.format("%04d", userId));
        item.put("userNickname", row.get("nickname"));
        item.put("nickname", row.get("nickname"));
        item.put("userPhone", row.get("phone"));
        item.put("phone", row.get("phone"));
        item.put("userAvatar", row.get("avatar"));
        item.put("avatar", row.get("avatar"));
        item.put("level", toInt(row.get("level"), 1));
        item.put("teamCount", toInt(row.get("team_size"), 0));
        item.put("directCount", toInt(row.get("direct_count"), 0));
        item.put("totalCommission", row.get("total_commission_value"));
        item.put("withdrawable", row.get("available_commission_value"));
        // 动态获取 sign_time 或 agreement_time
        Object signTime = row.get("sign_time");
        if (signTime == null) {
            signTime = row.get("agreement_time");
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

    private Long countArtistByStatus(int status) {
        String artistTable = artistTable();
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + artistTable + " WHERE " + artistStatusColumn(artistTable) + " = ?",
            Long.class,
            status
        );
    }

    private Long createUserForAdmin(String phone, String nickname, String avatar, List<String> identities) {
        String userTable = userTable();
        LocalDateTime now = LocalDateTime.now();
        if ("users".equals(userTable)) {
            String avatarColumn = avatarColumn(userTable);
            jdbcTemplate.update("""
                INSERT INTO users (nickname, phone, %s, identities, status, register_time, create_time, update_time)
                VALUES (?, ?, ?, ?, 1, ?, ?, ?)
                """.formatted(avatarColumn),
                nickname,
                phone,
                avatar.isEmpty() ? null : avatar,
                String.join(",", identities),
                now,
                now,
                now
            );
        } else {
            jdbcTemplate.update("""
                INSERT INTO sys_user (nickname, phone, avatar, status, identity, identity_json, create_time, update_time)
                VALUES (?, ?, ?, 1, ?, ?, ?, ?)
                """,
                nickname,
                phone,
                avatar.isEmpty() ? null : avatar,
                primaryIdentity(identities),
                toIdentityJson(identities),
                now,
                now
            );
        }
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
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
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id FROM " + userTable() + " WHERE phone = ? LIMIT 1", phone);
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

    private String normalizeAttachmentField(Object value) {
        String raw = Objects.toString(value, "").trim();
        if (raw.isEmpty()) {
            return "";
        }
        return raw.replace("[", "").replace("]", "").replace("\"", "");
    }

    private String userTable() {
        return schemaInspector.resolveTable("user", "users", "sys_user");
    }

    private String artistTable() {
        return schemaInspector.resolveTable("artist", "artist_certifications", "artist_certification");
    }

    private String promoterTable() {
        return schemaInspector.resolveTable("promoter", "promoter_record");
    }

    private String commissionTable() {
        String table = schemaInspector.resolveTable("commission", "commission_logs", "commission_log");
        return table;
    }

    private String identityColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "identities", "identity_json", "identity");
    }

    private String createTimeColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "register_time", "create_time");
    }

    private String avatarColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "avatar", "head_url", "icon");
    }

    private String artistStatusColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "status", "cert_status");
    }

    private String artistResumeColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "resume", "bio");
    }

    private String artistWorksColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "artworks", "portfolio");
    }

    private String artistExhibitsColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "exhibits", "exhibition_proof");
    }

    private String rejectReasonColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "reject_reason");
    }

    private String reviewTimeColumn(String tableName) {
        return schemaInspector.firstExistingColumn(tableName, "review_time", "cert_time");
    }

    private int mapArtistStatus(String status) {
        return switch (status) {
            case "approved" -> 1;
            case "rejected" -> 2;
            default -> 0;
        };
    }

    private String rejectReasonAssignment(String tableName) {
        String column = rejectReasonColumn(tableName);
        return column + " = ?";
    }

    private String reviewTimeAssignment(String tableName) {
        return reviewTimeColumn(tableName) + " = ?";
    }

    private String updateTimeAssignment(String tableName) {
        return schemaInspector.hasColumn(tableName, "update_time") ? "update_time = ?" : createTimeColumn(tableName) + " = ?";
    }

    private String columnOrNull(String tableName, String columnName) {
        return schemaInspector.hasColumn(tableName, columnName) ? columnName : "NULL";
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
}
