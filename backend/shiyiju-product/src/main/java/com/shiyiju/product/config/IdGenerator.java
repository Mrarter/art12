package com.shiyiju.product.config;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ID生成器工具类
 * 格式: [种类前缀(3位)][日期YYYYMMDD(8位)][4位序列号][4位随机码]
 * 总长度: 19位
 * 
 * @author AI Assistant
 */
@Component
public class IdGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    // 静态计数器，格式: prefix+date -> sequence
    private static final AtomicIntegerCounter counter = new AtomicIntegerCounter();
    
    // ID前缀定义
    public static final String PREFIX_USER = "USR";      // 用户
    public static final String PREFIX_SESSION = "SES";   // 专场
    public static final String PREFIX_LOT = "LOT";       // 拍品
    public static final String PREFIX_POST = "POST";     // 帖子
    public static final String PREFIX_COMMENT = "CMT";   // 评论
    public static final String PREFIX_TOPIC = "TOP";     // 话题
    public static final String PREFIX_WITHDRAW = "WTH";  // 提现记录
    public static final String PREFIX_COMMISSION = "COM";// 佣金记录
    public static final String PREFIX_BID = "BID";      // 竞拍记录
    public static final String PREFIX_AFTERSALE = "ASM"; // 售后管理
    public static final String PREFIX_ARTWORK = "ART";  // 作品

    /**
     * 生成ID
     * @param prefix 种类前缀(3位)
     * @return 生成的ID
     */
    public static String generateId(String prefix) {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        int sequence = counter.getNextSequence(prefix + dateStr);
        String randomCode = generateRandomCode(4);
        
        return prefix + dateStr + String.format("%04d", sequence) + randomCode;
    }

    /**
     * 生成用户ID
     */
    public static String generateUserId() {
        return generateId(PREFIX_USER);
    }

    /**
     * 生成专场ID
     */
    public static String generateSessionId() {
        return generateId(PREFIX_SESSION);
    }

    /**
     * 生成拍品ID
     */
    public static String generateLotId() {
        return generateId(PREFIX_LOT);
    }

    /**
     * 生成帖子ID
     */
    public static String generatePostId() {
        return generateId(PREFIX_POST);
    }

    /**
     * 生成评论ID
     */
    public static String generateCommentId() {
        return generateId(PREFIX_COMMENT);
    }

    /**
     * 生成话题ID
     */
    public static String generateTopicId() {
        return generateId(PREFIX_TOPIC);
    }

    /**
     * 生成提现记录ID
     */
    public static String generateWithdrawId() {
        return generateId(PREFIX_WITHDRAW);
    }

    /**
     * 生成佣金记录ID
     */
    public static String generateCommissionId() {
        return generateId(PREFIX_COMMISSION);
    }

    /**
     * 生成竞拍记录ID
     */
    public static String generateBidId() {
        return generateId(PREFIX_BID);
    }

    /**
     * 生成售后记录ID
     */
    public static String generateAfterSaleId() {
        return generateId(PREFIX_AFTERSALE);
    }

    /**
     * 生成作品ID
     */
    public static String generateArtworkId() {
        return generateId(PREFIX_ARTWORK);
    }

    /**
     * 生成指定长度的随机码
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
     * 从ID中提取日期
     */
    public static String extractDate(String id) {
        if (id == null || id.length() < 12) {
            return null;
        }
        return id.substring(3, 11);
    }

    /**
     * 从ID中提取前缀
     */
    public static String extractPrefix(String id) {
        if (id == null || id.length() < 3) {
            return null;
        }
        return id.substring(0, 3);
    }

    /**
     * 验证ID格式是否合法
     */
    public static boolean isValid(String id) {
        if (id == null || id.length() != 19) {
            return false;
        }
        // 检查日期部分是否为有效日期
        String dateStr = id.substring(3, 11);
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return false;
        }
        // 检查序列号和随机码部分是否为数字和大写字母
        String suffix = id.substring(11);
        return suffix.matches("[A-Z0-9]{8}");
    }

    /**
     * 获取ID类型描述
     */
    public static String getTypeDescription(String id) {
        String prefix = extractPrefix(id);
        if (prefix == null) return "未知";
        
        switch (prefix) {
            case PREFIX_USER: return "用户";
            case PREFIX_SESSION: return "专场";
            case PREFIX_LOT: return "拍品";
            case PREFIX_POST: return "帖子";
            case PREFIX_COMMENT: return "评论";
            case PREFIX_TOPIC: return "话题";
            case PREFIX_WITHDRAW: return "提现记录";
            case PREFIX_COMMISSION: return "佣金记录";
            case PREFIX_BID: return "竞拍记录";
            case PREFIX_AFTERSALE: return "售后管理";
            case PREFIX_ARTWORK: return "作品";
            default: return "未知";
        }
    }

    /**
     * 线程安全的计数器
     */
    private static class AtomicIntegerCounter {
        private final java.util.concurrent.ConcurrentHashMap<String, AtomicInteger> counters = 
            new java.util.concurrent.ConcurrentHashMap<>();

        public int getNextSequence(String key) {
            AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));
            return counter.incrementAndGet();
        }

        public void reset(String key) {
            counters.remove(key);
        }

        public void resetAll() {
            counters.clear();
        }
    }
}
