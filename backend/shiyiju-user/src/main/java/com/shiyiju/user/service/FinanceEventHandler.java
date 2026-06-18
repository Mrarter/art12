package com.shiyiju.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.entity.Artwork;
import com.shiyiju.common.event.FinanceEvent;
import com.shiyiju.common.event.FinanceEventType;
import com.shiyiju.common.mapper.ArtworkMapper;
import com.shiyiju.user.entity.ArtworkPriceHistory;
import com.shiyiju.user.entity.ArtworkTradeRecord;
import com.shiyiju.user.entity.CommissionRecord;
import com.shiyiju.user.entity.PromoterRecord;
import com.shiyiju.user.entity.ResaleRecord;
import com.shiyiju.user.mapper.ArtworkPriceHistoryMapper;
import com.shiyiju.user.mapper.ArtworkTradeRecordMapper;
import com.shiyiju.user.mapper.CommissionRecordMapper;
import com.shiyiju.user.mapper.PromoterRecordMapper;
import com.shiyiju.user.mapper.ResaleRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 金融事件处理器 — 事件驱动资金系统的核心
 *
 * 所有 FinanceEvent 由本处理器接收并执行对应的 WalletService 操作。
 * 使用 @TransactionalEventListener 确保在发布方事务提交后才处理事件。
 *
 * 设计原则：
 * 1. 每个事件类型对应一个处理方法
 * 2. 所有资金最终操作统一走 WalletService
 * 3. 失败时记录日志（可扩展为死信队列+补偿机制）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceEventHandler {

    private final WalletService walletService;
    private final ResaleService resaleService;
    private final ResaleRecordMapper resaleRecordMapper;
    private final ArtworkTradeRecordMapper artworkTradeRecordMapper;
    private final ArtworkPriceHistoryMapper artworkPriceHistoryMapper;
    private final ArtworkMapper artworkMapper;
    private final CommissionRecordMapper commissionRecordMapper;
    private final PromoterRecordMapper promoterRecordMapper;

    private static final BigDecimal DIRECT_COMMISSION_RATE = new BigDecimal("0.05");
    private static final BigDecimal TEAM_COMMISSION_RATE = new BigDecimal("0.02");

    /**
     * 处理所有金融事件（发布方事务提交后执行）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(rollbackFor = Exception.class)
    public void handleFinanceEvent(FinanceEvent event) {
        log.info("处理金融事件: type={}, userId={}, amount={}",
                event.getType(), event.getUserId(), event.getAmount());

        try {
            switch (event.getType()) {
                // ===== 入账事件 =====
                case ARTIST_INCOME -> handleArtistIncome(event);
                case SELLER_INCOME -> handleSellerIncome(event);
                case PLATFORM_FEE -> handlePlatformFee(event);
                case COMMISSION_SETTLE -> handleCommissionSettle(event);
                case RESALE_MARK_PAID -> handleResaleMarkPaid(event);

                // ===== 退款事件 =====
                case REFUND_ARTIST -> handleRefundArtist(event);
                case REFUND_SELLER -> handleRefundSeller(event);
                case REFUND_PLATFORM -> handleRefundPlatform(event);
                case RESALE_ROLLBACK -> handleResaleRollback(event);

                default -> log.warn("未知金融事件类型: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("处理金融事件失败: type={}, event={}", event.getType(), event, e);
            // TODO: 发送到死信队列或重试队列
        }
    }

    // ===================== 入账处理 =====================

    private void handleArtistIncome(FinanceEvent e) {
        if (e.getUserId() == null || e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) return;
        walletService.income(e.getUserId(), e.getAmount(), "income",
                e.getRelatedId(), e.getRelatedType(),
                "艺术家收益: " + e.getRemark());
    }

    private void handleSellerIncome(FinanceEvent e) {
        if (e.getUserId() == null || e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) return;
        walletService.income(e.getUserId(), e.getAmount(), "resale",
                e.getRelatedId(), "resale",
                "转售收入: " + e.getRemark());
    }

    private void handlePlatformFee(FinanceEvent e) {
        if (e.getPlatformWalletUserId() == null || e.getPlatformWalletUserId() <= 0) return;
        if (e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) return;
        walletService.income(e.getPlatformWalletUserId(), e.getAmount(), "resale",
                e.getRelatedId(), "resale",
                "平台服务费: " + e.getRemark());
    }

    private void handleCommissionSettle(FinanceEvent e) {
        if (e.getUserId() == null || e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) return;
        settleCommission(e, e.getUserId(), e.getBuyerUserId(), 1, "promoter_reward", DIRECT_COMMISSION_RATE);

        PromoterRecord directPromoter = findActivePromoterByUserId(e.getUserId());
        if (directPromoter == null || directPromoter.getParentId() == null
                || directPromoter.getParentId().equals(e.getUserId())) {
            return;
        }
        settleCommission(e, directPromoter.getParentId(), e.getUserId(), 2, "team_reward", TEAM_COMMISSION_RATE);
    }

    private void settleCommission(FinanceEvent e, Long receiverUserId, Long sourceUserId,
                                  int level, String type, BigDecimal rate) {
        BigDecimal commissionAmount = e.getAmount().multiply(rate).setScale(2, RoundingMode.DOWN);
        if (commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CommissionRecord record = new CommissionRecord();
        record.setUserId(receiverUserId);
        record.setSourceUserId(sourceUserId);
        record.setOrderId(e.getRelatedId());
        record.setArtworkId(e.getArtworkId());
        record.setCommissionType(type);
        record.setCommissionLevel(level);
        record.setRate(rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.DOWN));
        record.setAmount(commissionAmount);
        record.setStatus("settled");
        record.setRemark(type + " " + e.getOrderNo());
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        commissionRecordMapper.insert(record);

        walletService.income(receiverUserId, commissionAmount, "commission",
                e.getRelatedId(), "order",
                (level == 1 ? "一级推广佣金: " : "二级团队奖励: ") + e.getRemark());
        updatePromoterStats(receiverUserId, e.getAmount());
    }

    private PromoterRecord findActivePromoterByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return promoterRecordMapper.selectOne(new LambdaQueryWrapper<PromoterRecord>()
                .eq(PromoterRecord::getUserId, userId)
                .eq(PromoterRecord::getStatus, 1)
                .last("LIMIT 1"));
    }

    private void updatePromoterStats(Long userId, BigDecimal orderAmount) {
        PromoterRecord promoter = findActivePromoterByUserId(userId);
        if (promoter == null) {
            return;
        }
        promoter.setTotalSales(promoter.getTotalSales() == null
                ? orderAmount : promoter.getTotalSales().add(orderAmount));
        promoter.setTotalOrders(promoter.getTotalOrders() == null ? 1 : promoter.getTotalOrders() + 1);
        promoterRecordMapper.updateById(promoter);
    }

    private void handleResaleMarkPaid(FinanceEvent e) {
        if (e.getResaleId() == null || e.getBuyerUserId() == null) return;
        ResaleRecord record = resaleRecordMapper.selectById(e.getResaleId());
        if (record == null) {
            log.warn("转售不存在: resaleId={}", e.getResaleId());
            return;
        }
        if ("completed".equals(record.getStatus())) return; // 幂等

        if ("pending".equals(record.getStatus())) {
            resaleService.markAsPaid(e.getResaleId(), e.getBuyerUserId());
        } else if (!"paid".equals(record.getStatus())) {
            log.warn("转售支付事件状态不可处理: resaleId={}, status={}", e.getResaleId(), record.getStatus());
            return;
        }
        resaleService.completeResale(e.getResaleId());
        log.info("转售支付后已完成权属流转: resaleId={}, buyerId={}", e.getResaleId(), e.getBuyerUserId());
    }

    // ===================== 退款处理 =====================

    private void handleRefundArtist(FinanceEvent e) {
        if (e.getUserId() == null || e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) return;
        // 使用 refund() 扣回已入账的艺术家收益（余额不足时扣到0）
        walletService.refund(e.getUserId(), e.getAmount(), "refund",
                e.getRelatedId(), "order_refund",
                "退款扣回艺术家收益: " + e.getRemark());
    }

    private void handleRefundSeller(FinanceEvent e) {
        if (e.getUserId() == null || e.getSellerIncome() == null || e.getSellerIncome().compareTo(BigDecimal.ZERO) <= 0) return;
        walletService.refund(e.getUserId(), e.getSellerIncome(), "refund",
                e.getRelatedId(), "resale_refund",
                "退款扣回卖家收入: " + e.getRemark());
    }

    private void handleRefundPlatform(FinanceEvent e) {
        if (e.getPlatformWalletUserId() == null || e.getPlatformWalletUserId() <= 0) return;
        if (e.getPlatformFee() == null || e.getPlatformFee().compareTo(BigDecimal.ZERO) <= 0) return;
        walletService.refund(e.getPlatformWalletUserId(), e.getPlatformFee(), "refund",
                e.getRelatedId(), "resale_refund",
                "退款扣回平台服务费: " + e.getRemark());
    }

    /**
     * 转售退款回滚 — 完整反向操作
     * 1. 扣回艺术家收益
     * 2. 扣回卖家收入
     * 3. 扣回平台服务费
     * 4. 恢复 resale_record 状态
     * 5. 移除 artwork_trade_record
     * 6. 恢复 artwork.holder
     */
    private void handleResaleRollback(FinanceEvent e) {
        if (e.getResaleId() == null) return;
        ResaleRecord record = resaleRecordMapper.selectById(e.getResaleId());
        if (record == null) {
            log.warn("转售退款回滚: 转售不存在, id={}", e.getResaleId());
            return;
        }
        if (!"paid".equals(record.getStatus()) && !"completed".equals(record.getStatus())) {
            log.warn("转售退款回滚: 状态不可回滚, id={}, status={}", e.getResaleId(), record.getStatus());
            return;
        }

        log.info("转售退款回滚: resaleId={}, price={}, artist={}, platform={}, seller={}",
                e.getResaleId(), record.getResalePrice(), record.getArtistIncome(),
                record.getPlatformFee(), record.getSellerIncome());

        // 1. 扣回艺术家收益（使用 refund，余额不足时扣到0）
        if (record.getArtistIncome() != null && record.getArtistIncome().compareTo(BigDecimal.ZERO) > 0) {
            Long artistId = e.getArtworkId() != null
                    ? artworkMapper.selectById(e.getArtworkId()).getAuthorId() : null;
            if (artistId != null) {
                walletService.refund(artistId, record.getArtistIncome(), "refund_reverse",
                        e.getResaleId(), "resale_refund",
                        "退款扣回艺术家收益: resaleId=" + e.getResaleId());
            }
        }

        // 2. 扣回卖家收入
        if (record.getSellerIncome() != null && record.getSellerIncome().compareTo(BigDecimal.ZERO) > 0) {
            walletService.refund(record.getSellerUserId(), record.getSellerIncome(), "refund_reverse",
                    e.getResaleId(), "resale_refund",
                    "退款扣回卖家收入: resaleId=" + e.getResaleId());
        }

        // 3. 扣回平台服务费
        if (e.getPlatformWalletUserId() != null && e.getPlatformWalletUserId() > 0
                && record.getPlatformFee() != null && record.getPlatformFee().compareTo(BigDecimal.ZERO) > 0) {
            walletService.refund(e.getPlatformWalletUserId(), record.getPlatformFee(), "refund_reverse",
                    e.getResaleId(), "resale_refund",
                    "退款扣回平台服务费: resaleId=" + e.getResaleId());
        }

        // 4. 保存 artworkId 后在回滚转售状态
        Long artworkId = record.getArtworkId();
        resaleService.rollbackResaleAfterRefund(e.getResaleId());

        // 5. 删除最近一条 trade_record（回滚本轮转售）
        if (artworkId != null) {
            artworkTradeRecordMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtworkTradeRecord>()
                            .eq(ArtworkTradeRecord::getArtworkId, artworkId)
                            .eq(ArtworkTradeRecord::getTradeType, "resale")
                            .orderByDesc(ArtworkTradeRecord::getTradeRound)
                            .last("LIMIT 1"));
        }

        log.info("转售退款回滚完成: resaleId={}", e.getResaleId());
    }
}
