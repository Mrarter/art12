package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.AdminAccountService;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/system/admin")
public class SystemAdminController {

    private final AdminAccountService adminAccountService;

    public SystemAdminController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(adminAccountService.listAdmins());
    }

    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return Result.success(adminAccountService.createAdmin(params));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        adminAccountService.updateAdmin(id, params);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminAccountService.deleteAdmin(id);
        return Result.success();
    }
}
