package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.AdminAccountService;
import com.shiyiju.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API前缀控制器 - 处理 /api/admin/** 路径请求
 * 前端baseURL为 /api/admin，需要转换为 /admin
 */
@RestController
@RequestMapping("/api/admin")
public class ApiPrefixController {

    @Autowired
    private AdminAccountService adminAccountService;

    /**
     * 管理员登录 - /api/admin/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        Map<String, Object> data = adminAccountService.login(username, password);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail("用户名或密码错误");
    }

    /**
     * 获取管理员信息 - /api/admin/info
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getAdminInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> admin = adminAccountService.getAdminInfo(token);
        return admin == null ? Result.fail("登录已失效") : Result.success(admin);
    }
}
