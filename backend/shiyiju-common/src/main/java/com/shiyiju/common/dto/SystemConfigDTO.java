package com.shiyiju.common.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 系统配置DTO - 用于运营后台配置管理
 */
@Data
public class SystemConfigDTO implements Serializable {
    
    /** 交易设置 */
    private TradeConfig trade;
    
    /** 分销设置 */
    private PromotionConfig promotion;
    
    /** 价格增长设置 */
    private PriceGrowthConfig priceGrowth;
    
    /** 拍卖设置 */
    private AuctionConfig auction;
    
    /** 审核设置 */
    private AuditConfig audit;
    
    @Data
    public static class TradeConfig implements Serializable {
        private Integer orderTimeout = 30;           // 订单超时时间（分钟）
        private Integer refundDays = 7;              // 退款处理周期（工作日）
        private Boolean allowRepeatBuy = false;      // 允许重复购买
        private String priceUnit = "fen";            // 价格单位
    }
    
    @Data
    public static class PromotionConfig implements Serializable {
        private BigDecimal directCommission = new BigDecimal("5");      // 一级佣金比例
        private BigDecimal teamCommission = new BigDecimal("2");        // 二级佣金比例
        private String settlementType = "after_pay";                     // 结算时机
        private BigDecimal minWithdraw = new BigDecimal("100");         // 最低提现金额
        private BigDecimal withdrawFee = BigDecimal.ZERO;                // 提现手续费
        private Integer withdrawDays = 3;                              // 提现周期
        private String promoterCondition = "free";                       // 成为艺荐官条件
        private BigDecimal purchaseThreshold = new BigDecimal("1000");  // 消费门槛
    }
    
    @Data
    public static class PriceGrowthConfig implements Serializable {
        private Boolean enabled = true;                    // 开关
        
        // 时间因素
        private BigDecimal baseDailyRate = new BigDecimal("0.0002");    // 基础日增长率
        private BigDecimal matureDailyRate = new BigDecimal("0.0003");  // 成熟期日增长率
        private Integer matureDays = 30;                   // 成熟期天数阈值
        
        // 艺术家知名度系数
        private BigDecimal defaultBadgeRate = new BigDecimal("1.0");   // 普通
        private BigDecimal verifiedBadgeRate = new BigDecimal("1.5");  // 认证
        private BigDecimal popularBadgeRate = new BigDecimal("2.0");   // 人气
        private BigDecimal masterBadgeRate = new BigDecimal("3.0");    // 大师
        
        // 热度系数
        private Integer viewThreshold = 100;              // 浏览量阈值
        private BigDecimal viewRate = new BigDecimal("1.1");  // 浏览量加成系数
        private Integer favoriteThreshold = 5;            // 收藏量阈值
        private BigDecimal favoriteRate = new BigDecimal("1.1");  // 收藏量加成系数
        
        // 销售加成
        private BigDecimal saleRate = new BigDecimal("0.05");     // 单次销售加成
        private Integer maxSaleCount = 10;                       // 最多计算销售次数
        
        // 涨幅限制
        private BigDecimal maxGrowthMultiple = new BigDecimal("5.0"); // 最大涨幅倍数
    }
    
    @Data
    public static class AuctionConfig implements Serializable {
        private BigDecimal auctionDeposit = new BigDecimal("1000");  // 拍卖保证金
        private Boolean depositRefund = true;                        // 保证金退还
        private BigDecimal bidIncrement = new BigDecimal("100");     // 延时加价幅度
        private Integer delayCycles = 3;                            // 延时周期
        private Integer delayMinutes = 5;                           // 延时时长
    }
    
    @Data
    public static class AuditConfig implements Serializable {
        private Boolean artistAudit = true;     // 艺术家认证审核
        private Boolean artworkAudit = true;    // 作品审核
        private Boolean postAudit = false;      // 动态审核
        private Boolean sensitiveFilter = true; // 敏感词过滤
        private String sensitiveWords;          // 敏感词库
    }
}
