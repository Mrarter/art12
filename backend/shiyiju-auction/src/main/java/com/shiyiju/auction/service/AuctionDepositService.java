package com.shiyiju.auction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.auction.entity.*;
import com.shiyiju.auction.mapper.*;
import com.shiyiju.common.constant.AuctionConstant;
import com.shiyiju.common.service.AlipayService;
import com.shiyiju.common.service.WxPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuctionDepositService {
    private final AuctionSessionMapper sessionMapper;
    private final AuctionLotMapper lotMapper;
    private final AuctionDepositMapper depositMapper;
    private final AlipayService alipayService;
    private final WxPayService wxPayService;

    @Transactional
    public Map<String, Object> createPayment(Long sessionId, Long userId, String channel,
                                             String scene, String openId) {
        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("拍卖专场不存在");
        LocalDateTime now = LocalDateTime.now();
        if (session.getEndTime() != null && !now.isBefore(session.getEndTime())) {
            throw new IllegalStateException("拍卖专场已结束");
        }
        BigDecimal amount = lotMapper.selectList(new LambdaQueryWrapper<AuctionLot>()
                        .eq(AuctionLot::getSessionId, sessionId))
                .stream().map(AuctionLot::getDepositAmount).filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO).setScale(2);
        if (amount.signum() <= 0) throw new IllegalStateException("该专场无需缴纳保证金");

        String normalizedChannel = "wechat".equalsIgnoreCase(channel) ? "wechat" : "alipay";
        AuctionDeposit deposit = depositMapper.selectSessionDepositForUpdate(sessionId, userId);
        if (deposit != null && AuctionConstant.DEPOSIT_PAID.equals(deposit.getPayStatus())) {
            return statusMap(deposit);
        }
        if (deposit == null) {
            deposit = new AuctionDeposit();
            deposit.setSessionId(sessionId);
            deposit.setUserId(userId);
            deposit.setCreateTime(now);
        }
        deposit.setAmount(amount);
        deposit.setPayStatus(AuctionConstant.DEPOSIT_UNPAID);
        deposit.setPayChannel(normalizedChannel);
        deposit.setPayNo("AD" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        deposit.setExpireTime(now.plusMinutes(30));
        deposit.setUpdateTime(now);
        if (deposit.getId() == null) depositMapper.insert(deposit); else depositMapper.updateById(deposit);

        Map<String, Object> result = new LinkedHashMap<>(statusMap(deposit));
        String subject = "拍卖保证金-" + session.getTitle();
        if ("alipay".equals(normalizedChannel)) {
            boolean app = "app".equalsIgnoreCase(scene);
            result.putAll(app ? alipayService.createAppPay(deposit.getPayNo(), amount, subject)
                    : alipayService.createWapPay(deposit.getPayNo(), amount, subject));
        } else {
            int cents = amount.movePointRight(2).intValueExact();
            if ("app".equalsIgnoreCase(scene)) {
                result.put("payParams", wxPayService.unifiedOrderApp(deposit.getPayNo(), String.valueOf(cents), subject));
            } else {
                if (openId == null || openId.isBlank()) throw new IllegalArgumentException("微信支付缺少 openId");
                result.put("payParams", wxPayService.unifiedOrderJsApi(deposit.getPayNo(), String.valueOf(cents), openId, subject, scene));
            }
        }
        return result;
    }

    public AuctionDeposit findOwned(String payNo, Long userId) {
        return depositMapper.selectOne(new LambdaQueryWrapper<AuctionDeposit>()
                .eq(AuctionDeposit::getPayNo, payNo).eq(AuctionDeposit::getUserId, userId));
    }

    @Transactional
    public boolean markPaid(String payNo, String transactionId, BigDecimal paidAmount) {
        AuctionDeposit deposit = depositMapper.selectOne(new LambdaQueryWrapper<AuctionDeposit>()
                .eq(AuctionDeposit::getPayNo, payNo).last("FOR UPDATE"));
        if (deposit == null) return false;
        if (AuctionConstant.DEPOSIT_PAID.equals(deposit.getPayStatus())) return true;
        if (paidAmount == null || deposit.getAmount().compareTo(paidAmount.setScale(2)) != 0) return false;
        deposit.setPayStatus(AuctionConstant.DEPOSIT_PAID);
        deposit.setTransactionId(transactionId);
        deposit.setPayTime(LocalDateTime.now());
        deposit.setUpdateTime(LocalDateTime.now());
        depositMapper.updateById(deposit);
        return true;
    }

    public Map<String, Object> statusMap(AuctionDeposit d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("payNo", d.getPayNo()); map.put("sessionId", d.getSessionId());
        map.put("amount", d.getAmount()); map.put("channel", d.getPayChannel());
        map.put("status", d.getPayStatus()); map.put("paid", AuctionConstant.DEPOSIT_PAID.equals(d.getPayStatus()));
        map.put("expireTime", d.getExpireTime());
        return map;
    }
}
