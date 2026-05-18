package com.shiyiju.gateway.config;

import com.shiyiju.common.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GatewayJwtConfig {

    @Value("${jwt.secret:shiyiju-secret-key-2026-very-long-and-secure-key-for-jwt-signing}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        log.info("网关 JWT 密钥初始化: prefix={}...", jwtSecret.substring(0, Math.min(20, jwtSecret.length())));
        JwtUtil.setSecret(jwtSecret);
    }
}
