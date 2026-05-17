package com.shiyiju.user.service.finance;

import com.shiyiju.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 风控服务 — 资金安全防护
 *
 * 规则：
 * 1. 单笔 > ¥50,000 → 需要人工审核
 * 2. 同一用户 1分钟内 > 5笔 → 限流
 * 3. 转售价格涨幅 > 300% → 拦截
 * 4. 高频退款用户 → 风险标记
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlService {

    /** 单笔限额 */
    private static final BigDecimal SINGLE_LIMIT = new BigDecimal("50000");

    /** 1分钟内最大交易次数 */
    private static final int MAX_TX_PER_MINUTE = 5;

    /** 涨幅上限 */
    private static final BigDecimal MAX_PRICE_INCREASE_RATE = new BigDecimal("3.00"); // 300%

    /** 用户交易频次计数（内存级，生产环境应使用 Redis） */
    private final ConcurrentHashMap<Long, UserTxCounter> txCounters = new ConcurrentHashMap<>();

    /**
     * 检查单笔金额限制
     */
    public void checkSingleAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(SINGLE_LIMIT) > 0) {
            log.warn("风控拦截: 单笔超限, amount={}, limit={}", amount, SINGLE_LIMIT);
            throw new BusinessException(400, "单笔交易金额超过 ¥50,000 限制，请联系客服处理");
        }
    }

    /**
     * 检查用户交易频次
     */
    public void checkTxFrequency(Long userId) {
        if (userId == null) return;
        long now = System.currentTimeMillis();
        UserTxCounter counter = txCounters.computeIfAbsent(userId, k -> new UserTxCounter());

        synchronized (counter) {
            // 清理超过1分钟的旧记录
            counter.clean(now);

            if (counter.count() >= MAX_TX_PER_MINUTE) {
                log.warn("风控拦截: 用户交易频次超限, userId={}, count={}", userId, counter.count());
                throw new BusinessException(429, "交易过于频繁，请稍后再试");
            }
            counter.increment(now);
        }
    }

    /**
     * 检查转售价格涨幅
     */
    public void checkResalePriceIncrease(BigDecimal originalPrice, BigDecimal resalePrice) {
        if (originalPrice == null || resalePrice == null) return;
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal increaseRate = resalePrice.subtract(originalPrice)
                .divide(originalPrice, 2, java.math.RoundingMode.HALF_UP);
        if (increaseRate.compareTo(MAX_PRICE_INCREASE_RATE) > 0) {
            log.warn("风控拦截: 转售涨幅超限, original={}, resale={}, rate={}%",
                    originalPrice, resalePrice, increaseRate.multiply(new BigDecimal("100")));
            throw new BusinessException(400, "转售价格涨幅超过300%，已被风控系统拦截");
        }
    }

    /**
     * 用户交易计数器（内存实现）
     */
    static class UserTxCounter {
        private final long[] timestamps = new long[MAX_TX_PER_MINUTE];
        private int index = 0;
        private int count = 0;

        synchronized void clean(long now) {
            int valid = 0;
            for (int i = 0; i < count; i++) {
                if (now - timestamps[(index - count + i + MAX_TX_PER_MINUTE) % MAX_TX_PER_MINUTE] < 60000) {
                    timestamps[valid++] = timestamps[(index - count + i + MAX_TX_PER_MINUTE) % MAX_TX_PER_MINUTE];
                }
            }
            count = valid;
        }

        synchronized void increment(long now) {
            timestamps[index] = now;
            index = (index + 1) % MAX_TX_PER_MINUTE;
            if (count < MAX_TX_PER_MINUTE) count++;
        }

        synchronized int count() { return count; }
    }
}
