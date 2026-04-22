package com.shiyiju.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.user.entity.PromoterRecord;
import com.shiyiju.user.mapper.PromoterRecordMapper;
import com.shiyiju.promotion.entity.CommissionLog;
import com.shiyiju.promotion.mapper.CommissionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金计算服务 - 二级分销+团队奖励
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionLogMapper commissionLogMapper;
    private final PromoterRecordMapper promoterRecordMapper;

    /** 直接推广佣金比例 - 5% */
    private static final BigDecimal DIRECT_COMMISSION_RATE = new BigDecimal("0.05");
    
    /** 团队奖励佣金比例 - 2% */
    private static final BigDecimal TEAM_COMMISSION_RATE = new BigDecimal("0.02");

    /**
     * 计算并发放佣金（二级分销+团队奖励）
     * 
     * @param orderId 订单ID
     * @param orderNo 订单号
     * @param orderAmount 订单金额
     * @param buyerId 购买者ID
     * @param promoterId 锁客艺荐官ID（一级推广人）
     */
    @Transactional
    public void calculateAndSettleCommission(Long orderId, String orderNo, BigDecimal orderAmount, Long buyerId, Long promoterId) {
        log.info("开始计算佣金 - orderId:{}, orderAmount:{}, buyerId:{}, promoterId:{}", 
                orderId, orderAmount, buyerId, promoterId);
        
        if (promoterId == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("佣金计算参数异常，跳过 - promoterId:{}, orderAmount:{}", promoterId, orderAmount);
            return;
        }

        // ========== 一级佣金：直接推广佣金 ==========
        settleDirectCommission(orderId, orderNo, orderAmount, promoterId);

        // ========== 二级佣金：团队奖励 ==========
        // 如果购买者也是艺荐官，给其上级发放团队奖励
        settleTeamCommission(orderId, orderNo, orderAmount, buyerId, promoterId);
    }

    /**
     * 一级佣金：直接推广佣金
     */
    private void settleDirectCommission(Long orderId, String orderNo, BigDecimal orderAmount, Long promoterId) {
        // 计算佣金金额
        long commissionAmount = orderAmount
                .multiply(DIRECT_COMMISSION_RATE)
                .longValue();
        
        if (commissionAmount <= 0) {
            return;
        }

        // 创建佣金记录
        CommissionLog commissionLog = new CommissionLog();
        commissionLog.setPromoterId(promoterId);
        commissionLog.setOrderId(orderId);
        commissionLog.setLevel(1); // 一级
        commissionLog.setCommissionRate(DIRECT_COMMISSION_RATE.multiply(new BigDecimal("100"))); // 百分比
        commissionLog.setOrderAmount(orderAmount.longValue());
        commissionLog.setCommissionAmount(commissionAmount);
        commissionLog.setStatus(1); // 已结算
        commissionLog.setSettleTime(LocalDateTime.now());
        commissionLog.setCreateTime(LocalDateTime.now());
        commissionLogMapper.insert(commissionLog);

        // 更新艺荐官统计数据
        updatePromoterStats(promoterId, orderAmount, 1);

        log.info("一级佣金结算完成 - promoterId:{}, commission:{}", promoterId, commissionAmount);
    }

    /**
     * 二级佣金：团队奖励
     * 逻辑：如果购买者是艺荐官，给其上级艺荐官发放团队奖励
     */
    private void settleTeamCommission(Long orderId, String orderNo, BigDecimal orderAmount, Long buyerId, Long directPromoterId) {
        // 查询购买者是否是艺荐官
        PromoterRecord buyerPromoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, buyerId)
                        .eq(PromoterRecord::getStatus, 1)
        );

        if (buyerPromoter == null) {
            log.info("购买者不是艺荐官，跳过团队奖励");
            return;
        }

        // 获取购买者的上级
        Long upperPromoterId = buyerPromoter.getParentId();
        if (upperPromoterId == null || upperPromoterId.equals(directPromoterId)) {
            log.info("购买者无上级或上级是直接推广人，跳过团队奖励");
            return;
        }

        // 计算团队奖励金额
        long commissionAmount = orderAmount
                .multiply(TEAM_COMMISSION_RATE)
                .longValue();
        
        if (commissionAmount <= 0) {
            return;
        }

        // 创建佣金记录
        CommissionLog commissionLog = new CommissionLog();
        commissionLog.setPromoterId(upperPromoterId);
        commissionLog.setOrderId(orderId);
        commissionLog.setLevel(2); // 二级
        commissionLog.setCommissionRate(TEAM_COMMISSION_RATE.multiply(new BigDecimal("100"))); // 百分比
        commissionLog.setOrderAmount(orderAmount.longValue());
        commissionLog.setCommissionAmount(commissionAmount);
        commissionLog.setStatus(1); // 已结算
        commissionLog.setSettleTime(LocalDateTime.now());
        commissionLog.setCreateTime(LocalDateTime.now());
        commissionLogMapper.insert(commissionLog);

        // 更新艺荐官统计数据
        updatePromoterStats(upperPromoterId, orderAmount, 1);

        log.info("二级团队奖励结算完成 - upperPromoterId:{}, commission:{}", upperPromoterId, commissionAmount);
    }

    /**
     * 更新艺荐官统计数据
     */
    private void updatePromoterStats(Long promoterId, BigDecimal orderAmount, int orderCount) {
        PromoterRecord promoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, promoterId)
        );

        if (promoter != null) {
            // 更新累计销售额
            if (promoter.getTotalSales() == null) {
                promoter.setTotalSales(BigDecimal.ZERO);
            }
            promoter.setTotalSales(promoter.getTotalSales().add(orderAmount));
            
            // 更新累计订单数
            if (promoter.getTotalOrders() == null) {
                promoter.setTotalOrders(0);
            }
            promoter.setTotalOrders(promoter.getTotalOrders() + orderCount);
            
            promoterRecordMapper.updateById(promoter);
        }
    }
}
