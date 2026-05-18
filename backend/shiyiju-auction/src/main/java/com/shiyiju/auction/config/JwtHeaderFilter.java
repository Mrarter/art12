package com.shiyiju.auction.config;

import com.shiyiju.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Servlet 过滤器 - 从 JWT Token 中提取用户 ID 并注入 X-User-Id 请求头
 * 
 * 在 Spring 处理请求之前运行，通过包装 HttpServletRequest 添加头信息
 */
@Slf4j
@Component
@Order(1)
public class JwtHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        
        // 已有 X-User-Id 头（GET 请求成功透传），直接放行
        if (request.getHeader("X-User-Id") != null) {
            chain.doFilter(request, servletResponse);
            return;
        }
        
        // 从 Authorization 头解析 JWT Token 获取 userId
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (JwtUtil.validateToken(token)) {
                    Claims claims = JwtUtil.parseToken(token);
                    Long userId = claims.get("userId", Long.class);
                    if (userId != null) {
                        // 通过包装请求动态添加 X-User-Id 头
                        String finalUserId = String.valueOf(userId);
                        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
                            @Override
                            public String getHeader(String name) {
                                if ("X-User-Id".equalsIgnoreCase(name)) {
                                    return finalUserId;
                                }
                                return super.getHeader(name);
                            }

                            @Override
                            public Enumeration<String> getHeaders(String name) {
                                if ("X-User-Id".equalsIgnoreCase(name)) {
                                    return Collections.enumeration(Collections.singleton(finalUserId));
                                }
                                return super.getHeaders(name);
                            }

                            @Override
                            public Enumeration<String> getHeaderNames() {
                                List<String> names = Collections.list(super.getHeaderNames());
                                names.add("X-User-Id");
                                return Collections.enumeration(names);
                            }
                        };
                        chain.doFilter(wrapper, servletResponse);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("JWT 解析失败: {}", e.getMessage());
            }
        }
        
        chain.doFilter(request, servletResponse);
    }
}
