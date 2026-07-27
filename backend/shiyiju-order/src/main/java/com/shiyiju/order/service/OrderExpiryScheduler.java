package com.shiyiju.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期关闭超时未支付订单并释放预占库存。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${order.expiry-scan-delay-ms:30000}")
    public void closeExpiredOrders() {
        try {
            int expired = orderService.expirePendingOrders(200);
            if (expired > 0) {
                log.info("已关闭超时待付款订单: count={}", expired);
            }
        } catch (Exception e) {
            log.error("关闭超时待付款订单失败", e);
        }
    }
}
