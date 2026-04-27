package com.shiyiju.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户ID映射服务
 * 提供旧ID（数字）和新ID（19位字符串）之间的相互转换
 * 
 * 19位ID格式: [种类前缀(3位)][日期YYYYMMDD(8位)][4位序列号][4位随机码]
 * 用户ID示例: USR20260426001AK
 */
@Slf4j
@Service
public class UserIdMappingService {

    private final JdbcTemplate jdbcTemplate;

    public UserIdMappingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据旧ID（数字）获取新UID（19位字符串）
     * 
     * @param oldUserId 旧的用户ID（数字）
     * @return 新的用户UID
     */
    public Optional<String> getUidByOldId(Long oldUserId) {
        if (oldUserId == null) {
            return Optional.empty();
        }
        try {
            String sql = "SELECT uid FROM user_id_mapping WHERE old_user_id = ? LIMIT 1";
            String uid = jdbcTemplate.queryForObject(sql, String.class, oldUserId);
            return Optional.ofNullable(uid);
        } catch (Exception e) {
            log.warn("未找到旧用户ID {} 对应的UID", oldUserId);
            return Optional.empty();
        }
    }

    /**
     * 根据新UID获取旧ID（数字）
     * 
     * @param uid 新的用户UID（19位字符串）
     * @return 旧的用户ID
     */
    public Optional<Long> getOldIdByUid(String uid) {
        if (uid == null || uid.isEmpty()) {
            return Optional.empty();
        }
        try {
            String sql = "SELECT old_user_id FROM user_id_mapping WHERE uid = ? LIMIT 1";
            Long oldId = jdbcTemplate.queryForObject(sql, Long.class, uid);
            return Optional.ofNullable(oldId);
        } catch (Exception e) {
            log.warn("未找到UID {} 对应的旧用户ID", uid);
            return Optional.empty();
        }
    }

    /**
     * 安全获取用户UID，如果不存在则自动生成
     * 
     * @param userId 用户ID（可能是数字或字符串）
     * @return 用户UID
     */
    public String resolveUid(Object userId) {
        if (userId == null) {
            return null;
        }
        
        String idStr = String.valueOf(userId).trim();
        
        // 如果是19位标准UID，直接返回（格式: USR+15位）
        if (idStr.length() == 19 && idStr.startsWith("USR")) {
            return idStr;
        }
        
        // 如果是数字ID，尝试查找映射
        try {
            Long oldId = Long.parseLong(idStr);
            Optional<String> uid = getUidByOldId(oldId);
            if (uid.isPresent()) {
                return uid.get();
            }
            
            // 如果没有找到映射，可能是新用户或需要生成新UID
            // 对于新用户，调用此方法前应该已经生成了UID
            log.warn("用户ID {} 没有对应的映射记录", oldId);
            return null;
        } catch (NumberFormatException e) {
            // 不是有效的数字ID
            log.warn("无效的用户ID格式: {}", idStr);
            return null;
        }
    }

    /**
     * 安全获取旧用户ID（数字）
     * 
     * @param userId 用户ID（可能是数字或字符串）
     * @return 旧的用户ID
     */
    public Long resolveOldId(Object userId) {
        if (userId == null) {
            return null;
        }
        
        String idStr = String.valueOf(userId).trim();
        
        // 如果是数字ID，直接返回
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            // 是字符串ID，查找映射
        }
        
        Optional<Long> oldId = getOldIdByUid(idStr);
        return oldId.orElse(null);
    }

    /**
     * 创建新的用户ID映射
     * 
     * @param oldUserId 旧的用户ID
     * @return 新生成的UID
     */
    public String createMapping(Long oldUserId) {
        // 生成新的19位UID (格式: USR+日期+序列+随机)
        String newUid = generateUserUid(oldUserId);
        
        // 检查是否已存在映射
        Optional<String> existing = getUidByOldId(oldUserId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        try {
            String sql = "INSERT INTO user_id_mapping (old_user_id, uid) VALUES (?, ?)";
            jdbcTemplate.update(sql, oldUserId, newUid);
            log.info("为旧用户ID {} 创建了新映射: {}", oldUserId, newUid);
            return newUid;
        } catch (Exception e) {
            // 可能并发插入导致重复，尝试重新获取
            Optional<String> uid = getUidByOldId(oldUserId);
            if (uid.isPresent()) {
                return uid.get();
            }
            throw new RuntimeException("创建用户ID映射失败", e);
        }
    }

    /**
     * 生成用户UID
     * 格式: USR + yyyyMMdd + 4位序列号 + 4位随机码 = 19位
     */
    private String generateUserUid(Long userId) {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%04d", userId % 10000);
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "USR" + date + seq + random;
    }

    /**
     * 批量获取旧ID对应的UID
     * 
     * @param oldUserIds 旧的用户ID列表
     * @return UID列表
     */
    public java.util.List<String> batchGetUids(java.util.List<Long> oldUserIds) {
        if (oldUserIds == null || oldUserIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        String placeholders = String.join(",", oldUserIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = String.format("SELECT old_user_id, uid FROM user_id_mapping WHERE old_user_id IN (%s)", placeholders);
        
        java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql, oldUserIds.toArray());
        java.util.Map<Long, String> mapping = new java.util.HashMap<>();
        rows.forEach(row -> {
            mapping.put(((Number) row.get("old_user_id")).longValue(), (String) row.get("uid"));
        });
        
        return oldUserIds.stream()
            .map(id -> mapping.getOrDefault(id, null))
            .collect(java.util.stream.Collectors.toList());
    }
}
