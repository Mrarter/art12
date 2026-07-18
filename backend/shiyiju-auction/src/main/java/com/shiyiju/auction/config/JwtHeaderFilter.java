package com.shiyiju.auction.config;

import com.shiyiju.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/** Only a verified JWT may produce the internal identity header. */
@Slf4j
@Component
public class JwtHeaderFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Long verifiedUserId = null;
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                Claims claims = JwtUtil.parseToken(authorization.substring(7));
                Object raw = claims.get("userId");
                if (raw instanceof Number number) verifiedUserId = number.longValue();
                else if (raw != null) verifiedUserId = Long.valueOf(raw.toString());
            } catch (Exception e) {
                log.debug("忽略无效 JWT: {}", e.getMessage());
            }
        }
        final Long userId = verifiedUserId;
        HttpServletRequestWrapper sanitized = new HttpServletRequestWrapper(request) {
            @Override public String getHeader(String name) {
                return "X-User-Id".equalsIgnoreCase(name) ? (userId == null ? null : userId.toString()) : super.getHeader(name);
            }
            @Override public Enumeration<String> getHeaders(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) {
                    return Collections.enumeration(userId == null ? List.of() : List.of(userId.toString()));
                }
                return super.getHeaders(name);
            }
            @Override public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.removeIf("X-User-Id"::equalsIgnoreCase);
                if (userId != null) names.add("X-User-Id");
                return Collections.enumeration(names);
            }
        };
        chain.doFilter(sanitized, response);
    }
}
