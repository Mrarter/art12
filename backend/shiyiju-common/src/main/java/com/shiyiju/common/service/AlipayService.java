package com.shiyiju.common.service;

import com.alibaba.fastjson2.JSON;
import com.shiyiju.common.config.AlipayConfig;
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
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 支付宝手机网站支付服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    private static final String WAP_PAY_METHOD = "alipay.trade.wap.pay";

    private final AlipayConfig alipayConfig;

    public Map<String, Object> createWapPay(String orderNo, BigDecimal amountYuan, String subject) {
        ensureEnabled();

        Map<String, String> bizContent = new TreeMap<>();
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
        params.put("notify_url", alipayConfig.getNotifyUrl());
        if (hasText(alipayConfig.getReturnUrl())) {
            params.put("return_url", alipayConfig.getReturnUrl());
        }
        params.put("biz_content", JSON.toJSONString(bizContent));
        params.put("sign", sign(params));

        String payForm = buildAutoSubmitForm(alipayConfig.getGatewayUrl(), params);
        return Map.of(
                "provider", "alipay",
                "order_no", orderNo,
                "pay_amount", amountYuan,
                "pay_form", payForm
        );
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
                + "\" method=\"POST\">\n" + inputs + "\n</form>"
                + "<script>document.forms['alipay_submit'].submit();</script>";
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
