package com.shiyiju.order.controller;

import com.shiyiju.common.service.AlipayService;
import com.shiyiju.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付宝支付异步通知。
 */
@Slf4j
@RestController
@RequestMapping("/pay/alipay")
@RequiredArgsConstructor
public class AlipayCallbackController {

    private final AlipayService alipayService;
    private final OrderService orderService;

    @PostMapping("/notify")
    public String handleNotify(@RequestParam Map<String, String> params) {
        Map<String, String> safeLogParams = params.entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        log.info("收到支付宝支付回调: {}", safeLogParams);

        if (!alipayService.verifyNotify(params)) {
            log.warn("支付宝回调签名验证失败: out_trade_no={}", params.get("out_trade_no"));
            return "fail";
        }

        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            orderService.handlePayCallback(params.get("out_trade_no"), params.get("trade_no"));
            return "success";
        }

        log.info("支付宝回调非成功状态: out_trade_no={}, trade_status={}",
                params.get("out_trade_no"), tradeStatus);
        return "success";
    }
}
