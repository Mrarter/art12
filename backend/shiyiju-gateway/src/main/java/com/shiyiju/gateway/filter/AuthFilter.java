package com.shiyiju.gateway.filter;

import com.shiyiju.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤器
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /** 无需认证的路径 */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/auth/wx-login",
            "/admin/login",
            "/admin/info",
            "/product/categories",
            "/product/artwork/list",
            "/product/artwork/detail",
            "/product/homepage/banners",
            "/product/list",
            "/product/search",
            "/product/banners",
            "/product/update",
            "/product/create",
            "/product/delete",
            "/auction/session/list",
            "/auction/session/detail",
            "/auction/lot/detail",
            "/community/post/list",
            "/community/post/detail",
            "/community/topic/list",
            "/file/upload",
            "/health",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 去掉 /api 前缀（Gateway路由StripPrefix在后置过滤器中处理）
        String internalPath = path;
        if (path.startsWith("/api/")) {
            internalPath = path.substring(4);
        }
        
        // 白名单路径直接放行
        if (isWhiteList(internalPath)) {
            return chain.filter(exchange);
        }

        // 获取 Token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证 Token
        if (token != null && JwtUtil.validateToken(token)) {
            try {
                Claims claims = JwtUtil.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                
                // 将用户ID添加到请求头
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", String.valueOf(userId))
                        .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                log.warn("Token 解析失败: {}", e.getMessage());
            }
        }

        // 返回未认证
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
