package com.shiyiju.gateway.filter;

import com.shiyiju.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
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

    public static final String USER_ID_ATTR = "gateway.userId";

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login", "/user/auth/wx-login", "/user/wxlogin",
            "/admin/login", "/admin/info", "/admin/dashboard", "/admin/",
            "/admin/product/list", "/admin/product/categories", "/admin/product/audit/list",
            "/admin/order/list", "/admin/order/aftersale/list",
            "/admin/user/artist/list", "/admin/user/promoter/list",
            "/admin/community/topic/list", "/admin/community/comment/list", "/admin/community/post/list",
            "/admin/auction/session/list", "/admin/auction/lot/list", "/admin/auction/record/list",
            "/admin/auction/admin/stats", "/admin/promotion/commission/list",
            "/admin/promotion/withdraw/list", "/admin/promotion/admin/stats",
            "/admin/system/admin/list", "/admin/system/banner/list", "/admin/system/operation-log/list",
            "/admin/message/list", "/admin/message/template/list",
            "/product/categories", "/product/artwork/list", "/product/artwork/detail",
            "/product/homepage/banners", "/product/list", "/product/search",
            "/product/banners", "/product/recommend", "/product/following",
            "/product/update", "/product/create", "/product/delete", "/product/audit/list",
            "/product/upload", "/product/",
            "/artist/score/", "/artist/score", "/artist/identity/", "/artist/identity",
            "/admin/artist/score/", "/admin/artist/score", "/admin/artist/identity/", "/admin/artist/identity",
            "/config/priceGrowth",
            "/order/list", "/order/aftersale/list",
            "/user/artist/list", "/user/artist/search", "/user/artist/",
            "/user/promoter/list",
            "/auction/session/list", "/auction/session/detail", "/auction/sessions",
            "/auction/sessions/", "/auction/lot/", "/auction/lots/",
            "/auction/lot/list", "/auction/lot/detail", "/auction/record/list",
            "/auction/admin/stats", "/auction/reminders",
            "/promotion/commission/list", "/promotion/withdraw/list",
            "/promotion/admin/stats", "/promotion/product-commission",
            "/promoter/",
            "/system/admin/list", "/system/banner/list", "/system/operation-log/list",
            "/community/post/list", "/community/post/detail", "/community/topic/list",
            "/community/comment/list", "/message/list", "/message/template/list",
            "/file/upload", "/pay/callback/notify", "/health", "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String internalPath = path.startsWith("/api/") ? path.substring(4) : path;

        if (isWhiteList(internalPath)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (token != null && JwtUtil.validateToken(token)) {
                try {
                    Claims claims = JwtUtil.parseToken(token);
                    Long userId = claims.get("userId", Long.class);
                    exchange.getAttributes().put(USER_ID_ATTR, userId);
                    return chain.filter(exchange);
                } catch (Exception e) {
                    log.warn("Token 解析失败: {}", e.getMessage());
                }
            }
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
