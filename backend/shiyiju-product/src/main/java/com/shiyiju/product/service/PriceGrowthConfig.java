package com.shiyiju.product.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 价格增长配置 - 从 Nacos 配置中心读取
 * 配置前缀: price.growth
 */
@Data
@Component
@ConfigurationProperties(prefix = "price.growth")
public class PriceGrowthConfig {
    
    /** 是否启用价格增长 */
    private Boolean enabled = true;
    
    // ==================== 时间因素 ====================
    /** 基础日增长率 */
    private BigDecimal baseDailyRate = new BigDecimal("0.0002");
    
    /** 成熟期日增长率 */
    private BigDecimal matureDailyRate = new BigDecimal("0.0003");
    
    /** 成熟期天数阈值 */
    private Integer matureDays = 30;
    
    // ==================== 艺术家知名度系数 ====================
    /** 普通艺术家 */
    private BigDecimal defaultBadgeRate = new BigDecimal("1.0");
    
    /** 认证艺术家 */
    private BigDecimal verifiedBadgeRate = new BigDecimal("1.5");
    
    /** 人气艺术家 */
    private BigDecimal popularBadgeRate = new BigDecimal("2.0");
    
    /** 大师级艺术家 */
    private BigDecimal masterBadgeRate = new BigDecimal("3.0");
    
    // ==================== 浏览量阈值 ====================
    private Integer viewThreshold1 = 100;
    private BigDecimal viewRate1 = new BigDecimal("1.1");
    private Integer viewThreshold2 = 500;
    private BigDecimal viewRate2 = new BigDecimal("1.2");
    private Integer viewThreshold3 = 1000;
    private BigDecimal viewRate3 = new BigDecimal("1.3");
    private Integer viewThreshold4 = 5000;
    private BigDecimal viewRate4 = new BigDecimal("1.5");
    
    // ==================== 收藏量阈值 ====================
    private Integer favoriteThreshold1 = 5;
    private BigDecimal favoriteRate1 = new BigDecimal("1.1");
    private Integer favoriteThreshold2 = 20;
    private BigDecimal favoriteRate2 = new BigDecimal("1.2");
    private Integer favoriteThreshold3 = 50;
    private BigDecimal favoriteRate3 = new BigDecimal("1.3");
    private Integer favoriteThreshold4 = 100;
    private BigDecimal favoriteRate4 = new BigDecimal("1.5");
    
    // ==================== 销售加成 ====================
    /** 单次销售加成 */
    private BigDecimal saleRate = new BigDecimal("0.05");
    
    /** 最多计算销售次数 */
    private Integer maxSaleCount = 10;
    
    // ==================== 涨幅限制 ====================
    /** 最大涨幅倍数 */
    private BigDecimal maxGrowthMultiple = new BigDecimal("5.0");
}
