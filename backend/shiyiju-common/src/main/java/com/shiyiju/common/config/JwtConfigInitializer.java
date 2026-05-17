package com.shiyiju.common.config;

import com.shiyiju.common.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 配置初始化器 - 启动时将 JWT 密钥注入 JwtUtil 静态工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtConfigInitializer {

    private final JwtConfig jwtConfig;

    @PostConstruct
    public void init() {
        JwtUtil.setSecret(jwtConfig.getSecret());
    }
}
