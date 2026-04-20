package com.shiyiju.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.mapper.ArtworkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 作品价格增长计算服务
 * 根据发布时间、艺术家知名度、浏览量、点赞、收藏量等因素动态计算价格增长
 * 
 * 所有配置从 PriceGrowthConfig (Nacos) 读取，支持运营后台动态配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceGrowthService {

    private final ArtworkMapper artworkMapper;
    private final PriceGrowthConfig config;

    /**
     * 计算价格增长率
     * 综合考虑：发布时间、艺术家知名度、浏览量、收藏量、销售次数
     */
    public BigDecimal calculatePriceRise(Artwork artwork) {
        // 检查开关
        if (config.getEnabled() == null || !config.getEnabled()) {
            return BigDecimal.ZERO;
        }
        
        if (artwork == null || artwork.getOriginalPrice() == null || artwork.getOriginalPrice() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalMultiplier = BigDecimal.ONE;

        // 1. 时间因素：发布越久，增长越多
        BigDecimal timeMultiplier = calculateTimeMultiplier(artwork.getCreateTime());
        totalMultiplier = totalMultiplier.multiply(timeMultiplier);

        // 2. 艺术家知名度因素
        BigDecimal badgeMultiplier = calculateBadgeMultiplier(artwork.getAuthorBadge());
        totalMultiplier = totalMultiplier.multiply(badgeMultiplier);

        // 3. 浏览量因素
        BigDecimal viewMultiplier = calculateViewMultiplier(artwork.getViewCount());
        totalMultiplier = totalMultiplier.multiply(viewMultiplier);

        // 4. 收藏量因素
        BigDecimal favoriteMultiplier = calculateFavoriteMultiplier(artwork.getFavoriteCount());
        totalMultiplier = totalMultiplier.multiply(favoriteMultiplier);

        // 5. 销售次数因素
        int sales = artwork.getSaleCount() != null ? Math.min(artwork.getSaleCount(), config.getMaxSaleCount()) : 0;
        for (int i = 0; i < sales; i++) {
            totalMultiplier = totalMultiplier.multiply(BigDecimal.ONE.add(config.getSaleRate()));
        }

        // 限制最大增长倍数
        BigDecimal maxMultiple = config.getMaxGrowthMultiple();
        if (totalMultiplier.compareTo(maxMultiple) > 0) {
            totalMultiplier = maxMultiple;
        }

        // 计算增长率 = (当前倍数 - 1) * 100%
        return totalMultiplier.subtract(BigDecimal.ONE)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算当前实时价格
     */
    public Long calculateCurrentPrice(Artwork artwork) {
        if (artwork == null || artwork.getOriginalPrice() == null) {
            return artwork != null ? artwork.getPrice() : 0L;
        }

        BigDecimal originalPrice = BigDecimal.valueOf(artwork.getOriginalPrice());
        BigDecimal multiplier = BigDecimal.ONE.add(calculatePriceRise(artwork));
        BigDecimal currentPrice = originalPrice.multiply(multiplier);

        return currentPrice.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 计算时间因素倍数
     */
    private BigDecimal calculateTimeMultiplier(LocalDateTime createTime) {
        if (createTime == null) {
            return BigDecimal.ONE;
        }

        long days = ChronoUnit.DAYS.between(createTime, LocalDateTime.now());
        if (days < 0) days = 0;

        BigDecimal dailyRate;
        if (days > config.getMatureDays()) {
            dailyRate = config.getMatureDailyRate();
            days = days - config.getMatureDays();
        } else {
            dailyRate = config.getBaseDailyRate();
        }

        BigDecimal multiplier = BigDecimal.ONE.add(dailyRate.multiply(BigDecimal.valueOf(days)));
        return multiplier;
    }

    /**
     * 计算艺术家知名度倍数
     */
    private BigDecimal calculateBadgeMultiplier(String authorBadge) {
        if (authorBadge == null || authorBadge.isEmpty()) {
            return config.getDefaultBadgeRate();
        }

        String badge = authorBadge.toLowerCase();
        if (badge.contains("大师") || badge.contains("master")) {
            return config.getMasterBadgeRate();
        } else if (badge.contains("人气")) {
            return config.getPopularBadgeRate();
        } else if (badge.contains("认证") || badge.contains("verified")) {
            return config.getVerifiedBadgeRate();
        }
        return config.getDefaultBadgeRate();
    }

    /**
     * 计算浏览量倍数
     */
    private BigDecimal calculateViewMultiplier(Integer viewCount) {
        if (viewCount == null || viewCount <= 0) {
            return BigDecimal.ONE;
        }

        if (viewCount >= config.getViewThreshold4()) {
            return config.getViewRate4();
        } else if (viewCount >= config.getViewThreshold3()) {
            return config.getViewRate3();
        } else if (viewCount >= config.getViewThreshold2()) {
            return config.getViewRate2();
        } else if (viewCount >= config.getViewThreshold1()) {
            return config.getViewRate1();
        }
        return BigDecimal.ONE;
    }

    /**
     * 计算收藏量倍数
     */
    private BigDecimal calculateFavoriteMultiplier(Integer favoriteCount) {
        if (favoriteCount == null || favoriteCount <= 0) {
            return BigDecimal.ONE;
        }

        if (favoriteCount >= config.getFavoriteThreshold4()) {
            return config.getFavoriteRate4();
        } else if (favoriteCount >= config.getFavoriteThreshold3()) {
            return config.getFavoriteRate3();
        } else if (favoriteCount >= config.getFavoriteThreshold2()) {
            return config.getFavoriteRate2();
        } else if (favoriteCount >= config.getFavoriteThreshold1()) {
            return config.getFavoriteRate1();
        }
        return BigDecimal.ONE;
    }

    /**
     * 定时任务：更新所有作品的价格增长率
     * 默认每小时执行一次，可通过 @Scheduled 注解调整为每日
     */
    // @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    @Scheduled(fixedRate = 3600000) // 每小时执行一次，方便测试
    public void updateAllPriceRise() {
        if (config.getEnabled() == null || !config.getEnabled()) {
            log.debug("价格增长功能已关闭，跳过定时任务");
            return;
        }
        
        log.info("开始执行价格增长率更新任务...");
        
        int pageSize = 100;
        int pageNum = 1;
        int totalUpdated = 0;

        while (true) {
            Page<Artwork> page = new Page<>(pageNum, pageSize);
            List<Artwork> artworks = artworkMapper.selectPage(page, 
                    new LambdaQueryWrapper<Artwork>()
                            .eq(Artwork::getStatus, 1)
                            .isNotNull(Artwork::getOriginalPrice)
            ).getRecords();

            if (artworks.isEmpty()) {
                break;
            }

            for (Artwork artwork : artworks) {
                try {
                    BigDecimal priceRise = calculatePriceRise(artwork);
                    Long currentPrice = calculateCurrentPrice(artwork);
                    
                    artwork.setPriceRise(priceRise);
                    artwork.setPrice(currentPrice);
                    artwork.setUpdateTime(LocalDateTime.now());
                    artworkMapper.updateById(artwork);
                    totalUpdated++;
                } catch (Exception e) {
                    log.error("更新作品价格失败, artworkId={}", artwork.getId(), e);
                }
            }

            if (artworks.size() < pageSize) {
                break;
            }
            pageNum++;
        }

        log.info("价格增长率更新任务完成，共更新 {} 个作品", totalUpdated);
    }

    /**
     * 更新单个作品价格（浏览/收藏变化时调用）
     */
    public void updateSinglePrice(Long artworkId) {
        if (config.getEnabled() == null || !config.getEnabled()) {
            return;
        }
        
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null || artwork.getOriginalPrice() == null) {
            return;
        }

        BigDecimal priceRise = calculatePriceRise(artwork);
        Long currentPrice = calculateCurrentPrice(artwork);
        
        artwork.setPriceRise(priceRise);
        artwork.setPrice(currentPrice);
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);
    }
    
    /**
     * 获取当前配置（供运营后台使用）
     */
    public PriceGrowthConfig getConfig() {
        return config;
    }
}
