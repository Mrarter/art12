package com.shiyiju.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /** 是否启用支付宝真实下单。 */
    private boolean enabled = false;

    /** 支付宝开放平台应用 APP_ID。 */
    private String appId;

    /** 应用私钥，PKCS8 格式，支持单行或 PEM 格式。 */
    private String privateKey;

    /** 支付宝公钥，用于异步通知验签。 */
    private String alipayPublicKey;

    /** 支付宝网关地址。 */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    /** 异步通知地址，必须为外网可访问 HTTPS 地址。 */
    private String notifyUrl;

    /** 前端支付完成后跳转地址。 */
    private String returnUrl;

    /** 签名算法。 */
    private String signType = "RSA2";

    /** 字符集。 */
    private String charset = "UTF-8";

    /** 是否启用支付宝实名认证。 */
    private boolean realnameEnabled = false;

    /** 支付宝实名认证完成后的前端回跳地址。 */
    private String realnameReturnUrl;

    /** 实名认证业务码，默认人脸实人认证。 */
    private String realnameBizCode = "FACE";
}
