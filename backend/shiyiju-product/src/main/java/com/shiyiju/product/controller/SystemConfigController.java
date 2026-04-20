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
        priceGrowth.put("viewThreshold1", priceGrowthConfig.getViewThreshold1());
        priceGrowth.put("viewRate1", priceGrowthConfig.getViewRate1());
        priceGrowth.put("viewThreshold2", priceGrowthConfig.getViewThreshold2());
        priceGrowth.put("viewRate2", priceGrowthConfig.getViewRate2());
        priceGrowth.put("viewThreshold3", priceGrowthConfig.getViewThreshold3());
        priceGrowth.put("viewRate3", priceGrowthConfig.getViewRate3());
        priceGrowth.put("viewThreshold4", priceGrowthConfig.getViewThreshold4());
        priceGrowth.put("viewRate4", priceGrowthConfig.getViewRate4());
        priceGrowth.put("favoriteThreshold1", priceGrowthConfig.getFavoriteThreshold1());
        priceGrowth.put("favoriteRate1", priceGrowthConfig.getFavoriteRate1());
        priceGrowth.put("favoriteThreshold2", priceGrowthConfig.getFavoriteThreshold2());
        priceGrowth.put("favoriteRate2", priceGrowthConfig.getFavoriteRate2());
        priceGrowth.put("favoriteThreshold3", priceGrowthConfig.getFavoriteThreshold3());
        priceGrowth.put("favoriteRate3", priceGrowthConfig.getFavoriteRate3());
        priceGrowth.put("favoriteThreshold4", priceGrowthConfig.getFavoriteThreshold4());
        priceGrowth.put("favoriteRate4", priceGrowthConfig.getFavoriteRate4());
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
            // 价格增长配置需要写入 Nacos 配置中心，Spring Cloud 会自动刷新 @RefreshScope Bean
            
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
        result.put("viewThreshold1", priceGrowthConfig.getViewThreshold1());
        result.put("viewRate1", priceGrowthConfig.getViewRate1());
        result.put("viewThreshold2", priceGrowthConfig.getViewThreshold2());
        result.put("viewRate2", priceGrowthConfig.getViewRate2());
        result.put("viewThreshold3", priceGrowthConfig.getViewThreshold3());
        result.put("viewRate3", priceGrowthConfig.getViewRate3());
        result.put("viewThreshold4", priceGrowthConfig.getViewThreshold4());
        result.put("viewRate4", priceGrowthConfig.getViewRate4());
        result.put("favoriteThreshold1", priceGrowthConfig.getFavoriteThreshold1());
        result.put("favoriteRate1", priceGrowthConfig.getFavoriteRate1());
        result.put("favoriteThreshold2", priceGrowthConfig.getFavoriteThreshold2());
        result.put("favoriteRate2", priceGrowthConfig.getFavoriteRate2());
        result.put("favoriteThreshold3", priceGrowthConfig.getFavoriteThreshold3());
        result.put("favoriteRate3", priceGrowthConfig.getFavoriteRate3());
        result.put("favoriteThreshold4", priceGrowthConfig.getFavoriteThreshold4());
        result.put("favoriteRate4", priceGrowthConfig.getFavoriteRate4());
        result.put("saleRate", priceGrowthConfig.getSaleRate());
        result.put("maxSaleCount", priceGrowthConfig.getMaxSaleCount());
        result.put("maxGrowthMultiple", priceGrowthConfig.getMaxGrowthMultiple());
        
        return Result.success(result);
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
