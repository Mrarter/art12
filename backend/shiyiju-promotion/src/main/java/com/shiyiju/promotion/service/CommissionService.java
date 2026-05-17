package com.shiyiju.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.user.entity.CommissionRecord;
import com.shiyiju.user.entity.PromoterRecord;
import com.shiyiju.user.mapper.CommissionRecordMapper;
import com.shiyiju.user.mapper.PromoterRecordMapper;
import com.shiyiju.user.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金计算服务 - 二级分销+团队奖励
 * 使用统一 commission_record 表记录，结算后自动入账钱包
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRecordMapper commissionRecordMapper;
    private final PromoterRecordMapper promoterRecordMapper;
    private final WalletService walletService;

    /** 直接推广佣金比例 - 5% */
    private static final BigDecimal DIRECT_COMMISSION_RATE = new BigDecimal("0.05");

    /** 团队奖励佣金比例 - 2% */
    private static final BigDecimal TEAM_COMMISSION_RATE = new BigDecimal("0.02");

    /** 待结算状态 */
    private static final String STATUS_PENDING = "pending";
    /** 已结算状态（已入账） */
    private static final String STATUS_SETTLED = "settled";

    /**
     * 计算并发放佣金（二级分销+团队奖励）
     */
    @Transactional
    public void calculateAndSettleCommission(Long orderId, String orderNo, BigDecimal orderAmount,
                                              Long buyerId, Long promoterId, Long artworkId) {
        log.info("开始计算佣金 - orderId:{}, amount:{}, buyerId:{}, promoterId:{}",
                orderId, orderAmount, buyerId, promoterId);

        if (promoterId == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 一级佣金：直接推广佣金
        settleCommission(orderId, orderNo, orderAmount, promoterId, buyerId, artworkId, 1,
                "promoter_reward", DIRECT_COMMISSION_RATE);

        // 二级佣金：团队奖励
        settleTeamCommission(orderId, orderNo, orderAmount, buyerId, promoterId, artworkId);
    }

    /**
     * 结算单级佣金
     */
    private void settleCommission(Long orderId, String orderNo, BigDecimal orderAmount,
                                   Long userId, Long sourceUserId, Long artworkId,
                                   int level, String commissionType, BigDecimal rate) {
        BigDecimal commissionAmount = orderAmount.multiply(rate);
        long amountLong = commissionAmount.longValue();
        if (amountLong <= 0) return;

        // 写入统一佣金记录表
        CommissionRecord record = new CommissionRecord();
        record.setUserId(userId);
        record.setSourceUserId(sourceUserId);
        record.setOrderId(orderId);
        record.setArtworkId(artworkId);
        record.setCommissionType(commissionType);
        record.setCommissionLevel(level);
        record.setRate(rate.multiply(new BigDecimal("100")));
        record.setAmount(BigDecimal.valueOf(amountLong));
        record.setStatus(STATUS_SETTLED);
        record.setRemark(commissionType + " " + orderNo);
        commissionRecordMapper.insert(record);

        // 更新艺荐官统计
        updatePromoterStats(userId, orderAmount, 1);

        // 佣金入账到钱包
        try {
            walletService.income(userId, BigDecimal.valueOf(amountLong),
                    "commission", orderId, "order",
                    (level == 1 ? "一级推广佣金" : "二级团队奖励") + " " + orderNo);
            log.info("佣金结算完成 - userId:{}, type:{}, amount:{}", userId, commissionType, amountLong);
        } catch (Exception e) {
            log.error("佣金入账失败: userId={}, amount={}", userId, amountLong, e);
        }
    }

    /**
     * 二级佣金：团队奖励
     */
    private void settleTeamCommission(Long orderId, String orderNo, BigDecimal orderAmount,
                                       Long buyerId, Long directPromoterId, Long artworkId) {
        PromoterRecord buyerPromoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, buyerId)
                        .eq(PromoterRecord::getStatus, 1));

        if (buyerPromoter == null) return;

        Long upperPromoterId = buyerPromoter.getParentId();
        if (upperPromoterId == null || upperPromoterId.equals(directPromoterId)) return;

        settleCommission(orderId, orderNo, orderAmount, upperPromoterId, buyerId, artworkId,
                2, "team_reward", TEAM_COMMISSION_RATE);
    }

    /**
     * 更新艺荐官统计数据
     */
    private void updatePromoterStats(Long promoterId, BigDecimal orderAmount, int orderCount) {
        PromoterRecord promoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, promoterId));
        if (promoter == null) return;

        promoter.setTotalSales(promoter.getTotalSales() == null
                ? orderAmount : promoter.getTotalSales().add(orderAmount));
        promoter.setTotalOrders(promoter.getTotalOrders() == null
                ? orderCount : promoter.getTotalOrders() + orderCount);
        promoterRecordMapper.updateById(promoter);
    }
}
