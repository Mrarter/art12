package com.shiyiju.common.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.shiyiju.common.config.AlipayConfig;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 支付宝支付服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    private static final String WAP_PAY_METHOD = "alipay.trade.wap.pay";
    private static final String APP_PAY_METHOD = "alipay.trade.app.pay";
    private static final String REFUND_METHOD = "alipay.trade.refund";
    private static final String CERTIFY_INIT_METHOD = "alipay.user.certify.open.initialize";
    private static final String CERTIFY_QUERY_METHOD = "alipay.user.certify.open.query";
    private static final String CERTIFY_OPEN_METHOD = "alipay.user.certify.open.certify";

    private final AlipayConfig alipayConfig;

    public Map<String, Object> createWapPay(String orderNo, BigDecimal amountYuan, String subject) {
        return createWapPay(orderNo, amountYuan, subject, normalizeAppUrl(alipayConfig.getReturnUrl()));
    }

    public Map<String, Object> createWapPay(String orderNo, BigDecimal amountYuan, String subject, String returnUrl) {
        ensureEnabled();

        Map<String, Object> bizContent = new TreeMap<>();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("total_amount", amountYuan.setScale(2).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "QUICK_WAP_WAY");

        Map<String, String> params = new TreeMap<>();
        params.put("app_id", alipayConfig.getAppId());
        params.put("method", WAP_PAY_METHOD);
        params.put("charset", alipayConfig.getCharset());
        params.put("sign_type", alipayConfig.getSignType());
        params.put("timestamp", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("notify_url", normalizeAppUrl(alipayConfig.getNotifyUrl()));
        if (hasText(returnUrl)) {
            params.put("return_url", normalizeAppUrl(returnUrl));
        }
        params.put("biz_content", JSON.toJSONString(bizContent));
        params.put("sign", sign(params));

        String payForm = buildAutoSubmitForm(alipayConfig.getGatewayUrl(), params);
        String payUrl = buildRedirectUrl(alipayConfig.getGatewayUrl(), params);
        return Map.of(
                "provider", "alipay",
                "order_no", orderNo,
                "pay_amount", amountYuan,
                "pay_form", payForm,
                "pay_url", payUrl
        );
    }

    public Map<String, Object> createAppPay(String orderNo, BigDecimal amountYuan, String subject) {
        ensureEnabled();

        Map<String, Object> bizContent = new TreeMap<>();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("total_amount", amountYuan.setScale(2).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");

        Map<String, String> params = buildSignedParams(APP_PAY_METHOD, bizContent, null);
        params.put("notify_url", normalizeAppUrl(alipayConfig.getNotifyUrl()));
        params.put("sign", sign(params));

        String orderString = params.entrySet().stream()
                .filter(entry -> hasText(entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + encodeQueryValue(entry.getValue()))
                .collect(Collectors.joining("&"));

        return Map.of(
                "provider", "alipay",
                "order_no", orderNo,
                "pay_amount", amountYuan,
                "order_string", orderString,
                "orderInfo", orderString
        );
    }

    public Map<String, String> refund(String outTradeNo, String outRequestNo, BigDecimal refundAmountYuan, String reason) {
        ensureEnabled();

        Map<String, Object> bizContent = new TreeMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("refund_amount", refundAmountYuan.setScale(2).toPlainString());
        bizContent.put("out_request_no", outRequestNo);
        if (hasText(reason)) {
            bizContent.put("refund_reason", reason);
        }

        JSONObject response = executeOpenApi(REFUND_METHOD, bizContent, null);
        JSONObject result = response.getJSONObject("alipay_trade_refund_response");
        if (result == null) {
            throw new IllegalStateException("支付宝退款失败：响应缺少结果");
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("code", result.getString("code"));
        data.put("msg", result.getString("msg"));
        data.put("subCode", result.getString("sub_code"));
        data.put("subMsg", result.getString("sub_msg"));
        data.put("tradeNo", result.getString("trade_no"));
        data.put("outTradeNo", result.getString("out_trade_no"));
        data.put("buyerLogonId", result.getString("buyer_logon_id"));
        data.put("fundChange", result.getString("fund_change"));
        data.put("refundFee", result.getString("refund_fee"));

        if (!"10000".equals(data.get("code"))) {
            throw new IllegalStateException(firstNonBlank(data.get("subMsg"), data.get("msg"), "支付宝退款失败"));
        }
        return data;
    }

    public boolean verifyNotify(Map<String, String> notifyParams) {
        try {
            String sign = notifyParams.get("sign");
            if (!hasText(sign)) {
                return false;
            }
            String content = buildSignContent(notifyParams, true);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(readPublicKey(alipayConfig.getAlipayPublicKey()));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.warn("支付宝回调验签失败", e);
            return false;
        }
    }

    public boolean isRealnameEnabled() {
        return alipayConfig.isRealnameEnabled();
    }

    public String getRealnameReturnUrl() {
        return normalizeAppUrl(alipayConfig.getRealnameReturnUrl());
    }

    public Map<String, String> initializeRealnameCert(String outerOrderNo, String realName, String idCard, String returnUrl) {
        ensureRealnameEnabled();

        Map<String, Object> identityParam = new LinkedHashMap<>();
        identityParam.put("identity_type", "CERT_INFO");
        identityParam.put("cert_type", "IDENTITY_CARD");
        identityParam.put("cert_name", realName);
        identityParam.put("cert_no", idCard);

        Map<String, Object> merchantConfig = new LinkedHashMap<>();
        if (hasText(returnUrl)) {
            merchantConfig.put("return_url", returnUrl);
        }

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("outer_order_no", outerOrderNo);
        bizContent.put("biz_code", alipayConfig.getRealnameBizCode());
        bizContent.put("identity_param", identityParam);
        if (!merchantConfig.isEmpty()) {
            bizContent.put("merchant_config", merchantConfig);
        }

        JSONObject response = executeOpenApi(CERTIFY_INIT_METHOD, bizContent, null);
        JSONObject result = response.getJSONObject("alipay_user_certify_open_initialize_response");
        if (result == null) {
            throw new IllegalStateException("支付宝实名认证初始化失败：响应缺少结果");
        }
        String code = result.getString("code");
        if (!"10000".equals(code)) {
            throw new IllegalStateException(firstNonBlank(result.getString("sub_msg"), result.getString("msg"), "支付宝实名认证初始化失败"));
        }
        String certifyId = result.getString("certify_id");
        if (!hasText(certifyId)) {
            throw new IllegalStateException("支付宝实名认证初始化失败：未返回 certify_id");
        }
        return Map.of(
                "certifyId", certifyId,
                "outerOrderNo", outerOrderNo
        );
    }

    public String buildRealnameCertifyUrl(String certifyId, String returnUrl) {
        ensureRealnameEnabled();

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("certify_id", certifyId);

        Map<String, String> params = buildSignedParams(CERTIFY_OPEN_METHOD, bizContent, returnUrl);
        return alipayConfig.getGatewayUrl() + "?" + params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + encodeQueryValue(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    public Map<String, String> queryRealnameCert(String certifyId) {
        ensureRealnameEnabled();

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("certify_id", certifyId);
        JSONObject response = executeOpenApi(CERTIFY_QUERY_METHOD, bizContent, null);
        JSONObject result = response.getJSONObject("alipay_user_certify_open_query_response");
        if (result == null) {
            throw new IllegalStateException("支付宝实名认证查询失败：响应缺少结果");
        }
        String code = result.getString("code");
        if (!"10000".equals(code)) {
            throw new IllegalStateException(firstNonBlank(result.getString("sub_msg"), result.getString("msg"), "支付宝实名认证查询失败"));
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("passed", String.valueOf(isPassedResult(result.get("passed"))));
        data.put("certifyStatus", firstNonBlank(result.getString("certify_status"), result.getString("status"), ""));
        data.put("failReason", firstNonBlank(result.getString("fail_reason"), result.getString("sub_msg"), ""));
        return data;
    }

    private void ensureEnabled() {
        if (!alipayConfig.isEnabled()) {
            throw new IllegalStateException("支付宝支付未启用，请配置 ALIPAY_ENABLED=true");
        }
        if (!hasText(alipayConfig.getAppId())
                || !hasText(alipayConfig.getPrivateKey())
                || !hasText(alipayConfig.getAlipayPublicKey())
                || !hasText(alipayConfig.getNotifyUrl())) {
            throw new IllegalStateException("支付宝配置不完整，请检查 APP_ID、应用私钥、支付宝公钥和回调地址");
        }
    }

    private void ensureRealnameEnabled() {
        if (!alipayConfig.isRealnameEnabled()) {
            throw new IllegalStateException("支付宝实名认证未启用，请配置 ALIPAY_REALNAME_ENABLED=true");
        }
        if (!hasText(alipayConfig.getRealnameReturnUrl())) {
            throw new IllegalStateException("支付宝实名认证回跳地址未配置，请检查 ALIPAY_REALNAME_RETURN_URL");
        }
        if (!hasText(alipayConfig.getAppId())
                || !hasText(alipayConfig.getPrivateKey())
                || !hasText(alipayConfig.getAlipayPublicKey())) {
            throw new IllegalStateException("支付宝实名认证配置不完整，请检查 APP_ID、应用私钥和支付宝公钥");
        }
    }

    private String normalizeAppUrl(String value) {
        if (!hasText(value)) {
            return value;
        }
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            if (host == null || (!"a.art1.cn".equalsIgnoreCase(host)
                    && !"art1.cn".equalsIgnoreCase(host)
                    && !"www.art1.cn".equalsIgnoreCase(host))) {
                return value;
            }
            return new URI(uri.getScheme(), uri.getUserInfo(), "a.art1.cn", uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (Exception ex) {
            log.warn("支付宝回调地址格式不正确，保留原地址: {}", value);
            return value;
        }
    }

    private JSONObject executeOpenApi(String method, Map<String, Object> bizContent, String returnUrl) {
        Map<String, String> params = buildSignedParams(method, bizContent, returnUrl);
        Map<String, Object> formParams = new LinkedHashMap<>(params);
        try (HttpResponse response = HttpRequest.post(alipayConfig.getGatewayUrl())
                .form(formParams)
                .timeout(10000)
                .execute()) {
            String body = response.body();
            if (!response.isOk()) {
                throw new IllegalStateException("支付宝接口调用失败: HTTP " + response.getStatus());
            }
            return JSON.parseObject(body);
        } catch (Exception e) {
            throw new IllegalStateException("支付宝接口调用失败", e);
        }
    }

    private Map<String, String> buildSignedParams(String method, Map<String, Object> bizContent, String returnUrl) {
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", alipayConfig.getAppId());
        params.put("method", method);
        params.put("charset", alipayConfig.getCharset());
        params.put("sign_type", alipayConfig.getSignType());
        params.put("timestamp", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        if (hasText(returnUrl)) {
            params.put("return_url", returnUrl);
        }
        params.put("biz_content", JSON.toJSONString(bizContent));
        params.put("sign", sign(params));
        return params;
    }

    private String sign(Map<String, String> params) {
        try {
            String content = buildSignContent(params, false);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(readPrivateKey(alipayConfig.getPrivateKey()));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付宝签名失败", e);
        }
    }

    private String buildSignContent(Map<String, String> params, boolean excludeAlipaySignFields) {
        return params.entrySet().stream()
                .filter(entry -> hasText(entry.getValue()))
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !excludeAlipaySignFields || !"sign_type".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String buildAutoSubmitForm(String gatewayUrl, Map<String, String> params) {
        String inputs = params.entrySet().stream()
                .map(entry -> "<input type=\"hidden\" name=\"" + htmlEscape(entry.getKey())
                        + "\" value=\"" + htmlEscape(entry.getValue()) + "\" />")
                .collect(Collectors.joining("\n"));
        return "<form id=\"alipay_submit\" name=\"alipay_submit\" action=\"" + htmlEscape(gatewayUrl)
                + "\" method=\"POST\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\">\n" + inputs + "\n</form>"
                + "<script>document.forms['alipay_submit'].submit();</script>";
    }

    private String buildRedirectUrl(String gatewayUrl, Map<String, String> params) {
        String query = params.entrySet().stream()
                .filter(entry -> hasText(entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + encodeQueryValue(entry.getValue()))
                .collect(Collectors.joining("&"));
        return gatewayUrl + "?" + query;
    }

    private PrivateKey readPrivateKey(String key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(stripPem(key));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private java.security.PublicKey readPublicKey(String key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(stripPem(key));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private String stripPem(String key) {
        return key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private String htmlEscape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private boolean isPassedResult(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text)
                || "t".equalsIgnoreCase(text)
                || "y".equalsIgnoreCase(text)
                || "yes".equalsIgnoreCase(text)
                || "success".equalsIgnoreCase(text);
    }
}
