package com.shiyiju.admin.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API路径转发Filter - 将 /api/admin/** 路径请求转发到 /admin/**
 */
@Component
@Order(1)
public class ApiForwardFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // 处理 /api/admin/** 路径 -> 转发到 /admin/**
        if (path.startsWith("/api/admin/")) {
            String newPath = path.replaceFirst("^/api/admin", "/admin");
            req.getRequestDispatcher(newPath).forward(req, resp);
            return;
        }

        // 处理 /api/admin 路径（无尾部斜杠）
        if (path.equals("/api/admin")) {
            resp.sendRedirect("/admin");
            return;
        }

        chain.doFilter(request, response);
    }
}
