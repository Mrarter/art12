package com.shiyiju.product.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.product.service.PriceGrowthConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置接口
 * 提供运营后台配置管理接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
@RefreshScope // 支持配置动态刷新
public class SystemConfigController {
    
    private final PriceGrowthConfig priceGrowthConfig;
    
    // 内存中的配置存储（实际应持久化到数据库）
    private static final Map<String, Object> CONFIG_CACHE = new HashMap<>();
    
    static {
        // 初始化默认配置
        initDefaultConfig();
    }
    
    private static void initDefaultConfig() {
        // 交易设置
        Map<String, Object> trade = new HashMap<>();
        trade.put("orderTimeout", 30);
        trade.put("refundDays", 7);
        trade.put("allowRepeatBuy", false);
        trade.put("priceUnit", "fen");
        CONFIG_CACHE.put("trade", trade);
        
        // 分销设置
        Map<String, Object> promotion = new HashMap<>();
        promotion.put("directCommission", 5);
        promotion.put("teamCommission", 2);
        promotion.put("settlementType", "after_pay");
        promotion.put("minWithdraw", 100);
        promotion.put("withdrawFee", 0);
        promotion.put("withdrawDays", 3);
        promotion.put("promoterCondition", "free");
        promotion.put("purchaseThreshold", 1000);
        CONFIG_CACHE.put("promotion", promotion);
        
        // 拍卖设置
        Map<String, Object> auction = new HashMap<>();
        auction.put("auctionDeposit", 1000);
        auction.put("depositRefund", true);
        auction.put("bidIncrement", 100);
        auction.put("delayCycles", 3);
        auction.put("delayMinutes", 5);
        CONFIG_CACHE.put("auction", auction);
        
        // 审核设置
        Map<String, Object> audit = new HashMap<>();
        audit.put("artistAudit", true);
        audit.put("artworkAudit", true);
        audit.put("postAudit", false);
        audit.put("sensitiveFilter", true);
        audit.put("sensitiveWords", "");
        CONFIG_CACHE.put("audit", audit);
    }

    /**
     * 获取所有配置
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAllConfig() {
        Map<String, Object> result = new HashMap<>(CONFIG_CACHE);
        
        // 价格增长配置从 PriceGrowthConfig 读取
        Map<String, Object> priceGrowth = new HashMap<>();
        priceGrowth.put("enabled", priceGrowthConfig.getEnabled());
        priceGrowth.put("baseDailyRate", priceGrowthConfig.getBaseDailyRate());
        priceGrowth.put("matureDailyRate", priceGrowthConfig.getMatureDailyRate());
        priceGrowth.put("matureDays", priceGrowthConfig.getMatureDays());
        priceGrowth.put("defaultBadgeRate", priceGrowthConfig.getDefaultBadgeRate());
        priceGrowth.put("verifiedBadgeRate", priceGrowthConfig.getVerifiedBadgeRate());
        priceGrowth.put("popularBadgeRate", priceGrowthConfig.getPopularBadgeRate());
        priceGrowth.put("masterBadgeRate", priceGrowthConfig.getMasterBadgeRate());
        priceGrowth.put("viewThreshold", priceGrowthConfig.getViewThreshold());
        priceGrowth.put("viewRate", priceGrowthConfig.getViewRate());
        priceGrowth.put("favoriteThreshold", priceGrowthConfig.getFavoriteThreshold());
        priceGrowth.put("favoriteRate", priceGrowthConfig.getFavoriteRate());
        priceGrowth.put("saleRate", priceGrowthConfig.getSaleRate());
        priceGrowth.put("maxSaleCount", priceGrowthConfig.getMaxSaleCount());
        priceGrowth.put("maxGrowthMultiple", priceGrowthConfig.getMaxGrowthMultiple());
        result.put("priceGrowth", priceGrowth);
        
        return Result.success(result);
    }

    /**
     * 更新所有配置
     */
    @PostMapping("/update")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> config) {
        try {
            // 更新交易配置
            if (config.containsKey("trade")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> trade = (Map<String, Object>) config.get("trade");
                CONFIG_CACHE.put("trade", trade);
            }
            
            // 更新分销配置
            if (config.containsKey("promotion")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> promotion = (Map<String, Object>) config.get("promotion");
                CONFIG_CACHE.put("promotion", promotion);
            }
            
            // 更新拍卖配置
            if (config.containsKey("auction")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> auction = (Map<String, Object>) config.get("auction");
                CONFIG_CACHE.put("auction", auction);
            }
            
            // 更新审核配置
            if (config.containsKey("audit")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> audit = (Map<String, Object>) config.get("audit");
                CONFIG_CACHE.put("audit", audit);
            }
            
            // 价格增长配置通过 Nacos 刷新
            
            log.info("系统配置已更新");
            return Result.success(null);
        } catch (Exception e) {
            log.error("配置更新失败", e);
            return Result.fail("配置更新失败");
        }
    }

    /**
     * 获取价格增长配置
     */
    @GetMapping("/priceGrowth")
    public Result<Map<String, Object>> getPriceGrowthConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", priceGrowthConfig.getEnabled());
        result.put("baseDailyRate", priceGrowthConfig.getBaseDailyRate());
        result.put("matureDailyRate", priceGrowthConfig.getMatureDailyRate());
        result.put("matureDays", priceGrowthConfig.getMatureDays());
        result.put("defaultBadgeRate", priceGrowthConfig.getDefaultBadgeRate());
        result.put("verifiedBadgeRate", priceGrowthConfig.getVerifiedBadgeRate());
        result.put("popularBadgeRate", priceGrowthConfig.getPopularBadgeRate());
        result.put("masterBadgeRate", priceGrowthConfig.getMasterBadgeRate());
        result.put("viewThreshold", priceGrowthConfig.getViewThreshold());
        result.put("viewRate", priceGrowthConfig.getViewRate());
        result.put("favoriteThreshold", priceGrowthConfig.getFavoriteThreshold());
        result.put("favoriteRate", priceGrowthConfig.getFavoriteRate());
        result.put("saleRate", priceGrowthConfig.getSaleRate());
        result.put("maxSaleCount", priceGrowthConfig.getMaxSaleCount());
        result.put("maxGrowthMultiple", priceGrowthConfig.getMaxGrowthMultiple());
        
        return Result.success(result);
    }

    /**
     * 更新价格增长配置
     */
    @PostMapping("/priceGrowth")
    public Result<Void> updatePriceGrowthConfig(@RequestBody Map<String, Object> config) {
        try {
            if (config.containsKey("enabled")) {
                priceGrowthConfig.setEnabled((Boolean) config.get("enabled"));
            }
            if (config.containsKey("baseDailyRate")) {
                priceGrowthConfig.setBaseDailyRate(new java.math.BigDecimal(String.valueOf(config.get("baseDailyRate"))));
            }
            if (config.containsKey("matureDailyRate")) {
                priceGrowthConfig.setMatureDailyRate(new java.math.BigDecimal(String.valueOf(config.get("matureDailyRate"))));
            }
            if (config.containsKey("matureDays")) {
                priceGrowthConfig.setMatureDays(((Number) config.get("matureDays")).intValue());
            }
            if (config.containsKey("defaultBadgeRate")) {
                priceGrowthConfig.setDefaultBadgeRate(new java.math.BigDecimal(String.valueOf(config.get("defaultBadgeRate"))));
            }
            if (config.containsKey("verifiedBadgeRate")) {
                priceGrowthConfig.setVerifiedBadgeRate(new java.math.BigDecimal(String.valueOf(config.get("verifiedBadgeRate"))));
            }
            if (config.containsKey("popularBadgeRate")) {
                priceGrowthConfig.setPopularBadgeRate(new java.math.BigDecimal(String.valueOf(config.get("popularBadgeRate"))));
            }
            if (config.containsKey("masterBadgeRate")) {
                priceGrowthConfig.setMasterBadgeRate(new java.math.BigDecimal(String.valueOf(config.get("masterBadgeRate"))));
            }
            if (config.containsKey("viewThreshold")) {
                priceGrowthConfig.setViewThreshold(((Number) config.get("viewThreshold")).intValue());
            }
            if (config.containsKey("viewRate")) {
                priceGrowthConfig.setViewRate(new java.math.BigDecimal(String.valueOf(config.get("viewRate"))));
            }
            if (config.containsKey("favoriteThreshold")) {
                priceGrowthConfig.setFavoriteThreshold(((Number) config.get("favoriteThreshold")).intValue());
            }
            if (config.containsKey("favoriteRate")) {
                priceGrowthConfig.setFavoriteRate(new java.math.BigDecimal(String.valueOf(config.get("favoriteRate"))));
            }
            if (config.containsKey("saleRate")) {
                priceGrowthConfig.setSaleRate(new java.math.BigDecimal(String.valueOf(config.get("saleRate"))));
            }
            if (config.containsKey("maxSaleCount")) {
                priceGrowthConfig.setMaxSaleCount(((Number) config.get("maxSaleCount")).intValue());
            }
            if (config.containsKey("maxGrowthMultiple")) {
                priceGrowthConfig.setMaxGrowthMultiple(new java.math.BigDecimal(String.valueOf(config.get("maxGrowthMultiple"))));
            }
            return Result.success();
        } catch (Exception e) {
            log.error("更新价格增长配置失败", e);
            return Result.fail("更新配置失败");
        }
    }

    /**
     * 获取分销配置
     */
    @GetMapping("/promotion")
    public Result<Map<String, Object>> getPromotionConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> promotion = (Map<String, Object>) CONFIG_CACHE.getOrDefault("promotion", new HashMap<>());
        return Result.success(promotion);
    }

    /**
     * 获取交易配置
     */
    @GetMapping("/trade")
    public Result<Map<String, Object>> getTradeConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> trade = (Map<String, Object>) CONFIG_CACHE.getOrDefault("trade", new HashMap<>());
        return Result.success(trade);
    }
}
