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
    
    // ==================== 热度系数 ====================
    /** 浏览量阈值 */
    private Integer viewThreshold = 100;
    /** 浏览量加成系数 */
    private BigDecimal viewRate = new BigDecimal("1.1");
    /** 收藏量阈值 */
    private Integer favoriteThreshold = 5;
    /** 收藏量加成系数 */
    private BigDecimal favoriteRate = new BigDecimal("1.1");
    
    // ==================== 销售加成 ====================
    /** 单次销售加成 */
    private BigDecimal saleRate = new BigDecimal("0.05");
    
    /** 最多计算销售次数 */
    private Integer maxSaleCount = 10;
    
    // ==================== 涨幅限制 ====================
    /** 最大涨幅倍数 */
    private BigDecimal maxGrowthMultiple = new BigDecimal("5.0");
}
