package com.shiyiju.auction.controller;

import com.shiyiju.auction.entity.AuctionDeposit;
import com.shiyiju.auction.service.AuctionDepositService;
import com.shiyiju.common.result.Result;
import com.shiyiju.common.service.AlipayService;
import com.shiyiju.common.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auction")
public class AuctionPaymentController {
    private final AuctionDepositService depositService;
    private final AlipayService alipayService;
    private final WxPayService wxPayService;

    @PostMapping("/sessions/{sessionId}/deposit/orders")
    public Result<Map<String, Object>> create(@PathVariable Long sessionId,
            @RequestHeader(value="X-User-Id", required=false) Long userId,
            @RequestBody(required=false) Map<String, Object> body) {
        if (userId == null) return Result.fail(401, "请先登录");
        Map<String, Object> p = body == null ? Map.of() : body;
        return Result.success(depositService.createPayment(sessionId, userId,
                String.valueOf(p.getOrDefault("channel", "alipay")),
                String.valueOf(p.getOrDefault("scene", "h5")),
                p.get("openId") == null ? null : p.get("openId").toString()));
    }

    @GetMapping("/deposits/{payNo}/status")
    public Result<Map<String, Object>> status(@PathVariable String payNo,
            @RequestHeader(value="X-User-Id", required=false) Long userId) {
        if (userId == null) return Result.fail(401, "请先登录");
        AuctionDeposit deposit = depositService.findOwned(payNo, userId);
        return deposit == null ? Result.fail(404, "保证金订单不存在") : Result.success(depositService.statusMap(deposit));
    }

    @PostMapping("/pay/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        if (!alipayService.verifyNotify(params)) return "fail";
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) return "success";
        try {
            return depositService.markPaid(params.get("out_trade_no"), params.get("trade_no"),
                    new BigDecimal(params.get("total_amount"))) ? "success" : "fail";
        } catch (RuntimeException e) {
            log.error("拍卖支付宝回调处理失败: payNo={}", params.get("out_trade_no"), e);
            return "fail";
        }
    }

    @PostMapping(value="/pay/wechat/notify", produces="text/xml;charset=UTF-8")
    public String wechatNotify(@RequestBody String xml) {
        try {
            Map<String, String> p = wxPayService.parseCallbackNotify(xml);
            if (!wxPayService.verifyCallbackSign(p, p.get("sign"))) return wxPayService.buildFailResponse("签名失败");
            BigDecimal amount = new BigDecimal(p.get("total_fee")).movePointLeft(2);
            return depositService.markPaid(p.get("out_trade_no"), p.get("transaction_id"), amount)
                    ? wxPayService.buildSuccessResponse() : wxPayService.buildFailResponse("订单或金额不匹配");
        } catch (RuntimeException e) {
            log.error("拍卖微信回调处理失败", e);
            return wxPayService.buildFailResponse("系统异常");
        }
    }
}
