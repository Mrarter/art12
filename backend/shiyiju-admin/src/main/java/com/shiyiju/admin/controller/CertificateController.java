package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.CertificateService;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 收藏证书管理
 */
@RestController
@RequestMapping("/admin/certificate")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(certificateService.getList(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        Map<String, Object> cert = certificateService.getById(id);
        if (cert == null) return Result.fail("证书不存在");
        return Result.success(cert);
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> params) {
        return Result.success(certificateService.generate(params));
    }
}
