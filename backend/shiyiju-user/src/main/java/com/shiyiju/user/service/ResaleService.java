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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private final WalletService walletService;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    /** 艺术家持续收益比例（默认5%） */
    @Value("${resale.artist-income-rate:0.05}")
    private BigDecimal artistIncomeRate;

    /** 平台服务费比例（默认10%） */
    @Value("${resale.platform-fee-rate:0.10}")
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

        // 6. 计算预估收益
        BigDecimal artistIncome = resalePrice.multiply(artistIncomeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = resalePrice.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerIncome = resalePrice.subtract(artistIncome).subtract(platformFee)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. 创建转售记录
        ResaleRecord record = new ResaleRecord();
        record.setArtworkId(artworkId);
        record.setSellerUserId(sellerUserId);
        record.setResalePrice(resalePrice);
        record.setArtistIncome(artistIncome);
        record.setPlatformFee(platformFee);
        record.setSellerIncome(sellerIncome);
        record.setStatus("pending");
        record.setVersion(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        resaleRecordMapper.insert(record);

        log.info("发布转售: id={}, artworkId={}, sellerId={}, price={}, artistIncome={}, platformFee={}, sellerIncome={}",
                record.getId(), artworkId, sellerUserId, resalePrice, artistIncome, platformFee, sellerIncome);

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

            // 2. 生成交易编号（用于幂等）
            String tradeNo = generateTradeNo("RES");

            // 3. 状态变更：pending -> paid（带乐观锁）
            int rows = resaleRecordMapper.updateStatus(resaleId, "pending", "paid", buyerUserId, tradeNo, record.getVersion());
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
     * 2. 平台服务费入账（10%）
     * 3. 卖家收入入账（85%）
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
        if (record.getPlatformFee().compareTo(BigDecimal.ZERO) > 0 && platformWalletUserId != null && platformWalletUserId > 0) {
            walletService.income(platformWalletUserId, record.getPlatformFee(), "resale",
                    resaleId, "resale",
                    "平台转售服务费: 转售ID=" + resaleId + ", 价格=" + record.getResalePrice());
            log.info("平台服务费入账: walletId={}, amount={}, resaleId={}",
                    platformWalletUserId, record.getPlatformFee(), resaleId);
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
        return resaleRecordMapper.selectPage(p, wrapper);
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
        return resaleRecordMapper.selectPage(p, wrapper);
    }

    /**
     * 获取转售详情
     */
    public ResaleRecord getResaleDetail(Long resaleId) {
        ResaleRecord record = resaleRecordMapper.selectById(resaleId);
        if (record == null) {
            throw new BusinessException(404, "转售记录不存在");
        }
        return record;
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
