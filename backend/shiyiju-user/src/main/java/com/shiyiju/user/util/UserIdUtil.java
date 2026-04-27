package com.shiyiju.user.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户UID生成工具类
 * 格式: [分类前缀(3位)][日期YYYYMMDD(8位)][4位序号][4位随机码]
 * 总长度: 19位
 * 
 * 示例: USR202604250001A7KM
 */
public class UserIdUtil {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    // 每日序列号计数器 (key格式: 日期)
    private static final java.util.concurrent.ConcurrentHashMap<String, AtomicInteger> counters = 
        new java.util.concurrent.ConcurrentHashMap<>();
    
    // ID前缀
    public static final String PREFIX_USER = "USR";  // 用户
    
    /**
     * 生成用户UID
     * 格式: USR + 日期(YYYYMMDD) + 4位序号 + 4位随机码
     * 总长度: 19位
     * 
     * @return 生成的UID，如 USR202604250001A7KM
     */
    public static synchronized String generateUid() {
        String today = LocalDate.now().format(DATE_FORMAT);
        
        // 获取或创建当日计数器
        AtomicInteger counter = counters.computeIfAbsent(today, k -> new AtomicInteger(0));
        
        // 获取序列号（4位，不足补0）
        int seq = counter.incrementAndGet();
        String seqStr = String.format("%04d", seq);
        
        // 生成4位随机码
        String randomCode = generateRandomCode(4);
        
        return PREFIX_USER + today + seqStr + randomCode;
    }
    
    /**
     * 生成通用ID
     * @param prefix 3位前缀
     * @return 生成的ID
     */
    public static synchronized String generateId(String prefix) {
        String today = LocalDate.now().format(DATE_FORMAT);
        AtomicInteger counter = counters.computeIfAbsent(today, k -> new AtomicInteger(0));
        int seq = counter.incrementAndGet();
        String seqStr = String.format("%04d", seq);
        String randomCode = generateRandomCode(4);
        return prefix + today + seqStr + randomCode;
    }
    
    /**
     * 生成随机字母数字码
     * @param length 码长度
     */
    private static String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * 验证ID格式是否合法
     * @param uid 待验证的UID
     * @return 是否合法
     */
    public static boolean isValid(String uid) {
        if (uid == null || uid.length() != 19) {
            return false;
        }
        // 检查前缀
        if (!uid.startsWith(PREFIX_USER)) {
            return false;
        }
        // 检查日期部分
        try {
            String dateStr = uid.substring(3, 11);
            LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (Exception e) {
            return false;
        }
        // 检查后缀为字母数字
        String suffix = uid.substring(11);
        return suffix.matches("[A-Z0-9]{8}");
    }
    
    /**
     * 从UID提取日期
     * @param uid UID
     * @return 日期字符串，如 20260425
     */
    public static String extractDate(String uid) {
        if (uid == null || uid.length() < 11) {
            return null;
        }
        return uid.substring(3, 11);
    }
    
    /**
     * 从UID提取前缀
     * @param uid UID
     * @return 前缀，如 USR
     */
    public static String extractPrefix(String uid) {
        if (uid == null || uid.length() < 3) {
            return null;
        }
        return uid.substring(0, 3);
    }
    
    /**
     * 获取UID类型描述
     * @param uid UID
     * @return 类型描述
     */
    public static String getTypeDescription(String uid) {
        String prefix = extractPrefix(uid);
        if (prefix == null) return "未知";
        
        switch (prefix) {
            case PREFIX_USER: return "用户";
            default: return "未知";
        }
    }
    
    /**
     * 重置计数器（用于测试）
     */
    public static void reset() {
        counters.clear();
    }
    
    /**
     * 重置指定日期的计数器
     */
    public static void reset(String date) {
        counters.remove(date);
    }
}
