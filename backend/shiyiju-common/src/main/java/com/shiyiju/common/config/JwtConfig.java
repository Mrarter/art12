package com.shiyiju.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（从 application.yml 读取 jwt.secret）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /** JWT 签名密钥 */
    private String secret = "shiyiju-secret-key-2026-very-long-and-secure-key-for-jwt-signing";
}
