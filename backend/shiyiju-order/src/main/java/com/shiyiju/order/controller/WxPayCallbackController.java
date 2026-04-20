package com.shiyiju.order.controller;

import com.shiyiju.common.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信支付回调接口
 * 
 * 微信支付完成后会通知此地址
 * 注意: 此接口不能使用网关的统一前缀 /api/order/
 * 因为微信支付回调不支持自定义Header
 */
@Slf4j
@RestController
@RequestMapping("/pay/callback")
@RequiredArgsConstructor
public class WxPayCallbackController {

    private final WxPayService wxPayService;

    /**
     * 微信支付回调通知 (V2版本)
     * 
     * 微信服务器会POST XML格式的支付结果通知
     * 需要返回 SUCCESS 表示已收到通知
     */
    @PostMapping("/notify")
    public String handlePayNotify(@RequestBody String xmlData) {
        log.info("收到微信支付回调: {}", xmlData);
        
        try {
            // 解析回调数据
            Map<String, String> params = wxPayService.parseCallbackNotify(xmlData);
            
            // 验证签名
            String sign = params.get("sign");
            if (!wxPayService.verifyCallbackSign(params, sign)) {
                log.warn("微信支付回调签名验证失败");
                return wxPayService.buildFailResponse("签名验证失败");
            }
            
            // 返回状态
            String returnCode = params.get("return_code");
            String resultCode = params.get("result_code");
            
            if ("SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode)) {
                // 支付成功
                String orderNo = params.get("out_trade_no");      // 商户订单号
                String transactionId = params.get("transaction_id"); // 微信支付订单号
                String totalFee = params.get("total_fee");          // 订单金额(分)
                String openid = params.get("openid");                // 用户openid
                
                log.info("支付成功 - 订单号: {}, 微信交易号: {}, 金额: {}分",
                        orderNo, transactionId, totalFee);
                
                // 处理支付成功业务逻辑
                handlePaySuccess(orderNo, transactionId, totalFee, openid);
                
                return wxPayService.buildSuccessResponse();
            } else {
                // 支付失败
                String errCode = params.get("err_code");
                String errMsg = params.get("err_code_des");
                log.error("支付失败 - 错误码: {}, 错误信息: {}", errCode, errMsg);
                
                return wxPayService.buildFailResponse(errMsg);
            }
            
        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return wxPayService.buildFailResponse("系统异常");
        }
    }

    /**
     * 微信支付退款回调
     */
    @PostMapping("/refund")
    public String handleRefundNotify(@RequestBody String xmlData) {
        log.info("收到微信退款回调: {}", xmlData);
        
        try {
            Map<String, String> params = wxPayService.parseCallbackNotify(xmlData);
            
            String returnCode = params.get("return_code");
            String resultCode = params.get("result_code");
            
            if ("SUCCESS".equals(returnCode)) {
                if ("SUCCESS".equals(resultCode)) {
                    // 退款成功
                    String orderNo = params.get("out_trade_no");
                    String refundId = params.get("refund_id");
                    
                    log.info("退款成功 - 订单号: {}, 退款ID: {}", orderNo, refundId);
                    handleRefundSuccess(orderNo, refundId);
                    
                    return wxPayService.buildSuccessResponse();
                } else {
                    // 退款失败
                    log.error("退款失败: {}", params.get("err_code_des"));
                    return wxPayService.buildFailResponse(params.get("err_code_des"));
                }
            } else {
                return wxPayService.buildFailResponse(params.get("return_msg"));
            }
            
        } catch (Exception e) {
            log.error("处理退款回调异常", e);
            return wxPayService.buildFailResponse("系统异常");
        }
    }

    /**
     * 支付成功业务处理
     */
    private void handlePaySuccess(String orderNo, String transactionId, String totalFee, String openid) {
        // 此处调用订单服务更新订单状态
        // 由于在order模块中，可以通过Feign调用或直接注入服务
        log.info("订单 {} 支付成功，微信交易号: {}", orderNo, transactionId);
        
        // TODO: 调用订单服务更新状态
        // orderService.updateOrderStatus(orderNo, transactionId);
    }

    /**
     * 退款成功业务处理
     */
    private void handleRefundSuccess(String orderNo, String refundId) {
        log.info("订单 {} 退款成功，退款ID: {}", orderNo, refundId);
        
        // TODO: 调用订单服务更新退款状态
    }
}
