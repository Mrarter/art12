package com.shiyiju.order.controller;

import com.shiyiju.common.constant.OrderConstant;
import com.shiyiju.common.event.FinanceEvent;
import com.shiyiju.common.event.FinanceEventPublisher;
import com.shiyiju.common.event.FinanceEventType;
import com.shiyiju.common.service.WxPayService;
import com.shiyiju.order.entity.Order;
import com.shiyiju.order.mapper.OrderMapper;
import com.shiyiju.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * 微信支付回调接口 — Phase 5.3 终极一致性版
 *
 * 支付：仅更新本地订单状态，资金由 FinanceEvent 异步处理
 * 退款：Redis 幂等锁 + 完整资金回滚 + 转售状态恢复
 */
@Slf4j
@RestController
@RequestMapping("/pay/callback")
@RequiredArgsConstructor
public class WxPayCallbackController {

    private final WxPayService wxPayService;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final FinanceEventPublisher financeEventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${resale.platform-wallet-user-id:0}")
    private Long platformWalletUserId;

    @PostMapping("/notify")
    public String handlePayNotify(@RequestBody String xmlData) {
        log.info("收到微信支付回调: {}", xmlData);
        try {
            Map<String, String> params = wxPayService.parseCallbackNotify(xmlData);
            String sign = params.get("sign");
            if (!wxPayService.verifyCallbackSign(params, sign)) {
                log.warn("签名验证失败");
                return wxPayService.buildFailResponse("签名验证失败");
            }
            if ("SUCCESS".equals(params.get("return_code"))
                    && "SUCCESS".equals(params.get("result_code"))) {
                String orderNo = params.get("out_trade_no");
                log.info("支付成功: orderNo={}", orderNo);
                orderService.handlePayCallback(orderNo, params.get("transaction_id"));
                return wxPayService.buildSuccessResponse();
            }
            return wxPayService.buildFailResponse(params.get("err_code_des"));
        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            return wxPayService.buildFailResponse("系统异常");
        }
    }

    @PostMapping("/refund")
    public String handleRefundNotify(@RequestBody String xmlData) {
        log.info("收到退款回调: {}", xmlData);
        try {
            Map<String, String> params = wxPayService.parseCallbackNotify(xmlData);
            if ("SUCCESS".equals(params.get("return_code"))
                    && "SUCCESS".equals(params.get("result_code"))) {
                String orderNo = params.get("out_trade_no");
                log.info("退款成功: orderNo={}", orderNo);
                handleRefundSuccess(orderNo);
                return wxPayService.buildSuccessResponse();
            }
            return wxPayService.buildFailResponse(params.get("err_code_des"));
        } catch (Exception e) {
            log.error("处理退款回调异常", e);
            return wxPayService.buildFailResponse("系统异常");
        }
    }

    /**
     * 退款成功 — 幂等锁 + 事件驱动回滚
     *
     * 1. Redis 幂等锁（refund:lock:{orderNo}，30秒自动过期）
     * 2. 订单状态幂等检查
     * 3. 发布退款事件（RESALE_ROLLBACK / REFUND_ARTIST）
     * 4. 更新订单为 REFUNDED
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundSuccess(String orderNo) {
        // 1. Redis 幂等锁
        String lockKey = "refund:lock:" + orderNo;
        Boolean locked = acquireLock(lockKey);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("退款处理中，幂等返回: orderNo={}", orderNo);
            return;
        }
        try {
            // 2. 订单幂等检查
            Order order = orderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));
            if (order == null) {
                log.error("退款：订单不存在, orderNo={}", orderNo);
                return;
            }
            if ("REFUNDED".equals(order.getStatus())) {
                log.info("订单 {} 已退款，幂等返回", orderNo);
                return;
            }

            // 3. 发布退款事件
            FinanceEvent event = FinanceEvent.builder()
                    .orderNo(orderNo)
                    .relatedId(order.getId())
                    .relatedType("order_refund")
                    .platformWalletUserId(platformWalletUserId)
                    .build();

            if (OrderConstant.SOURCE_RESALE.equals(order.getSource())) {
                String remark = order.getRemark();
                Long resaleId = remark != null && remark.startsWith("resale:")
                        ? Long.parseLong(remark.substring("resale:".length())) : null;
                event.setType(FinanceEventType.RESALE_ROLLBACK);
                event.setResaleId(resaleId);
                event.setArtworkId(orderService.getFirstArtworkId(order.getId()));
            } else {
                event.setType(FinanceEventType.REFUND_ARTIST);
            }

            financeEventPublisher.publish(event);
            log.info("退款事件已发布: orderNo={}, type={}", orderNo, event.getType());

            // 4. 更新订单状态
            order.setStatus("REFUNDED");
            order.setPaymentStatus("REFUNDED");
            orderMapper.updateById(order);
            log.info("订单 {} 退款完成", orderNo);

        } catch (Exception e) {
            log.error("退款处理异常: orderNo={}", orderNo, e);
            throw e;
        } finally {
            releaseLock(lockKey);
        }
    }

    private Boolean acquireLock(String key) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(30));
        } catch (Exception e) {
            log.warn("Redis不可用，降级: key={}", key);
            return true;
        }
    }

    private void releaseLock(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis释放失败: key={}", key);
        }
    }
}
