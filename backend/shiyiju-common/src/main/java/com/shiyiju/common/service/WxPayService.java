package com.shiyiju.common.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.http.HttpUtil;
import com.shiyiju.common.config.WxPayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
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
        Map<String, String> params = buildUnifiedOrderParams(orderNo, totalAmount, description, "NATIVE", null, null);
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
     * 统一下单 (JSAPI支付 - 返回小程序调起支付所需的全部参数)
     * 适用于微信小程序和公众号支付
     */
    public Map<String, String> unifiedOrderJsApi(String orderNo, String totalAmount, String openId, String description, String payScene) {
        Map<String, String> params = buildUnifiedOrderParams(orderNo, totalAmount, description, "JSAPI", openId, payScene);
        String response = executePost(UNIFIED_ORDER_URL, params);
        Map<String, String> result = parseXmlResponse(response);
        
        log.info("微信JSAPI支付统一下单结果: {}", result);
        
        if ("SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"))) {
            String prepayId = result.get("prepay_id");
            return buildMiniProgramPayParams(prepayId, payScene);
        } else {
            throw new RuntimeException("微信JSAPI支付下单失败: " + result.get("err_code_des"));
        }
    }

    /**
     * 统一下单 (APP支付 - 返回 App SDK 调起参数)
     */
    public Map<String, String> unifiedOrderApp(String orderNo, String totalAmount, String description) {
        Map<String, String> params = buildUnifiedOrderParams(orderNo, totalAmount, description, "APP", null, "app");
        String response = executePost(UNIFIED_ORDER_URL, params);
        Map<String, String> result = parseXmlResponse(response);

        log.info("微信APP支付统一下单结果: {}", result);

        if ("SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"))) {
            String prepayId = result.get("prepay_id");
            return buildAppPayParams(prepayId);
        } else {
            throw new RuntimeException("微信APP支付下单失败: " + result.get("err_code_des"));
        }
    }

    /**
     * 构建小程序调起支付所需的参数
     * 返回前端 wx.requestPayment 需要的 5 个参数
     */
    private Map<String, String> buildMiniProgramPayParams(String prepayId, String payScene) {
        String appId = resolveAppId("JSAPI", payScene);
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = IdUtil.simpleUUID();
        String packageStr = "prepay_id=" + prepayId;
        
        // 按照微信文档签名：appId, nonceStr, package, signType, timeStamp
        Map<String, String> signParams = new TreeMap<>();
        signParams.put("appId", appId);
        signParams.put("nonceStr", nonceStr);
        signParams.put("package", packageStr);
        signParams.put("signType", "MD5");
        signParams.put("timeStamp", timeStamp);
        String paySign = generateSignature(signParams, resolveV2Key());
        
        Map<String, String> payResult = new HashMap<>();
        payResult.put("appId", appId);
        payResult.put("timeStamp", timeStamp);
        payResult.put("nonceStr", nonceStr);
        payResult.put("package", packageStr);
        payResult.put("signType", "MD5");
        payResult.put("paySign", paySign);
        payResult.put("prepay_id", prepayId);
        return payResult;
    }

    /**
     * 构建 App 调起支付所需的参数
     */
    private Map<String, String> buildAppPayParams(String prepayId) {
        String appId = resolveAppId("APP", "app");
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = IdUtil.simpleUUID();
        String packageStr = "Sign=WXPay";

        Map<String, String> signParams = new TreeMap<>();
        signParams.put("appid", appId);
        signParams.put("noncestr", nonceStr);
        signParams.put("package", packageStr);
        signParams.put("partnerid", wxPayConfig.getMchId());
        signParams.put("prepayid", prepayId);
        signParams.put("timestamp", timeStamp);
        String paySign = generateSignature(signParams, resolveV2Key());

        Map<String, String> payResult = new HashMap<>();
        payResult.put("appid", appId);
        payResult.put("partnerid", wxPayConfig.getMchId());
        payResult.put("prepayid", prepayId);
        payResult.put("package", packageStr);
        payResult.put("noncestr", nonceStr);
        payResult.put("timestamp", timeStamp);
        payResult.put("sign", paySign);
        return payResult;
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
        Map<String, String> result = refundWithResult(orderNo, refundNo, totalAmount, refundAmount, "用户申请退款");
        return "SUCCESS".equals(result.get("return_code")) && "SUCCESS".equals(result.get("result_code"));
    }

    public Map<String, String> refundWithResult(String orderNo, String refundNo, String totalAmount,
            String refundAmount, String reason) {
        Map<String, String> params = buildBaseParams();
        params.put("out_trade_no", orderNo);
        params.put("out_refund_no", refundNo);
        params.put("total_fee", totalAmount);
        params.put("refund_fee", refundAmount);
        params.put("refund_desc", reason == null || reason.isBlank() ? "用户申请退款" : reason);
        
        String response = executePostWithCert(REFUND_URL, params, wxPayConfig.getKeyPath());
        return parseXmlResponse(response);
    }

    /**
     * 验证支付回调签名
     */
    public boolean verifyCallbackSign(Map<String, String> params, String sign) {
        String signStr = generateSignature(params, resolveV2Key());
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
            String description, String tradeType, String openId, String payScene) {
        Map<String, String> params = buildBaseParams(tradeType, payScene);
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
        return buildBaseParams(null, null);
    }

    private Map<String, String> buildBaseParams(String tradeType) {
        return buildBaseParams(tradeType, null);
    }

    private Map<String, String> buildBaseParams(String tradeType, String payScene) {
        Map<String, String> params = new TreeMap<>();
        params.put("appid", resolveAppId(tradeType, payScene));
        params.put("mch_id", wxPayConfig.getMchId());
        params.put("nonce_str", IdUtil.simpleUUID());
        params.put("sign_type", "MD5");
        return params;
    }

    private String resolveAppId(String tradeType, String payScene) {
        if ("JSAPI".equalsIgnoreCase(tradeType)) {
            if ("h5".equalsIgnoreCase(payScene) || "official".equalsIgnoreCase(payScene)) {
                String officialAppId = wxPayConfig.getOfficialAppId();
                if (officialAppId != null && !officialAppId.isBlank()) {
                    return officialAppId;
                }
            }
            String miniAppId = wxPayConfig.getMiniAppId();
            if (miniAppId != null && !miniAppId.isBlank()) {
                return miniAppId;
            }
        }
        if ("APP".equalsIgnoreCase(tradeType)) {
            return wxPayConfig.getAppId();
        }
        return wxPayConfig.getAppId();
    }

    private String resolveV2Key() {
        String apiKey = wxPayConfig.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }
        return wxPayConfig.getMchKey();
    }

    /**
     * 生成签名
     */
    private String generateSignature(Map<String, String> params, String key) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !"sign".equalsIgnoreCase(e.getKey()))
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
     * 执行POST请求
     */
    private String executePost(String url, Map<String, String> params) {
        // 生成签名
        String sign = generateSignature(params, resolveV2Key());
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
        if (certPath == null || certPath.isBlank()) {
            throw new IllegalStateException("微信退款证书未配置，请设置 WXPAY_KEY_PATH");
        }
        File certFile = new File(certPath);
        if (!certFile.isFile()) {
            throw new IllegalStateException("微信退款证书不存在或不可读: " + certPath);
        }
        String sign = generateSignature(params, resolveV2Key());
        params.put("sign", sign);
        String xmlData = buildXmlRequest(params);

        try (FileInputStream inputStream = new FileInputStream(certFile)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            char[] password = wxPayConfig.getMchId().toCharArray();
            keyStore.load(inputStream, password);

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, password)
                    .build();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{"TLSv1.2"},
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "text/xml; charset=UTF-8");
                httpPost.setEntity(new StringEntity(xmlData, StandardCharsets.UTF_8));
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    log.debug("微信退款响应: {}", body);
                    return body;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("微信退款请求失败: " + e.getMessage(), e);
        }
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
