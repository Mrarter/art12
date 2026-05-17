package com.shiyiju.common.config;

import com.shiyiju.common.util.AESUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES 加密配置初始化器 - 启动时将密钥注入 AESUtil
 */
@Slf4j
@Component
public class AESConfig {

    @Value("${aes.secret:ShiyijuPay2026!!}")
    private String secret;

    @PostConstruct
    public void init() {
        AESUtil.setKey(secret);
    }
}
