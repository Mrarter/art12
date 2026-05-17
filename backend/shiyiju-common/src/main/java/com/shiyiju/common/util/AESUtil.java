package com.shiyiju.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密/解密工具（用于银行卡号加密存储）
 * 密钥通过 {@link #setKey(String)} 动态注入，启动时由配置中心注入
 */
@Slf4j
public class AESUtil {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static String KEY = "ShiyijuPay2026!!"; // 默认开发密钥，生产环境通过配置注入

    /**
     * 动态设置密钥（由 Spring 启动时通过配置注入）
     */
    public static void setKey(String key) {
        if (key != null && !key.isEmpty() && key.length() == 16) {
            KEY = key;
            log.info("AES 密钥已通过配置注入");
        }
    }

    private static SecretKeySpec getKey() {
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * AES 加密
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("加密失败");
        }
    }

    /**
     * AES 解密
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("解密失败");
        }
    }

    /**
     * 脱敏银行卡号：622202******1234
     */
    public static String maskBankCard(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) return cardNo;
        return cardNo.substring(0, 6) + "******" + cardNo.substring(cardNo.length() - 4);
    }

    /**
     * 脱敏身份证号：410***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 脱敏手机号：138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
