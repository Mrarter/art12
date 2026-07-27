package com.shiyiju.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.entity.Artwork;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.mapper.ArtworkMapper;
import com.shiyiju.user.entity.*;
import com.shiyiju.user.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 转售核心服务 - 艺术品二级流通生态
 *
 * 支持：转售发布、转售购买、艺术家持续收益、平台抽佣、价格成长、流通链路
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResaleService {

    private final ResaleRecordMapper resaleRecordMapper;
    private final ArtworkTradeRecordMapper artworkTradeRecordMapper;
    private final ArtworkPriceHistoryMapper artworkPriceHistoryMapper;
    private final ArtworkMapper artworkMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final WalletService walletService;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    /** 艺术家持续收益比例（默认5%） */
    @Value("${resale.artist-income-rate:0.05}")
    private BigDecimal artistIncomeRate;

    /** 平台服务费比例（默认15%） */
    @Value("${resale.platform-fee-rate:0.15}")
    private BigDecimal platformFeeRate;

    /** 平台收款钱包用户ID（平台服务费入到此账户） */
    @Value("${resale.platform-wallet-user-id:0}")
    private Long platformWalletUserId;

    // ===================== 核心业务 =====================

    /**
     * 发布转售
     * 校验：持有者校验、防重复、防未持有
     */
    @Transactional(rollbackFor = Exception.class)
    public ResaleRecord publishResale(Long sellerUserId, Long artworkId, BigDecimal resalePrice) {
        // 1. 校验参数
        if (sellerUserId == null || artworkId == null || resalePrice == null) {
            throw new BusinessException(400, "参数不完整");
        }
        if (resalePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "转售价格必须大于0");
        }

        // 2. 校验作品存在
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException(404, "作品不存在");
        }

        // 3. 校验持有者：必须当前持有者才能转售
        if (artwork.getHolderId() == null || !artwork.getHolderId().equals(sellerUserId)) {
            throw new BusinessException(403, "您不是当前作品持有者，无权转售");
        }

        // 4. 校验状态：必须是已售出状态
        if (artwork.getStatus() == null || artwork.getStatus() != 2) {
            throw new BusinessException(400, "该作品当前不可转售（仅已收藏作品可转售）");
        }

        // 5. 防重复：同一件作品不能被同一卖家重复发布待审核转售
        long pendingCount = resaleRecordMapper.selectCount(
                new LambdaQueryWrapper<ResaleRecord>()
                        .eq(ResaleRecord::getArtworkId, artworkId)
                        .eq(ResaleRecord::getSellerUserId, sellerUserId)
                        .eq(ResaleRecord::getStatus, "pending"));
        if (pendingCount > 0) {
            throw new BusinessException(400, "您已发布该作品的转售，请勿重复发布");
        }

        // 6. 创建转售记录
        ResaleRecord record = new ResaleRecord();
        record.setArtworkId(artworkId);
        record.setSellerUserId(sellerUserId);
        record.setSourceOrderId(resolveHolderSourceOrderId(artworkId, sellerUserId));
        record.setResalePrice(resalePrice);
        applyCurrentSettlement(record);
        record.setStatus("pending");
        record.setVersion(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        resaleRecordMapper.insert(record);

        log.info("发布转售: id={}, artworkId={}, sellerId={}, price={}, artistIncome={}, platformFee={}, sellerIncome={}",
                record.getId(), artworkId, sellerUserId, resalePrice, record.getArtistIncome(),
                record.getPlatformFee(), record.getSellerIncome());

        enrichResaleRecord(record);
        return record;
    }

    /**
     * 购买转售（标记已支付）
     * 调用此方法表示买家已完成支付，转售进入 paid 状态
     * 生成 trade_no 用于后续幂等控制
     */
    @Transactional(rollbackFor = Exception.class)
    public ResaleRecord markAsPaid(Long resaleId, Long buyerUserId) {
        String lockKey = "resale:lock:" + resaleId;
        Boolean locked = acquireLock(lockKey, 10);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(429, "该转售正在处理中，请稍后重试");
        }
        try {
            // 1. 校验转售记录
            ResaleRecord record = resaleRecordMapper.selectById(resaleId);
            if (record == null) {
                throw new BusinessException(404, "转售记录不存在");
            }
            if (!"pending".equals(record.getStatus())) {
                throw new BusinessException(400, "该转售当前不可购买，状态=" + record.getStatus());
            }
            if (record.getSellerUserId().equals(buyerUserId)) {
                throw new BusinessException(400, "不能购买自己的转售作品");
            }
            applyCurrentSettlement(record);

            // 2. 生成交易编号（用于幂等）
            String tradeNo = generateTradeNo("RES");

            // 3. 状态变更：pending -> paid（带乐观锁）
            int rows = resaleRecordMapper.updateStatus(resaleId, "pending", "paid", buyerUserId, tradeNo,
                    record.getArtistIncome(), record.getPlatformFee(), record.getSellerIncome(), record.getVersion());
            if (rows == 0) {
                throw new BusinessException(500, "购买转售失败，请重试");
            }

            record.setStatus("paid");
            record.setBuyerUserId(buyerUserId);
            record.setTradeNo(tradeNo);
            log.info("转售已支付: id={}, artworkId={}, buyerId={}, tradeNo={}", resaleId, record.getArtworkId(), buyerUserId, tradeNo);
            return record;
        } finally {
            releaseLock(lockKey);
        }
    }

    /**
     * 获取 Redis 分布式锁
     */
    private Boolean acquireLock(String key, int timeoutSeconds) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, "1",
                    java.time.Duration.ofSeconds(timeoutSeconds));
        } catch (Exception e) {
            log.warn("Redis 锁获取失败，降级为乐观锁: key={}", key);
            return true; // Redis不可用时降级
        }
    }

    /**
     * 释放 Redis 锁
     */
    private void releaseLock(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis 锁释放失败: key={}", key);
        }
    }

    /**
     * 完成转售（支付确认后调用）- 幂等安全
     *
     * 执行：
     * 1. 艺术家持续收益入账（5%）
     * 2. 平台服务费入账（15%）
     * 3. 卖家收入入账（80%）
     * 4. 更新作品持有者
     * 5. 记录交易链路 + 价格历史
     *
     * 幂等：通过 trade_no 唯一约束 + completed 状态校验防止重复执行
     */
    @Transactional(rollbackFor = Exception.class)
    public ResaleRecord completeResale(Long resaleId) {
        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在");
        }

        // === 幂等控制：已完成的直接返回 ===
        if ("completed".equals(record.getStatus())) {
            log.warn("转售已完成，幂等返回: id={}", resaleId);
            return record;
        }
        if (!"paid".equals(record.getStatus())) {
            throw new BusinessException(400, "转售状态不是已支付，无法完成");
        }

        Artwork artwork = artworkMapper.selectById(record.getArtworkId());
        if (artwork == null) {
            throw new BusinessException(404, "作品不存在");
        }

        int currentRound = getMaxTradeRound(record.getArtworkId());

        // 1. 艺术家持续收益入账
        if (record.getArtistIncome().compareTo(BigDecimal.ZERO) > 0) {
            Long artistId = artwork.getAuthorId();
            if (artistId != null) {
                walletService.income(artistId, record.getArtistIncome(), "resale",
                        resaleId, "resale",
                        "作品转售艺术家持续收益: 转售ID=" + resaleId + ", 价格=" + record.getResalePrice());
                log.info("艺术家持续收益入账: artistId={}, amount={}, resaleId={}",
                        artistId, record.getArtistIncome(), resaleId);
            }
        }

        // 2. 平台服务费入账
        Long effectivePlatformWalletUserId = resolvePlatformWalletUserId();
        if (record.getPlatformFee().compareTo(BigDecimal.ZERO) > 0
                && effectivePlatformWalletUserId != null
                && effectivePlatformWalletUserId > 0) {
            walletService.income(effectivePlatformWalletUserId, record.getPlatformFee(), "resale",
                    resaleId, "resale",
                    "平台转售服务费: 转售ID=" + resaleId + ", 价格=" + record.getResalePrice());
            log.info("平台服务费入账: walletId={}, amount={}, resaleId={}",
                    effectivePlatformWalletUserId, record.getPlatformFee(), resaleId);
        }

        // 3. 卖家收入入账
        if (record.getSellerIncome().compareTo(BigDecimal.ZERO) > 0) {
            walletService.income(record.getSellerUserId(), record.getSellerIncome(), "resale",
                    resaleId, "resale",
                    "作品转售收入: 转售ID=" + resaleId + ", 价格=" + record.getResalePrice());
            log.info("卖家收入入账: sellerId={}, amount={}, resaleId={}",
                    record.getSellerUserId(), record.getSellerIncome(), resaleId);
        }

        // 4. 更新作品持有者
        artwork.setHolderId(record.getBuyerUserId());
        artwork.setHolderSince(LocalDateTime.now());
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);

        // 5. 记录交易链路
        String tradeNo = record.getTradeNo() != null ? record.getTradeNo() : generateTradeNo("RES");
        ArtworkTradeRecord tradeRecord = new ArtworkTradeRecord();
        tradeRecord.setArtworkId(record.getArtworkId());
        tradeRecord.setTradeNo(tradeNo);
        tradeRecord.setSellerUserId(record.getSellerUserId());
        tradeRecord.setBuyerUserId(record.getBuyerUserId());
        tradeRecord.setTradePrice(record.getResalePrice());
        tradeRecord.setTradeType("resale");
        tradeRecord.setTradeRound(currentRound + 1);
        tradeRecord.setCreatedTime(LocalDateTime.now());
        artworkTradeRecordMapper.insert(tradeRecord);

        // 6. 记录价格历史
        BigDecimal beforePrice = artwork.getPrice() != null ? artwork.getPrice() : BigDecimal.ZERO;
        recordPriceHistory(record.getArtworkId(), beforePrice, record.getResalePrice(), "resale", resaleId);

        // 7. 更新作品当前价格
        artwork.setPrice(record.getResalePrice());
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);

        // 8. 标记转售完成（带乐观锁 + 状态校验）
        int rows = resaleRecordMapper.completeResale(resaleId, record.getBuyerUserId(),
                record.getArtistIncome(), record.getPlatformFee(), record.getSellerIncome(), record.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "完成转售失败（并发冲突或状态异常）");
        }

        record.setStatus("completed");
        log.info("转售完成: id={}, artworkId={}, tradeNo={}, round={}",
                resaleId, record.getArtworkId(), tradeNo, currentRound + 1);

        return record;
    }

    /**
     * 取消转售（仅待审核状态可取消）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelResale(Long resaleId, Long userId) {
        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在");
        }
        if (!record.getSellerUserId().equals(userId)) {
            throw new BusinessException(403, "无权取消该转售");
        }
        if (!"pending".equals(record.getStatus())) {
            throw new BusinessException(400, "当前状态不可取消");
        }

        int rows = resaleRecordMapper.cancelResale(resaleId, record.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "取消转售失败");
        }
        log.info("取消转售: id={}, userId={}", resaleId, userId);
    }

    /**
     * 调整转售价（仅卖家本人可修改待售转售）
     */
    @Transactional(rollbackFor = Exception.class)
    public ResaleRecord updateResalePrice(Long resaleId, Long userId, BigDecimal resalePrice) {
        if (userId == null || resaleId == null || resalePrice == null) {
            throw new BusinessException(400, "参数不完整");
        }
        if (resalePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "转售价格必须大于0");
        }

        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在");
        }
        if (!Objects.equals(record.getSellerUserId(), userId)) {
            throw new BusinessException(403, "无权修改该转售");
        }
        if (!"pending".equals(record.getStatus())) {
            throw new BusinessException(400, "当前状态不可调价");
        }
        if (Boolean.TRUE.equals(isPlatformPricingEnabled(record.getRemark()))) {
            throw new BusinessException(400, "已启用平台评估与热度涨价机制，请先关闭后再手动调价");
        }

        record.setResalePrice(resalePrice);
        applyCurrentSettlement(record);
        int rows = resaleRecordMapper.updateResalePrice(
                resaleId,
                resalePrice,
                record.getArtistIncome(),
                record.getPlatformFee(),
                record.getSellerIncome(),
                record.getVersion()
        );
        if (rows == 0) {
            throw new BusinessException(500, "调价失败，请重试");
        }

        ResaleRecord latest = resaleRecordMapper.selectById(resaleId);
        enrichResaleRecord(latest);
        log.info("调整转售价: id={}, userId={}, resalePrice={}", resaleId, userId, resalePrice);
        return latest;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResaleRecord updatePlatformPricing(Long resaleId, Long userId, Boolean enabled) {
        if (userId == null || resaleId == null || enabled == null) {
            throw new BusinessException(400, "参数不完整");
        }
        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在");
        }
        if (!Objects.equals(record.getSellerUserId(), userId)) {
            throw new BusinessException(403, "无权修改该转售");
        }
        if (!"pending".equals(record.getStatus())) {
            throw new BusinessException(400, "当前状态不可切换价格机制");
        }

        record.setSourceOrderId(resolveHolderSourceOrderId(record.getArtworkId(), record.getSellerUserId()));
        record.setRemark(mergePlatformPricingRemark(record.getRemark(), enabled));
        if (Boolean.TRUE.equals(enabled)) {
            Artwork artwork = artworkMapper.selectById(record.getArtworkId());
            populateSuggestedPriceRange(record, artwork);
            BigDecimal managedPrice = calculatePlatformManagedResalePrice(record, artwork);
            if (managedPrice != null && managedPrice.compareTo(BigDecimal.ZERO) > 0) {
                record.setResalePrice(managedPrice);
                applyCurrentSettlement(record);
            }
        }
        record.setUpdatedTime(LocalDateTime.now());
        resaleRecordMapper.updateById(record);

        ResaleRecord latest = resaleRecordMapper.selectById(resaleId);
        enrichResaleRecord(latest);
        log.info("切换平台评估与热度涨价机制: id={}, userId={}, enabled={}", resaleId, userId, enabled);
        return latest;
    }

    // ===================== 查询方法 =====================

    /**
     * 转售市场 - 分页查询可购买的转售
     */
    public Page<ResaleRecord> listResales(Integer page, Integer pageSize, Long artworkId) {
        Page<ResaleRecord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<ResaleRecord> wrapper = new LambdaQueryWrapper<ResaleRecord>()
                .eq(ResaleRecord::getStatus, "pending")
                .orderByDesc(ResaleRecord::getCreatedTime);
        if (artworkId != null) {
            wrapper.eq(ResaleRecord::getArtworkId, artworkId);
        }
        Page<ResaleRecord> result = resaleRecordMapper.selectPage(p, wrapper);
        enrichResaleRecords(result.getRecords());
        return result;
    }

    /**
     * 我的转售（作为卖家）
     */
    public Page<ResaleRecord> listMyResales(Long userId, Integer page, Integer pageSize, String status) {
        Page<ResaleRecord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<ResaleRecord> wrapper = new LambdaQueryWrapper<ResaleRecord>()
                .eq(ResaleRecord::getSellerUserId, userId)
                .orderByDesc(ResaleRecord::getCreatedTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ResaleRecord::getStatus, status);
        }
        Page<ResaleRecord> result = resaleRecordMapper.selectPage(p, wrapper);
        enrichResaleRecords(result.getRecords());
        return result;
    }

    /**
     * 获取转售详情
     */
    public ResaleRecord getResaleDetail(Long resaleId) {
        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在");
        }
        enrichResaleRecord(record);
        return record;
    }

    private void enrichResaleRecords(List<ResaleRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        records.forEach(this::enrichResaleRecord);
    }

    private void enrichResaleRecord(ResaleRecord record) {
        if (record == null) {
            return;
        }
        if ("pending".equals(record.getStatus())) {
            applyCurrentSettlement(record);
        }
        record.setArtistIncomeRate(resolveRate("platform.commission.resale.artist.income.rate", artistIncomeRate)
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP));
        record.setPlatformFeeRate(resolveRate("platform.commission.resale.platform.fee.rate", platformFeeRate)
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP));
        record.setPlatformPricingEnabled(isPlatformPricingEnabled(record.getRemark()));
        enrichArtworkInfo(record);
        record.setSellerUid(resolveUserUid(record.getSellerUserId()));
        record.setBuyerUid(resolveUserUid(record.getBuyerUserId()));
    }

    private void applyCurrentSettlement(ResaleRecord record) {
        if (record == null || record.getResalePrice() == null) {
            return;
        }
        if (!isPlatformCommissionEnabled()) {
            record.setArtistIncome(BigDecimal.ZERO);
            record.setPlatformFee(BigDecimal.ZERO);
            record.setSellerIncome(record.getResalePrice().setScale(2, RoundingMode.HALF_UP));
            return;
        }
        BigDecimal artistIncome = record.getResalePrice()
                .multiply(resolveRate("platform.commission.resale.artist.income.rate", artistIncomeRate))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = record.getResalePrice()
                .multiply(resolveRate("platform.commission.resale.platform.fee.rate", platformFeeRate))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal minPlatformFee = resolveAmount("platform.commission.min.fee", BigDecimal.ZERO);
        if (platformFee.compareTo(BigDecimal.ZERO) > 0 && platformFee.compareTo(minPlatformFee) < 0) {
            platformFee = minPlatformFee;
        }
        // A minimum fee must never create money beyond the transaction or make seller income negative.
        BigDecimal maxPlatformFee = record.getResalePrice().subtract(artistIncome).max(BigDecimal.ZERO);
        platformFee = platformFee.min(maxPlatformFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerIncome = record.getResalePrice().subtract(artistIncome).subtract(platformFee)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        record.setArtistIncome(artistIncome);
        record.setPlatformFee(platformFee);
        record.setSellerIncome(sellerIncome);
    }

    private boolean isPlatformCommissionEnabled() {
        String raw = readConfigValue("platform.commission.enabled");
        return raw == null || Boolean.parseBoolean(raw);
    }

    private BigDecimal resolveRate(String key, BigDecimal fallbackRate) {
        BigDecimal percent = resolveAmount(key, null);
        if (percent != null) {
            return percent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return fallbackRate != null ? fallbackRate : BigDecimal.ZERO;
    }

    private BigDecimal resolveAmount(String key, BigDecimal fallback) {
        String raw = readConfigValue(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            log.warn("平台抽佣配置解析失败: key={}, value={}", key, raw);
            return fallback;
        }
    }

    private Long resolvePlatformWalletUserId() {
        String walletUid = readConfigValue("platform.commission.wallet.uid");
        if (walletUid != null && !walletUid.isBlank()) {
            String trimmed = walletUid.trim();
            for (String table : List.of("users", "user_account", "sys_user")) {
                for (String column : List.of("uid", "user_uid")) {
                    Long userId = queryPlatformWalletUserId(table, column, trimmed);
                    if (userId != null) {
                        return userId;
                    }
                }
            }
            log.warn("平台钱包UID未匹配用户，回退配置文件用户ID: uid={}", walletUid);
        }
        return platformWalletUserId;
    }

    private Long queryPlatformWalletUserId(String table, String column, String walletUid) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM " + table + " WHERE " + column + " = ? LIMIT 1",
                    Long.class,
                    walletUid
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readConfigValue(String key) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT config_value FROM system_config WHERE config_key = ? LIMIT 1",
                    String.class,
                    key
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean booleanConfig(String key, boolean fallback) {
        String raw = readConfigValue(key);
        return raw == null || raw.isBlank() ? fallback : Boolean.parseBoolean(raw.trim());
    }

    private int intConfig(String key, int fallback) {
        BigDecimal value = resolveAmount(key, null);
        return value == null ? fallback : value.intValue();
    }

    private BigDecimal decimalConfig(String key, BigDecimal fallback) {
        BigDecimal value = resolveAmount(key, null);
        return value != null ? value : fallback;
    }

    private void enrichArtworkInfo(ResaleRecord record) {
        Long artworkId = record.getArtworkId();
        if (artworkId == null) {
            return;
        }
        record.setArtworkUid(resolveArtworkUid(artworkId));
        try {
            Artwork artwork = artworkMapper.selectById(artworkId);
            if (artwork == null) {
                return;
            }
            record.setArtworkTitle(normalizeDisplayText(artwork.getTitle()));
            record.setArtworkCoverImage(artwork.getCoverImage());
            record.setArtworkArtType(normalizeDisplayText(artwork.getArtType()));
            record.setArtworkMedium(normalizeDisplayText(artwork.getMedium()));
            record.setArtworkSize(normalizeDisplayText(artwork.getSize()));
            record.setArtworkYear(artwork.getYear());
            record.setArtworkCurrentPrice(artwork.getPrice() != null ? artwork.getPrice() : record.getResalePrice());
            populateSuggestedPriceRange(record, artwork);
            record.setArtistName(resolveArtistName(artwork));
            record.setCategoryName(resolveCategoryName(artwork));
        } catch (Exception e) {
            log.debug("补充转售作品信息失败: artworkId={}", artworkId, e);
        }
    }

    private void populateSuggestedPriceRange(ResaleRecord record, Artwork artwork) {
        if (record == null || artwork == null || record.getArtworkId() == null || record.getSellerUserId() == null) {
            return;
        }
        BigDecimal holderBuyPrice = resolveHolderBuyPrice(record.getArtworkId(), record.getSellerUserId());
        if (holderBuyPrice == null || holderBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            holderBuyPrice = artwork.getPrice() != null && artwork.getPrice().compareTo(BigDecimal.ZERO) > 0
                    ? artwork.getPrice()
                    : record.getResalePrice();
        }
        if (holderBuyPrice == null || holderBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        record.setHolderBuyPrice(holderBuyPrice.setScale(2, RoundingMode.HALF_UP));
        record.setSuggestedMinPrice(holderBuyPrice.multiply(new BigDecimal("1.22")).setScale(2, RoundingMode.HALF_UP));
        record.setSuggestedMaxPrice(holderBuyPrice.multiply(new BigDecimal("1.45")).setScale(2, RoundingMode.HALF_UP));
        record.setPlatformManagedPrice(calculatePlatformManagedResalePrice(record, artwork));
    }

    private BigDecimal calculatePlatformManagedResalePrice(ResaleRecord record, Artwork artwork) {
        if (record == null) {
            return null;
        }
        BigDecimal basePrice = record.getHolderBuyPrice();
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return record.getSuggestedMinPrice();
        }
        if (!booleanConfig("price.growth.enabled", true)) {
            return record.getSuggestedMinPrice();
        }

        BigDecimal managedPrice = basePrice.multiply(calculateGlobalPriceGrowthMultiplier(artwork))
                .setScale(2, RoundingMode.HALF_UP);
        if (record.getSuggestedMinPrice() != null && managedPrice.compareTo(record.getSuggestedMinPrice()) < 0) {
            managedPrice = record.getSuggestedMinPrice();
        }
        if (record.getSuggestedMaxPrice() != null && managedPrice.compareTo(record.getSuggestedMaxPrice()) > 0) {
            managedPrice = record.getSuggestedMaxPrice();
        }
        return managedPrice;
    }

    private BigDecimal calculateGlobalPriceGrowthMultiplier(Artwork artwork) {
        BigDecimal totalMultiplier = BigDecimal.ONE;
        long onlineDays = getInclusiveOnlineDays(artwork != null ? artwork.getCreateTime() : null);
        int matureDays = intConfig("price.growth.mature.days", 30);
        BigDecimal baseDailyRate = decimalConfig("price.growth.base.daily.rate", new BigDecimal("0.0002"));
        BigDecimal matureDailyRate = decimalConfig("price.growth.mature.daily.rate", new BigDecimal("0.0003"));

        long earlyDays = Math.min(onlineDays, matureDays);
        long maturePeriodDays = Math.max(onlineDays - matureDays, 0);
        totalMultiplier = totalMultiplier.multiply(
                BigDecimal.ONE
                        .add(baseDailyRate.multiply(BigDecimal.valueOf(earlyDays)))
                        .add(matureDailyRate.multiply(BigDecimal.valueOf(maturePeriodDays)))
        );

        int viewCount = safeCount(artwork != null ? artwork.getViewCount() : null)
                + safeCount(artwork != null ? artwork.getDailyViewCount() : null) * (int) onlineDays;
        if (viewCount >= intConfig("price.growth.view.threshold", 100)) {
            totalMultiplier = totalMultiplier.multiply(decimalConfig("price.growth.view.rate", new BigDecimal("1.1")));
        }

        int favoriteCount = safeCount(artwork != null ? artwork.getFavoriteCount() : null)
                + safeCount(artwork != null ? artwork.getDailyLikeCount() : null) * (int) onlineDays;
        if (favoriteCount >= intConfig("price.growth.favorite.threshold", 5)) {
            totalMultiplier = totalMultiplier.multiply(decimalConfig("price.growth.favorite.rate", new BigDecimal("1.1")));
        }

        int sales = Math.min(safeCount(artwork != null ? artwork.getSaleCount() : null), intConfig("price.growth.max.sale.count", 10));
        BigDecimal saleRate = decimalConfig("price.growth.sale.rate", new BigDecimal("0.05"));
        for (int i = 0; i < sales; i++) {
            totalMultiplier = totalMultiplier.multiply(BigDecimal.ONE.add(saleRate));
        }

        BigDecimal maxMultiple = decimalConfig("price.growth.max.multiple", new BigDecimal("5.0"));
        if (totalMultiplier.compareTo(maxMultiple) > 0) {
            totalMultiplier = maxMultiple;
        }
        return totalMultiplier;
    }

    private long getInclusiveOnlineDays(LocalDateTime createTime) {
        if (createTime == null) {
            return 1;
        }
        LocalDate start = createTime.toLocalDate();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(start, today) + 1;
        return Math.max(days, 1);
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private BigDecimal resolveHolderBuyPrice(Long artworkId, Long holderUserId) {
        Long sourceOrderId = resolveHolderSourceOrderId(artworkId, holderUserId);
        if (sourceOrderId != null) {
            try {
                BigDecimal orderPrice = jdbcTemplate.queryForObject(
                        """
                        SELECT toi.price
                        FROM trade_order_item toi
                        JOIN trade_order o ON o.id = toi.order_id
                        WHERE toi.order_id = ?
                          AND toi.artwork_id = ?
                          AND o.buyer_user_id = ?
                        ORDER BY o.created_at DESC
                        LIMIT 1
                        """,
                        BigDecimal.class,
                        sourceOrderId,
                        artworkId,
                        holderUserId
                );
                if (orderPrice != null && orderPrice.compareTo(BigDecimal.ZERO) > 0) {
                    return orderPrice;
                }
            } catch (Exception e) {
                log.debug("按来源订单查询持有者买入价失败: artworkId={}, holderUserId={}, sourceOrderId={}",
                        artworkId, holderUserId, sourceOrderId, e);
            }
        }

        try {
            BigDecimal orderPrice = jdbcTemplate.queryForObject(
                    """
                    SELECT toi.price
                    FROM trade_order_item toi
                    JOIN trade_order o ON o.id = toi.order_id
                    WHERE toi.artwork_id = ?
                      AND o.buyer_user_id = ?
                    ORDER BY o.created_at DESC
                    LIMIT 1
                    """,
                    BigDecimal.class,
                    artworkId,
                    holderUserId
            );
            if (orderPrice != null && orderPrice.compareTo(BigDecimal.ZERO) > 0) {
                return orderPrice;
            }
        } catch (Exception e) {
            log.debug("按买入订单查询持有者买入价失败: artworkId={}, holderUserId={}", artworkId, holderUserId, e);
        }

        List<ArtworkTradeRecord> trades = artworkTradeRecordMapper.selectList(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                        .eq(ArtworkTradeRecord::getBuyerUserId, holderUserId)
                        .orderByDesc(ArtworkTradeRecord::getCreatedTime)
                        .last("LIMIT 1")
        );
        if (trades == null || trades.isEmpty()) {
            return null;
        }
        return trades.get(0).getTradePrice();
    }

    private Long resolveHolderSourceOrderId(Long artworkId, Long holderUserId) {
        if (artworkId == null || holderUserId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT toi.order_id
                    FROM trade_order_item toi
                    JOIN trade_order o ON o.id = toi.order_id
                    WHERE toi.artwork_id = ?
                      AND o.buyer_user_id = ?
                    ORDER BY o.created_at DESC
                    LIMIT 1
                    """,
                    Long.class,
                    artworkId,
                    holderUserId
            );
        } catch (Exception e) {
            log.debug("查询持有者来源订单失败: artworkId={}, holderUserId={}", artworkId, holderUserId, e);
            return null;
        }
    }

    private boolean isPlatformPricingEnabled(String remark) {
        return remark != null && remark.contains("platformPricingEnabled=1");
    }

    private String mergePlatformPricingRemark(String remark, boolean enabled) {
        String cleaned = remark == null ? "" : remark
                .replace("platformPricingEnabled=1", "")
                .replace(";;", ";")
                .trim();
        cleaned = cleaned.replaceAll("^[;\\s]+|[;\\s]+$", "");
        if (!enabled) {
            return cleaned;
        }
        return cleaned.isBlank() ? "platformPricingEnabled=1" : cleaned + ";platformPricingEnabled=1";
    }

    private String resolveArtistName(Artwork artwork) {
        if (artwork == null) {
            return null;
        }
        String artistColumn = firstExistingColumn("artwork", "artist_name", "author_name");
        if (artistColumn != null && artwork.getId() != null) {
            try {
                String name = jdbcTemplate.queryForObject(
                        "SELECT " + artistColumn + " FROM artwork WHERE id = ? LIMIT 1",
                        String.class,
                        artwork.getId());
                String normalized = normalizeDisplayText(name);
                if (normalized != null && !normalized.isBlank()) {
                    return normalized;
                }
            } catch (Exception e) {
                log.debug("查询作品艺术家名称失败: artworkId={}, column={}", artwork.getId(), artistColumn, e);
            }
        }
        if (artwork.getAuthorId() == null) {
            return null;
        }
        try {
            User author = userMapper.selectById(artwork.getAuthorId());
            String nickname = normalizeDisplayText(author != null ? author.getNickname() : null);
            if (nickname != null && !nickname.isBlank()) {
                return nickname;
            }
        } catch (Exception e) {
            log.debug("查询作品作者用户失败: authorId={}", artwork.getAuthorId(), e);
        }
        return null;
    }

    private String resolveCategoryName(Artwork artwork) {
        if (artwork == null) {
            return null;
        }
        String categoryColumn = firstExistingColumn("artwork", "category_name");
        if (categoryColumn != null && artwork.getId() != null) {
            try {
                String name = jdbcTemplate.queryForObject(
                        "SELECT " + categoryColumn + " FROM artwork WHERE id = ? LIMIT 1",
                        String.class,
                        artwork.getId());
                String normalized = normalizeDisplayText(name);
                if (normalized != null && !normalized.isBlank()) {
                    return normalized;
                }
            } catch (Exception e) {
                log.debug("查询作品门类名称失败: artworkId={}, column={}", artwork.getId(), categoryColumn, e);
            }
        }
        if (artwork.getCategoryId() == null) {
            return null;
        }
        String nameColumn = firstExistingColumn("artwork_category", "name", "category_name", "title");
        if (nameColumn == null) {
            return null;
        }
        try {
            String name = jdbcTemplate.queryForObject(
                    "SELECT " + nameColumn + " FROM artwork_category WHERE id = ? LIMIT 1",
                    String.class,
                    artwork.getCategoryId());
            return normalizeDisplayText(name);
        } catch (Exception e) {
            log.debug("查询作品门类失败: categoryId={}, column={}", artwork.getCategoryId(), nameColumn, e);
            return null;
        }
    }

    private String normalizeDisplayText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.contains("�")) {
            return null;
        }
        if (value.contains("Ã") || value.contains("Â") || value.contains("æ") || value.contains("ç") || value.contains("ï")) {
            try {
                String decoded = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                if (!decoded.isBlank() && !decoded.contains("�")) {
                    return decoded;
                }
            } catch (Exception ignored) {
                // Keep the original value if it is already correctly encoded.
            }
            return null;
        }
        return value;
    }

    private String resolveUserUid(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            User user = userMapper.selectById(userId);
            if (user != null && user.getUid() != null && !user.getUid().isBlank()) {
                return user.getUid();
            }
        } catch (Exception e) {
            log.debug("查询用户UID失败: userId={}", userId, e);
        }
        return null;
    }

    private String resolveArtworkUid(Long artworkId) {
        if (artworkId == null) {
            return null;
        }
        String uidColumn = firstExistingColumn("artwork", "uid", "artwork_uid", "code", "artwork_code");
        if (uidColumn == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT " + uidColumn + " FROM artwork WHERE id = ? LIMIT 1",
                    String.class,
                    artworkId);
        } catch (Exception e) {
            log.debug("查询作品UID失败: artworkId={}, column={}", artworkId, uidColumn, e);
            return null;
        }
    }

    private String firstExistingColumn(String tableName, String... columns) {
        for (String column : columns) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                        Integer.class,
                        tableName,
                        column);
                if (count != null && count > 0) {
                    return column;
                }
            } catch (Exception e) {
                log.debug("检查字段失败: table={}, column={}", tableName, column, e);
            }
        }
        return null;
    }

    /**
     * 获取作品完整交易链路
     */
    public List<ArtworkTradeRecord> getArtworkTradeHistory(Long artworkId) {
        return artworkTradeRecordMapper.selectList(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                        .orderByAsc(ArtworkTradeRecord::getTradeRound));
    }

    /**
     * 获取作品价格历史
     */
    public List<ArtworkPriceHistory> getArtworkPriceHistory(Long artworkId) {
        return artworkPriceHistoryMapper.selectList(
                new LambdaQueryWrapper<ArtworkPriceHistory>()
                        .eq(ArtworkPriceHistory::getArtworkId, artworkId)
                        .orderByDesc(ArtworkPriceHistory::getCreatedTime));
    }

    /**
     * 获取作品转售统计数据
     */
    public Map<String, Object> getArtworkResaleStats(Long artworkId) {
        Map<String, Object> stats = new HashMap<>();

        // 转售次数
        long resaleCount = artworkTradeRecordMapper.selectCount(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                        .eq(ArtworkTradeRecord::getTradeType, "resale"));
        stats.put("resaleCount", resaleCount);

        // 流通次数（首次出售 + 转售）
        long totalTrades = artworkTradeRecordMapper.selectCount(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId));
        stats.put("totalTrades", totalTrades);

        // 最高成交价
        List<ArtworkTradeRecord> trades = artworkTradeRecordMapper.selectList(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                        .orderByDesc(ArtworkTradeRecord::getTradePrice)
                        .last("LIMIT 1"));
        stats.put("highestPrice", trades.isEmpty() ? BigDecimal.ZERO : trades.get(0).getTradePrice());

        // 首次成交价
        List<ArtworkTradeRecord> firstTrade = artworkTradeRecordMapper.selectList(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                        .eq(ArtworkTradeRecord::getTradeType, "first_sale")
                        .orderByAsc(ArtworkTradeRecord::getTradeRound)
                        .last("LIMIT 1"));
        stats.put("firstPrice", firstTrade.isEmpty() ? BigDecimal.ZERO : firstTrade.get(0).getTradePrice());

        // 价格成长
        List<ArtworkPriceHistory> priceHistories = artworkPriceHistoryMapper.selectList(
                new LambdaQueryWrapper<ArtworkPriceHistory>()
                        .eq(ArtworkPriceHistory::getArtworkId, artworkId)
                        .orderByDesc(ArtworkPriceHistory::getCreatedTime)
                        .last("LIMIT 1"));

        if (!priceHistories.isEmpty()) {
            ArtworkPriceHistory last = priceHistories.get(0);
            stats.put("lastPrice", last.getAfterPrice());
            stats.put("totalGrowthRate", calculateTotalGrowthRate(artworkId, last.getAfterPrice()));
        } else {
            stats.put("lastPrice", BigDecimal.ZERO);
            stats.put("totalGrowthRate", BigDecimal.ZERO);
        }

        return stats;
    }

    /**
     * 后台管理 - 分页查询所有转售记录
     */
    public Page<ResaleRecord> adminListResales(Integer page, Integer pageSize, String status, Long artworkId) {
        Page<ResaleRecord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<ResaleRecord> wrapper = new LambdaQueryWrapper<ResaleRecord>()
                .orderByDesc(ResaleRecord::getCreatedTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ResaleRecord::getStatus, status);
        }
        if (artworkId != null) {
            wrapper.eq(ResaleRecord::getArtworkId, artworkId);
        }
        return resaleRecordMapper.selectPage(p, wrapper);
    }

    /**
     * 后台管理 - 强制取消转售（跳过卖家校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminCancelResale(Long resaleId) {
        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在");
        }
        if (!"pending".equals(record.getStatus())) {
            throw new BusinessException(400, "仅待审核状态的转售可取消");
        }
        int rows = resaleRecordMapper.cancelResale(resaleId, record.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "取消转售失败");
        }
        log.info("管理员强制取消转售: id={}", resaleId);
    }

    /**
     * 转售退款回滚 — 专用退款反向操作
     *
     * 与 markAsPaid() 语义完全相反。
     * 当转售订单退款时调用，执行：
     * 1. resale_record 状态: paid/completed → pending（可重新上架）
     * 2. 恢复 artwork.holderId 为原卖家
     * 3. 生成退款流水记录
     *
     * 资金回滚（扣回 artist/seller/platform）由调用方负责，
     * 此方法只处理转售记录层面的反向操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackResaleAfterRefund(Long resaleId) {
        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在: " + resaleId);
        }
        // 已完成或已支付的转售才能回滚
        if (!"paid".equals(record.getStatus()) && !"completed".equals(record.getStatus())) {
            throw new BusinessException(400, "转售状态不可回滚, status=" + record.getStatus());
        }

        // 1. 恢复转售状态为 pending（可重新上架）
        record.setStatus("pending");
        record.setBuyerUserId(null);
        record.setUpdatedTime(LocalDateTime.now());
        resaleRecordMapper.updateById(record);

        // 2. 恢复作品持有者为原卖家
        Artwork artwork = artworkMapper.selectById(record.getArtworkId());
        if (artwork != null && record.getSellerUserId() != null) {
            artwork.setHolderId(record.getSellerUserId());
            artwork.setHolderSince(LocalDateTime.now());
            artwork.setUpdateTime(LocalDateTime.now());
            artworkMapper.updateById(artwork);
        }

        log.info("转售退款回滚完成: resaleId={}, status={}→pending, holder={}",
                resaleId, record.getStatus(), record.getSellerUserId());
    }

    /**
     * 后台管理 - 平台抽佣统计
     */
    public Map<String, Object> adminPlatformFeeStats() {
        Map<String, Object> stats = new HashMap<>();

        // 所有已完成的转售
        List<ResaleRecord> completed = resaleRecordMapper.selectList(
                new LambdaQueryWrapper<ResaleRecord>()
                        .eq(ResaleRecord::getStatus, "completed"));

        BigDecimal totalPlatformFee = completed.stream()
                .map(ResaleRecord::getPlatformFee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalArtistIncome = completed.stream()
                .map(ResaleRecord::getArtistIncome)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellerIncome = completed.stream()
                .map(ResaleRecord::getSellerIncome)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTradeAmount = completed.stream()
                .map(ResaleRecord::getResalePrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalResaleCount", completed.size());
        stats.put("totalTradeAmount", totalTradeAmount);
        stats.put("totalPlatformFee", totalPlatformFee);
        stats.put("totalArtistIncome", totalArtistIncome);
        stats.put("totalSellerIncome", totalSellerIncome);

        return stats;
    }

    /**
     * 后台管理 - 流通数据统计
     */
    public Map<String, Object> adminCirculationStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalResales = resaleRecordMapper.selectCount(null);
        long completedResales = resaleRecordMapper.selectCount(
                new LambdaQueryWrapper<ResaleRecord>().eq(ResaleRecord::getStatus, "completed"));
        long pendingResales = resaleRecordMapper.selectCount(
                new LambdaQueryWrapper<ResaleRecord>().eq(ResaleRecord::getStatus, "pending"));

        long totalTradeRecords = artworkTradeRecordMapper.selectCount(null);
        long firstSales = artworkTradeRecordMapper.selectCount(
                new LambdaQueryWrapper<ArtworkTradeRecord>().eq(ArtworkTradeRecord::getTradeType, "first_sale"));
        long resaleTrades = artworkTradeRecordMapper.selectCount(
                new LambdaQueryWrapper<ArtworkTradeRecord>().eq(ArtworkTradeRecord::getTradeType, "resale"));

        long uniqueArtworks = artworkTradeRecordMapper.selectCount(
                new QueryWrapper<ArtworkTradeRecord>()
                        .select("DISTINCT artwork_id"));

        stats.put("totalResales", totalResales);
        stats.put("completedResales", completedResales);
        stats.put("pendingResales", pendingResales);
        stats.put("totalTradeRecords", totalTradeRecords);
        stats.put("firstSales", firstSales);
        stats.put("resaleTrades", resaleTrades);
        stats.put("uniqueArtworks", uniqueArtworks);

        return stats;
    }

    // ===================== 首次出售记录 =====================

    /**
     * 记录首次出售（订单完成时调用）
     * 由 OrderService 在首次订单完成时触发
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordFirstSale(Long artworkId, Long buyerUserId, BigDecimal tradePrice, String tradeNo) {
        if (buyerUserId == null || tradePrice == null) return;

        // 记录交易链路
        ArtworkTradeRecord tradeRecord = new ArtworkTradeRecord();
        tradeRecord.setArtworkId(artworkId);
        tradeRecord.setTradeNo(tradeNo != null ? tradeNo : generateTradeNo("FS"));
        tradeRecord.setBuyerUserId(buyerUserId);
        tradeRecord.setTradePrice(tradePrice);
        tradeRecord.setTradeType("first_sale");
        tradeRecord.setTradeRound(1);
        tradeRecord.setCreatedTime(LocalDateTime.now());
        artworkTradeRecordMapper.insert(tradeRecord);

        // 记录价格历史
        recordPriceHistory(artworkId, BigDecimal.ZERO, tradePrice, "first_sale", null);

        log.info("记录首次出售: artworkId={}, buyerId={}, price={}, tradeNo={}",
                artworkId, buyerUserId, tradePrice, tradeRecord.getTradeNo());
    }

    // ===================== 内部工具方法 =====================

    /**
     * 记录价格历史
     */
    private void recordPriceHistory(Long artworkId, BigDecimal beforePrice, BigDecimal afterPrice, String reason, Long resaleId) {
        BigDecimal growthRate = BigDecimal.ZERO;
        if (beforePrice != null && beforePrice.compareTo(BigDecimal.ZERO) > 0) {
            growthRate = afterPrice.subtract(beforePrice)
                    .multiply(new BigDecimal("100"))
                    .divide(beforePrice, 2, RoundingMode.HALF_UP);
        }

        ArtworkPriceHistory history = new ArtworkPriceHistory();
        history.setArtworkId(artworkId);
        history.setBeforePrice(beforePrice);
        history.setAfterPrice(afterPrice);
        history.setGrowthRate(growthRate);
        history.setReason(reason);
        history.setRelatedResaleId(resaleId);
        history.setCreatedTime(LocalDateTime.now());
        artworkPriceHistoryMapper.insert(history);

        log.info("记录价格历史: artworkId={}, before={}, after={}, growthRate={}%, reason={}",
                artworkId, beforePrice, afterPrice, growthRate, reason);
    }

    /**
     * 获取当前交易轮次
     */
    private int getMaxTradeRound(Long artworkId) {
        List<ArtworkTradeRecord> records = artworkTradeRecordMapper.selectList(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                        .orderByDesc(ArtworkTradeRecord::getTradeRound)
                        .last("LIMIT 1"));
        return records.isEmpty() ? 0 : records.get(0).getTradeRound();
    }

    /**
     * 计算总涨幅
     */
    private BigDecimal calculateTotalGrowthRate(Long artworkId, BigDecimal currentPrice) {
        List<ArtworkTradeRecord> firstTrade = artworkTradeRecordMapper.selectList(
                new LambdaQueryWrapper<ArtworkTradeRecord>()
                        .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                        .eq(ArtworkTradeRecord::getTradeType, "first_sale")
                        .orderByAsc(ArtworkTradeRecord::getTradeRound)
                        .last("LIMIT 1"));
        if (firstTrade.isEmpty() || firstTrade.get(0).getTradePrice().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal firstPrice = firstTrade.get(0).getTradePrice();
        return currentPrice.subtract(firstPrice)
                .multiply(new BigDecimal("100"))
                .divide(firstPrice, 2, RoundingMode.HALF_UP);
    }

    /**
     * 生成交易编号
     */
    private String generateTradeNo(String prefix) {
        return prefix + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
}
