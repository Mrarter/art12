package com.shiyiju.admin.config;

import com.shiyiju.admin.service.AdminAccountService;
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

    private final AdminAccountService adminAccountService;

    public ApiForwardFilter(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();

        boolean adminApi = path.startsWith("/api/admin/") || path.startsWith("/admin/");
        boolean loginRequest = path.equals("/api/admin/login") || path.equals("/admin/login");
        if (adminApi && !loginRequest && !"OPTIONS".equalsIgnoreCase(req.getMethod())
                && !adminAccountService.isValidSession(req.getHeader("Authorization"))) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"登录已失效，请重新登录\",\"data\":null}");
            return;
        }

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
