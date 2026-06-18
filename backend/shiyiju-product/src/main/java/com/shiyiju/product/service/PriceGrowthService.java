package com.shiyiju.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.ProductConstant;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.entity.ArtworkPriceLog;
import com.shiyiju.product.mapper.ArtworkMapper;
import com.shiyiju.product.mapper.ArtworkPriceLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final JdbcTemplate jdbcTemplate;
    private final ArtworkPriceLogMapper priceLogMapper;
    private volatile long lastConfigSyncAt = 0L;

    /**
     * 计算价格增长率
     * 综合考虑：发布时间、艺术家知名度、浏览量、收藏量、销售次数
     * 支持单个作品的自定义配置
     */
    public BigDecimal calculatePriceRise(Artwork artwork) {
        syncPersistedPriceGrowthConfig();

        if (isPriceLocked(artwork)) {
            return artwork.getPriceRise() != null ? artwork.getPriceRise() : BigDecimal.ZERO;
        }

        // 检查开关
        if (config.getEnabled() == null || !config.getEnabled()) {
            return BigDecimal.ZERO;
        }
        
        if (artwork == null || resolveBasePrice(artwork) <= 0) {
            return BigDecimal.ZERO;
        }

        // 检查是否启用单个作品的自定义配置
        boolean useCustomConfig = Boolean.TRUE.equals(artwork.getCustomPriceGrowthEnabled());

        BigDecimal totalMultiplier = BigDecimal.ONE;

        // 1. 时间因素：发布越久，增长越多
        BigDecimal timeMultiplier = useCustomConfig 
            ? calculateTimeMultiplierCustom(artwork) 
            : calculateTimeMultiplier(artwork.getCreateTime());
        totalMultiplier = totalMultiplier.multiply(timeMultiplier);

        // 2. 艺术家知名度因素
        BigDecimal badgeMultiplier = calculateBadgeMultiplier(artwork.getAuthorBadge());
        totalMultiplier = totalMultiplier.multiply(badgeMultiplier);

        // 3. 浏览量因素
        int displayViewCount = calculateDisplayViewCount(artwork);
        BigDecimal viewMultiplier = useCustomConfig
            ? calculateViewMultiplierCustom(artwork)
            : calculateViewMultiplier(displayViewCount);
        totalMultiplier = totalMultiplier.multiply(viewMultiplier);

        // 4. 收藏量因素
        int displayLikeCount = calculateDisplayLikeCount(artwork);
        BigDecimal favoriteMultiplier = useCustomConfig
            ? calculateFavoriteMultiplierCustom(artwork)
            : calculateFavoriteMultiplier(displayLikeCount);
        totalMultiplier = totalMultiplier.multiply(favoriteMultiplier);

        // 5. 销售次数因素
        int sales = artwork.getSaleCount() != null ? Math.min(artwork.getSaleCount(), config.getMaxSaleCount()) : 0;
        for (int i = 0; i < sales; i++) {
            totalMultiplier = totalMultiplier.multiply(BigDecimal.ONE.add(config.getSaleRate()));
        }

        // 限制最大增长倍数（支持自定义）
        BigDecimal maxMultiple = useCustomConfig && artwork.getCustomMaxGrowthMultiple() != null
            ? artwork.getCustomMaxGrowthMultiple() 
            : config.getMaxGrowthMultiple();
        if (totalMultiplier.compareTo(maxMultiple) > 0) {
            totalMultiplier = maxMultiple;
        }

        // 计算增长率 = (当前倍数 - 1) * 100%
        return totalMultiplier.subtract(BigDecimal.ONE)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算时间因素倍数（单个作品自定义配置）
     */
    private BigDecimal calculateTimeMultiplierCustom(Artwork artwork) {
        if (artwork.getCreateTime() == null) {
            return BigDecimal.ONE;
        }

        long days = ChronoUnit.DAYS.between(artwork.getCreateTime(), LocalDateTime.now());
        if (days < 0) days = 0;

        int matureDays = artwork.getCustomMatureDays() != null ? artwork.getCustomMatureDays() : config.getMatureDays();

        BigDecimal baseDailyRate = artwork.getCustomBaseDailyRate() != null
                ? artwork.getCustomBaseDailyRate()
                : config.getBaseDailyRate();
        BigDecimal matureDailyRate = artwork.getCustomMatureDailyRate() != null
                ? artwork.getCustomMatureDailyRate()
                : config.getMatureDailyRate();

        return calculateSegmentedTimeMultiplier(days, matureDays, baseDailyRate, matureDailyRate);
    }

    /**
     * 计算浏览量倍数（单个作品自定义配置）
     */
    private BigDecimal calculateViewMultiplierCustom(Artwork artwork) {
        int viewCount = calculateDisplayViewCount(artwork);
        if (viewCount <= 0) {
            return BigDecimal.ONE;
        }
        
        // 使用全局的浏览量阈值
        if (viewCount >= config.getViewThreshold()) {
            return artwork.getCustomViewRate() != null ? artwork.getCustomViewRate() : config.getViewRate();
        }
        return BigDecimal.ONE;
    }

    /**
     * 计算收藏量倍数（单个作品自定义配置）
     */
    private BigDecimal calculateFavoriteMultiplierCustom(Artwork artwork) {
        int favoriteCount = calculateDisplayLikeCount(artwork);
        if (favoriteCount <= 0) {
            return BigDecimal.ONE;
        }
        
        // 使用全局的收藏量阈值
        if (favoriteCount >= config.getFavoriteThreshold()) {
            return artwork.getCustomFavoriteRate() != null ? artwork.getCustomFavoriteRate() : config.getFavoriteRate();
        }
        return BigDecimal.ONE;
    }

    /**
     * 计算当前实时价格
     */
    public Long calculateCurrentPrice(Artwork artwork) {
        if (artwork == null) {
            return 0L;
        }

        if (isPriceLocked(artwork)) {
            return artwork.getPrice() != null ? artwork.getPrice().setScale(0, RoundingMode.HALF_UP).longValue() : 0L;
        }

        long basePrice = resolveBasePrice(artwork);
        if (basePrice <= 0) {
            BigDecimal p = artwork.getPrice();
            return p != null ? p.longValue() : 0L;
        }

        BigDecimal originalPrice = BigDecimal.valueOf(basePrice);
        BigDecimal multiplier = BigDecimal.ONE.add(calculatePriceRise(artwork));
        BigDecimal currentPrice = originalPrice.multiply(multiplier);

        return currentPrice.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private boolean isPriceLocked(Artwork artwork) {
        return artwork != null
                && (artwork.getHolderId() != null || ProductConstant.STATUS_SOLD_OUT.equals(artwork.getStatus()));
    }

    private long resolveBasePrice(Artwork artwork) {
        if (artwork == null) {
            return 0L;
        }
        if (artwork.getOriginalPrice() != null && artwork.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            return artwork.getOriginalPrice().longValue();
        }
        return artwork.getPrice() != null ? artwork.getPrice().longValue() : 0L;
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

        return calculateSegmentedTimeMultiplier(days, config.getMatureDays(), config.getBaseDailyRate(), config.getMatureDailyRate());
    }

    private BigDecimal calculateSegmentedTimeMultiplier(long days, Integer matureDays, BigDecimal baseDailyRate, BigDecimal matureDailyRate) {
        int thresholdDays = matureDays != null ? Math.max(matureDays, 0) : 0;
        long baseDays = thresholdDays > 0 ? Math.min(days, thresholdDays) : 0;
        long matureDaysCount = thresholdDays > 0 ? Math.max(days - thresholdDays, 0) : days;

        BigDecimal baseGrowth = (baseDailyRate != null ? baseDailyRate : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(baseDays));
        BigDecimal matureGrowth = (matureDailyRate != null ? matureDailyRate : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(matureDaysCount));

        return BigDecimal.ONE.add(baseGrowth).add(matureGrowth);
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
        
        if (viewCount >= config.getViewThreshold()) {
            return config.getViewRate();
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
        
        if (favoriteCount >= config.getFavoriteThreshold()) {
            return config.getFavoriteRate();
        }
        return BigDecimal.ONE;
    }

    public int calculateDisplayViewCount(Artwork artwork) {
        if (artwork == null) return 0;
        int displayCount = calculateDisplayCount(artwork.getViewCount(), artwork.getDailyViewCount(), artwork.getCreateTime());
        if (!Boolean.TRUE.equals(config.getViewAutoGrowthEnabled())) {
            return displayCount;
        }
        int onlineDays = getInclusiveOnlineDays(artwork.getCreateTime());
        int dailyGrowth = randomizeGrowthBase(config.getDailyViewGrowth(), artwork, 11);
        int weeklyGrowth = randomizeGrowthBase(config.getWeeklyViewGrowth(), artwork, 17);
        int monthlyGrowth = randomizeGrowthBase(config.getMonthlyViewGrowth(), artwork, 23);
        return displayCount
            + dailyGrowth * onlineDays
            + weeklyGrowth * (onlineDays / 7)
            + monthlyGrowth * (onlineDays / 30);
    }

    private int randomizeGrowthBase(Integer baseValue, Artwork artwork, int salt) {
        int base = baseValue != null ? Math.max(baseValue, 0) : 0;
        if (base == 0) {
            return 0;
        }
        BigDecimal fluctuationRate = config.getViewGrowthRandomRate() != null
                ? config.getViewGrowthRandomRate().max(BigDecimal.ZERO).min(BigDecimal.ONE)
                : new BigDecimal("0.58");
        if (fluctuationRate.compareTo(BigDecimal.ZERO) <= 0) {
            return base;
        }

        long seed = artwork != null && artwork.getId() != null ? artwork.getId() : 0L;
        double randomUnit = stableRandomUnit(seed, salt);
        BigDecimal factor = BigDecimal.ONE
                .subtract(fluctuationRate)
                .add(fluctuationRate.multiply(BigDecimal.valueOf(randomUnit * 2)));
        return BigDecimal.valueOf(base)
                .multiply(factor)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private double stableRandomUnit(long seed, int salt) {
        long mixed = seed * 1103515245L + salt * 12345L + 0x9E3779B97F4A7C15L;
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= (mixed >>> 33);
        return (mixed >>> 11) * 0x1.0p-53;
    }

    public int calculateDisplayLikeCount(Artwork artwork) {
        if (artwork == null) return 0;
        return calculateDisplayCount(artwork.getFavoriteCount(), artwork.getDailyLikeCount(), artwork.getCreateTime());
    }

    private int calculateDisplayCount(Integer realCount, Integer dailyCount, LocalDateTime createTime) {
        int real = realCount != null ? realCount : 0;
        int daily = dailyCount != null ? dailyCount : 0;
        if (daily <= 0) return real;
        return real + daily * getInclusiveOnlineDays(createTime);
    }

    private int getInclusiveOnlineDays(LocalDateTime createTime) {
        if (createTime == null) return 1;
        LocalDate start = createTime.toLocalDate();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(start, today) + 1;
        return (int) Math.max(days, 1);
    }

    public Long calculateTomorrowIncreaseMin(Artwork artwork) {
        return calculateTomorrowIncrease(artwork, true);
    }

    public Long calculateTomorrowIncreaseMax(Artwork artwork) {
        return calculateTomorrowIncrease(artwork, false);
    }

    private Long calculateTomorrowIncrease(Artwork artwork, boolean min) {
        if (artwork == null || artwork.getPrice() == null || artwork.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }
        BigDecimal baseRate = resolveBaseDailyRate(artwork);
        BigDecimal matureRate = resolveMatureDailyRate(artwork);
        BigDecimal rate = min ? baseRate.min(matureRate) : baseRate.max(matureRate);
        return artwork.getPrice()
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private BigDecimal resolveBaseDailyRate(Artwork artwork) {
        if (Boolean.TRUE.equals(artwork.getCustomPriceGrowthEnabled()) && artwork.getCustomBaseDailyRate() != null) {
            return artwork.getCustomBaseDailyRate();
        }
        return config.getBaseDailyRate() != null ? config.getBaseDailyRate() : BigDecimal.ZERO;
    }

    private BigDecimal resolveMatureDailyRate(Artwork artwork) {
        if (Boolean.TRUE.equals(artwork.getCustomPriceGrowthEnabled()) && artwork.getCustomMatureDailyRate() != null) {
            return artwork.getCustomMatureDailyRate();
        }
        return config.getMatureDailyRate() != null ? config.getMatureDailyRate() : resolveBaseDailyRate(artwork);
    }

    /**
     * 定时任务：更新所有作品的价格增长率
     * 默认每小时执行一次，可通过 @Scheduled 注解调整为每日
     */
    // @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    @Scheduled(fixedRate = 3600000) // 每小时执行一次，方便测试
    public void updateAllPriceRise() {
        syncPersistedPriceGrowthConfig(true);

        if (config.getEnabled() == null || !config.getEnabled()) {
            log.debug("价格增长功能已关闭，跳过定时任务");
            return;
        }

        log.info("开始执行价格增长率更新任务...");

        // 批量查询已有涨价日志的作品ID，避免逐个查询
        Set<Long> loggedArtworkIds = new HashSet<>();
        try {
            List<Long> ids = jdbcTemplate.queryForList(
                    "SELECT DISTINCT artwork_id FROM artwork_price_log", Long.class);
            loggedArtworkIds.addAll(ids);
        } catch (Exception e) {
            log.warn("查询已有涨价日志列表失败", e);
        }

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
                    hydrateCustomPriceGrowthConfig(artwork);
                    BigDecimal oldPriceRise = artwork.getPriceRise();
                    BigDecimal priceRise = calculatePriceRise(artwork);
                    Long currentPrice = calculateCurrentPrice(artwork);

                    artwork.setPriceRise(priceRise);
                    // 注意：只更新 priceRise，不覆盖用户设置的 price
                    // price 是用户设置的销售价格，currentPrice 是计算后的当前价格
                    artwork.setUpdateTime(LocalDateTime.now());
                    artworkMapper.updateById(artwork);
                    totalUpdated++;

                    // 涨价幅度变化或首次记录时写入涨价日志
                    boolean hasLog = loggedArtworkIds.contains(artwork.getId());
                    writePriceRiseLog(artwork, oldPriceRise, priceRise, currentPrice, hasLog);
                    // 记录已写入日志
                    loggedArtworkIds.add(artwork.getId());
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
        syncPersistedPriceGrowthConfig(true);

        if (config.getEnabled() == null || !config.getEnabled()) {
            return;
        }
        
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null || artwork.getOriginalPrice() == null) {
            return;
        }
        if (isPriceLocked(artwork)) {
            log.debug("作品已被收藏，跳过单品涨价更新: artworkId={}", artworkId);
            return;
        }
        hydrateCustomPriceGrowthConfig(artwork);

        BigDecimal oldPriceRise = artwork.getPriceRise();
        BigDecimal priceRise = calculatePriceRise(artwork);
        Long currentPrice = calculateCurrentPrice(artwork);
        
        artwork.setPriceRise(priceRise);
        // 注意：不要覆盖用户设置的 price，只更新 priceRise
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);
        
        // 涨价幅度变化时写入涨价日志
        boolean hasExistingLog = priceLogMapper.selectCount(
                new LambdaQueryWrapper<ArtworkPriceLog>().eq(ArtworkPriceLog::getArtworkId, artworkId)) > 0;
        writePriceRiseLog(artwork, oldPriceRise, priceRise, currentPrice, hasExistingLog);
    }
    
    /**
     * 获取当前配置（供运营后台使用）
     */
    public PriceGrowthConfig getConfig() {
        syncPersistedPriceGrowthConfig(true);
        return config;
    }

    private void syncPersistedPriceGrowthConfig() {
        syncPersistedPriceGrowthConfig(false);
    }

    private void syncPersistedPriceGrowthConfig(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastConfigSyncAt < 5000) {
            return;
        }
        lastConfigSyncAt = now;
        try {
            List<Map<String, Object>> rows = queryPersistedConfigRows();
            if (rows.isEmpty()) {
                return;
            }
            Map<String, String> values = new HashMap<>();
            for (Map<String, Object> row : rows) {
                values.put(String.valueOf(row.get("config_key")), String.valueOf(row.get("config_value")));
            }
            applyPersistedConfig(values);
        } catch (Exception e) {
            log.debug("同步价格增长配置失败，继续使用当前内存配置: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> queryPersistedConfigRows() {
        String[] tableNames = {"config", "system_config", "sys_configs"};
        RuntimeException lastError = null;
        for (String tableName : tableNames) {
            try {
                return jdbcTemplate.queryForList(
                        "SELECT config_key, config_value FROM " + tableName + " WHERE config_key LIKE 'price.growth.%'");
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return List.of();
    }

    private void applyPersistedConfig(Map<String, String> values) {
        config.setEnabled(booleanValue(values, "price.growth.enabled", config.getEnabled()));
        config.setBaseDailyRate(decimalValue(values, "price.growth.base.daily.rate", config.getBaseDailyRate()));
        config.setMatureDailyRate(decimalValue(values, "price.growth.mature.daily.rate", config.getMatureDailyRate()));
        config.setMatureDays(intValue(values, "price.growth.mature.days", config.getMatureDays(), 0));
        config.setDefaultBadgeRate(decimalValue(values, "price.growth.badge.default.rate", config.getDefaultBadgeRate()));
        config.setVerifiedBadgeRate(decimalValue(values, "price.growth.badge.verified.rate", config.getVerifiedBadgeRate()));
        config.setPopularBadgeRate(decimalValue(values, "price.growth.badge.popular.rate", config.getPopularBadgeRate()));
        config.setMasterBadgeRate(decimalValue(values, "price.growth.badge.master.rate", config.getMasterBadgeRate()));
        config.setViewThreshold(intValue(values, "price.growth.view.threshold", config.getViewThreshold(), 0));
        config.setViewRate(decimalValue(values, "price.growth.view.rate", config.getViewRate()));
        config.setViewAutoGrowthEnabled(booleanValue(values, "price.growth.view.auto.enabled", config.getViewAutoGrowthEnabled()));
        config.setViewGrowthRandomRate(rateValue(values, "price.growth.view.random.rate", config.getViewGrowthRandomRate()));
        config.setDailyViewGrowth(intValue(values, "price.growth.view.daily.growth", config.getDailyViewGrowth(), 0));
        config.setWeeklyViewGrowth(intValue(values, "price.growth.view.weekly.growth", config.getWeeklyViewGrowth(), 0));
        config.setMonthlyViewGrowth(intValue(values, "price.growth.view.monthly.growth", config.getMonthlyViewGrowth(), 0));
        config.setFavoriteThreshold(intValue(values, "price.growth.favorite.threshold", config.getFavoriteThreshold(), 0));
        config.setFavoriteRate(decimalValue(values, "price.growth.favorite.rate", config.getFavoriteRate()));
        config.setSaleRate(decimalValue(values, "price.growth.sale.rate", config.getSaleRate()));
        config.setMaxSaleCount(intValue(values, "price.growth.max.sale.count", config.getMaxSaleCount(), 0));
        config.setMaxGrowthMultiple(decimalValue(values, "price.growth.max.multiple", config.getMaxGrowthMultiple()));
    }

    private Boolean booleanValue(Map<String, String> values, String key, Boolean fallback) {
        String raw = values.get(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw);
    }

    private Integer intValue(Map<String, String> values, String key, Integer fallback, int min) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Math.max(Integer.parseInt(raw), min);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private BigDecimal decimalValue(Map<String, String> values, String key, BigDecimal fallback) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private BigDecimal rateValue(Map<String, String> values, String key, BigDecimal fallback) {
        BigDecimal rate = decimalValue(values, key, fallback);
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return rate.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    /**
     * 写入涨价日志
     * <p>
     * 写入条件：
     * 1. 该作品没有日志（首次）→ 总是写入
     * 2. 该作品已有日志且涨价幅度变化 → 写入
     */
    private void writePriceRiseLog(Artwork artwork, BigDecimal oldPriceRise, BigDecimal newPriceRise, Long currentPrice, boolean hasExistingLog) {
        boolean changed = (oldPriceRise == null && newPriceRise != null && newPriceRise.compareTo(BigDecimal.ZERO) > 0)
                || (oldPriceRise != null && newPriceRise != null && oldPriceRise.compareTo(newPriceRise) != 0);
        // 首次记录或值有变化时才写入
        if (hasExistingLog && !changed) {
            return;
        }

        try {
            long basePrice = resolveBasePrice(artwork);
            ArtworkPriceLog log = new ArtworkPriceLog();
            log.setArtworkId(artwork.getId());
            log.setArtistId(artwork.getAuthorId());
            log.setOldPrice(basePrice);
            log.setNewPrice(currentPrice);
            log.setChangeRate(newPriceRise);
            log.setChangeReason("SYSTEM");
            log.setRemark("系统自动计算涨价");
            log.setOperatorId(0L);
            log.setCreatedAt(LocalDateTime.now());
            priceLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("写入涨价日志失败, artworkId={}", artwork.getId(), e);
        }
    }

    private void hydrateCustomPriceGrowthConfig(Artwork artwork) {
        if (artwork == null || !hasAllCustomPriceGrowthColumns()) {
            return;
        }
        try {
            jdbcTemplate.query("""
                    SELECT custom_price_growth_enabled,
                           custom_base_daily_rate,
                           custom_mature_daily_rate,
                           custom_mature_days,
                           custom_view_rate,
                           custom_favorite_rate,
                           custom_max_growth_multiple
                    FROM artwork
                    WHERE id = ?
                    """, rs -> {
                if (rs.next()) {
                    artwork.setCustomPriceGrowthEnabled(rs.getInt("custom_price_growth_enabled") == 1);
                    artwork.setCustomBaseDailyRate(rs.getBigDecimal("custom_base_daily_rate"));
                    artwork.setCustomMatureDailyRate(rs.getBigDecimal("custom_mature_daily_rate"));
                    int matureDays = rs.getInt("custom_mature_days");
                    artwork.setCustomMatureDays(rs.wasNull() || matureDays <= 0 ? null : matureDays);
                    artwork.setCustomViewRate(rs.getBigDecimal("custom_view_rate"));
                    artwork.setCustomFavoriteRate(rs.getBigDecimal("custom_favorite_rate"));
                    artwork.setCustomMaxGrowthMultiple(rs.getBigDecimal("custom_max_growth_multiple"));
                }
                return null;
            }, artwork.getId());
        } catch (Exception e) {
            log.warn("加载作品自定义涨价配置失败: artworkId={}, error={}", artwork.getId(), e.getMessage());
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean hasAllCustomPriceGrowthColumns() {
        return columnExists("artwork", "custom_price_growth_enabled")
                && columnExists("artwork", "custom_base_daily_rate")
                && columnExists("artwork", "custom_mature_daily_rate")
                && columnExists("artwork", "custom_mature_days")
                && columnExists("artwork", "custom_view_rate")
                && columnExists("artwork", "custom_favorite_rate")
                && columnExists("artwork", "custom_max_growth_multiple");
    }
}
