package com.shiyiju.auction.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Admin mutations are closed unless an explicit production key is configured. */
@Order(1)
@Component
public class AuctionAdminGuardFilter implements Filter {
    @Value("${auction.admin-api-key:}") private String adminApiKey;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if (!request.getRequestURI().startsWith("/auction/admin/")) {
            chain.doFilter(req, res); return;
        }
        String supplied = request.getHeader("X-Auction-Admin-Key");
        boolean valid = adminApiKey != null && !adminApiKey.isBlank() && supplied != null
                && MessageDigest.isEqual(adminApiKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"无权访问拍卖管理接口\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
