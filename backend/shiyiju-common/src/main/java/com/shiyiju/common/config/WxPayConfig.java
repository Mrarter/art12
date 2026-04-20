package com.shiyiju.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wxpay")
public class WxPayConfig {
    
    /** 微信公众号AppID */
    private String appId;
    
    /** 商户号 */
    private String mchId;
    
    /** 商户密钥 */
    private String mchKey;
    
    /** 证书路径 (退款等需要证书的操作) */
    private String keyPath;
    
    /** 回调地址 */
    private String notifyUrl;
    
    /** 交易类型: JSAPI/NATIVE/APP */
    private String tradeType;
    
    /** 微信支付API版本: v2/v3 */
    private String version;
    
    /** 微信支付API密钥 (v2) */
    private String apiKey;
    
    /** 微信支付证书序列号 (v3) */
    private String serialNo;
    
    /** 小程序AppID (用于JSAPI支付获取openid) */
    private String miniAppId;
}
