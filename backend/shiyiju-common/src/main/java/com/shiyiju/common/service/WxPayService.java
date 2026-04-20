package com.shiyiju.common.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shiyiju.common.config.WxPayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 微信支付服务 (V2版本 - 使用Hutool实现)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayService {

    private final WxPayConfig wxPayConfig;

    /** 微信支付API地址 */
    private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    private static final String ORDER_QUERY_URL = "https://api.mch.weixin.qq.com/pay/orderquery";
    private static final String CLOSE_ORDER_URL = "https://api.mch.weixin.qq.com/pay/closeorder";
    private static final String REFUND_URL = "https://api.mch.weixin.qq.com/secapi/pay/refund";

    /**
     * 统一下单 (Native支付 - 返回支付二维码链接)
     * 适用于PC网页支付场景
     */
    public String unifiedOrderNative(String orderNo, String totalAmount, String description) {
        Map<String, String> params = buildUnifiedOrderParams(orderNo, totalAmount, description, "NATIVE", null);
        String response = executePost(UNIFIED_ORDER_URL, params);
        Map<String, String> result = parseXmlResponse(response);
        
        log.info("微信Native支付统一下单结果: {}", result);
        
        if ("SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"))) {
            return result.get("code_url");
        } else {
            throw new RuntimeException("微信支付下单失败: " + result.get("err_code_des"));
        }
    }

    /**
     * 统一下单 (JSAPI支付 - 返回预支付交易会话标识)
     * 适用于微信小程序和公众号支付
     */
    public Map<String, String> unifiedOrderJsApi(String orderNo, String totalAmount, String openId, String description) {
        Map<String, String> params = buildUnifiedOrderParams(orderNo, totalAmount, description, "JSAPI", openId);
        String response = executePost(UNIFIED_ORDER_URL, params);
        Map<String, String> result = parseXmlResponse(response);
        
        log.info("微信JSAPI支付统一下单结果: {}", result);
        
        if ("SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"))) {
            Map<String, String> payResult = new HashMap<>();
            payResult.put("prepay_id", result.get("prepay_id"));
            payResult.put("sign", generateJsApiSign(result.get("prepay_id")));
            return payResult;
        } else {
            throw new RuntimeException("微信JSAPI支付下单失败: " + result.get("err_code_des"));
        }
    }

    /**
     * 查询订单
     */
    public Map<String, String> queryOrder(String orderNo) {
        Map<String, String> params = buildBaseParams();
        params.put("out_trade_no", orderNo);
        String response = executePost(ORDER_QUERY_URL, params);
        return parseXmlResponse(response);
    }

    /**
     * 关闭订单
     */
    public boolean closeOrder(String orderNo) {
        Map<String, String> params = buildBaseParams();
        params.put("out_trade_no", orderNo);
        String response = executePost(CLOSE_ORDER_URL, params);
        Map<String, String> result = parseXmlResponse(response);
        return "SUCCESS".equals(result.get("return_code"));
    }

    /**
     * 申请退款
     */
    public boolean refund(String orderNo, String refundNo, String totalAmount, String refundAmount) {
        Map<String, String> params = buildBaseParams();
        params.put("out_trade_no", orderNo);
        params.put("out_refund_no", refundNo);
        params.put("total_fee", totalAmount);
        params.put("refund_fee", refundAmount);
        params.put("refund_desc", "用户申请退款");
        
        // 退款需要双向证书，这里简化处理
        String response = executePostWithCert(REFUND_URL, params, wxPayConfig.getKeyPath());
        Map<String, String> result = parseXmlResponse(response);
        return "SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"));
    }

    /**
     * 验证支付回调签名
     */
    public boolean verifyCallbackSign(Map<String, String> params, String sign) {
        String signStr = generateSignature(params, wxPayConfig.getMchKey());
        return signStr.equals(sign);
    }

    /**
     * 解析回调通知
     */
    public Map<String, String> parseCallbackNotify(String xmlData) {
        return parseXmlResponse(xmlData);
    }

    /**
     * 生成回调成功响应
     */
    public String buildSuccessResponse() {
        Map<String, String> data = new HashMap<>();
        data.put("return_code", "SUCCESS");
        data.put("return_msg", "OK");
        return buildXmlRequest(data);
    }

    /**
     * 生成回调失败响应
     */
    public String buildFailResponse(String msg) {
        Map<String, String> data = new HashMap<>();
        data.put("return_code", "FAIL");
        data.put("return_msg", msg);
        return buildXmlRequest(data);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建统一下单参数
     */
    private Map<String, String> buildUnifiedOrderParams(String orderNo, String totalAmount, 
            String description, String tradeType, String openId) {
        Map<String, String> params = buildBaseParams();
        params.put("body", description);
        params.put("out_trade_no", orderNo);
        params.put("total_fee", totalAmount);
        params.put("spbill_create_ip", "127.0.0.1");
        params.put("notify_url", wxPayConfig.getNotifyUrl());
        params.put("trade_type", tradeType);
        if (openId != null) {
            params.put("openid", openId);
        }
        return params;
    }

    /**
     * 构建基础参数
     */
    private Map<String, String> buildBaseParams() {
        Map<String, String> params = new TreeMap<>();
        params.put("appid", wxPayConfig.getAppId());
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("nonce_str", IdUtil.simpleUUID());
        params.put("sign_type", "MD5");
        return params;
    }

    /**
     * 生成签名
     */
    private String generateSignature(Map<String, String> params, String key) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append("&"));
        sb.append("key=").append(key);
        return md5Hex(sb.toString());
    }
    
    /**
     * MD5加密
     */
    private String md5Hex(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    /**
     * 生成JSAPI支付签名
     */
    private String generateJsApiSign(String prepayId) {
        try {
            String appId = wxPayConfig.getAppId();
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonceStr = IdUtil.simpleUUID();
            
            String signStr = String.format("%s\n%s\n%s\n%s\n", appId, timeStamp, nonceStr, prepayId);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(wxPayConfig.getMchKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] result = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("生成JSAPI签名失败", e);
        }
    }

    /**
     * 执行POST请求
     */
    private String executePost(String url, Map<String, String> params) {
        // 生成签名
        String sign = generateSignature(params, wxPayConfig.getMchKey());
        params.put("sign", sign);
        
        // 构建XML请求
        String xmlData = buildXmlRequest(params);
        log.debug("微信支付请求: {}", xmlData);
        
        // 发送请求
        String response = HttpUtil.createPost(url)
                .header("Content-Type", "text/xml; charset=UTF-8")
                .body(xmlData)
                .timeout(30000)
                .execute()
                .body();
        
        log.debug("微信支付响应: {}", response);
        return response;
    }

    /**
     * 执行带证书的POST请求 (退款用)
     */
    private String executePostWithCert(String url, Map<String, String> params, String certPath) {
        // 签名
        String sign = generateSignature(params, wxPayConfig.getMchKey());
        params.put("sign", sign);
        
        String xmlData = buildXmlRequest(params);
        
        // 注意: 使用证书需要KeyStore，这里简化处理
        // 实际生产环境建议使用官方SDK或Apache HttpClient with SSL
        log.warn("退款需要证书配置，当前环境未配置证书");
        return "<xml><return_code>FAIL</return_code><return_msg>证书未配置</return_msg></xml>";
    }

    /**
     * 构建XML请求
     */
    private String buildXmlRequest(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append("<").append(entry.getKey()).append(">");
            sb.append("<![CDATA[").append(entry.getValue()).append("]]>");
            sb.append("</").append(entry.getKey()).append(">");
        }
        sb.append("</xml>");
        return sb.toString();
    }

    /**
     * 解析XML响应
     */
    private Map<String, String> parseXmlResponse(String xml) {
        Map<String, String> result = new HashMap<>();
        try {
            Document doc = XmlUtil.parseXml(xml);
            Element root = doc.getDocumentElement();
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                if (root.getChildNodes().item(i) instanceof org.w3c.dom.Element) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) root.getChildNodes().item(i);
                    result.put(element.getTagName(), element.getTextContent());
                }
            }
        } catch (Exception e) {
            log.error("解析XML失败: {}", xml, e);
            result.put("return_code", "FAIL");
            result.put("return_msg", "XML解析失败");
        }
        return result;
    }
}
