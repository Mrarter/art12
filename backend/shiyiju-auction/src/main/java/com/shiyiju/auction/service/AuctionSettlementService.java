package com.shiyiju.auction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.auction.entity.*;
import com.shiyiju.auction.mapper.*;
import com.shiyiju.common.constant.AuctionConstant;
import com.shiyiju.common.service.AlipayService;
import com.shiyiju.common.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSettlementService {
    private final AuctionSessionMapper sessionMapper;
    private final AuctionLotMapper lotMapper;
    private final AuctionDepositMapper depositMapper;
    private final AlipayService alipayService;
    private final WxPayService wxPayService;

    @Scheduled(fixedDelayString = "${auction.settlement-interval-ms:60000}")
    public void settleExpiredSessions() {
        List<AuctionSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AuctionSession>()
                .lt(AuctionSession::getEndTime, LocalDateTime.now())
                .ne(AuctionSession::getStatus, AuctionConstant.SESSION_STATUS_ENDED));
        sessions.forEach(session -> {
            try { settle(session.getId()); }
            catch (Exception e) { log.error("拍卖结算失败，稍后重试: sessionId={}", session.getId(), e); }
        });
        retryRefunds();
    }

    @Transactional
    public void settle(Long sessionId) {
        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session == null || AuctionConstant.SESSION_STATUS_ENDED.equals(session.getStatus())) return;
        List<AuctionLot> lots = lotMapper.selectList(new LambdaQueryWrapper<AuctionLot>()
                .eq(AuctionLot::getSessionId, sessionId));
        Set<Long> winners = new HashSet<>();
        for (AuctionLot lot : lots) {
            if (AuctionConstant.LOT_STATUS_SOLD.equals(lot.getStatus()) || AuctionConstant.LOT_STATUS_UNSOLD.equals(lot.getStatus())) continue;
            boolean reserveReached = lot.getBuyerId() != null && (lot.getReservePrice() == null
                    || lot.getReservePrice().signum() == 0 || lot.getCurrentPrice().compareTo(lot.getReservePrice()) >= 0);
            lot.setStatus(reserveReached ? AuctionConstant.LOT_STATUS_SOLD : AuctionConstant.LOT_STATUS_UNSOLD);
            if (reserveReached) winners.add(lot.getBuyerId()); else lot.setBuyerId(null);
            lot.setUpdateTime(LocalDateTime.now());
            lotMapper.updateById(lot);
        }
        List<AuctionDeposit> deposits = depositMapper.selectList(new LambdaQueryWrapper<AuctionDeposit>()
                .eq(AuctionDeposit::getSessionId, sessionId)
                .eq(AuctionDeposit::getPayStatus, AuctionConstant.DEPOSIT_PAID));
        for (AuctionDeposit deposit : deposits) {
            if (winners.contains(deposit.getUserId())) {
                deposit.setPayStatus(AuctionConstant.DEPOSIT_DEDUCTED);
                deposit.setUpdateTime(LocalDateTime.now());
                depositMapper.updateById(deposit);
            } else if (deposit.getRefundNo() == null) {
                deposit.setRefundNo("AR" + UUID.randomUUID().toString().replace("-", ""));
                deposit.setUpdateTime(LocalDateTime.now());
                depositMapper.updateById(deposit);
            }
        }
        session.setStatus(AuctionConstant.SESSION_STATUS_ENDED);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    /** Provider calls stay outside the settlement transaction; failed refunds remain retryable. */
    public void retryRefunds() {
        List<AuctionDeposit> pending = depositMapper.selectList(new LambdaQueryWrapper<AuctionDeposit>()
                .eq(AuctionDeposit::getPayStatus, AuctionConstant.DEPOSIT_PAID)
                .isNotNull(AuctionDeposit::getRefundNo).last("LIMIT 20"));
        for (AuctionDeposit deposit : pending) {
            try {
                boolean success;
                if ("wechat".equalsIgnoreCase(deposit.getPayChannel())) {
                    String cents = deposit.getAmount().movePointRight(2).toBigIntegerExact().toString();
                    success = wxPayService.refund(deposit.getPayNo(), deposit.getRefundNo(), cents, cents);
                } else {
                    alipayService.refund(deposit.getPayNo(), deposit.getRefundNo(), deposit.getAmount(), "拍卖未中标保证金退回");
                    success = true;
                }
                if (success) markRefunded(deposit.getId());
            } catch (Exception e) {
                log.error("保证金退款失败，保留待重试: payNo={}", deposit.getPayNo(), e);
            }
        }
    }

    @Transactional
    public void markRefunded(Long id) {
        AuctionDeposit deposit = depositMapper.selectById(id);
        if (deposit == null || !AuctionConstant.DEPOSIT_PAID.equals(deposit.getPayStatus())) return;
        deposit.setPayStatus(AuctionConstant.DEPOSIT_REFUNDED);
        deposit.setRefundTime(LocalDateTime.now()); deposit.setUpdateTime(LocalDateTime.now());
        depositMapper.updateById(deposit);
    }
}
