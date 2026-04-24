package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.SystemConfigPersistenceService;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 系统配置控制器
 */
@RestController
@RequestMapping("/admin/config")
public class SystemConfigController {

    private final SystemConfigPersistenceService systemConfigPersistenceService;

    public SystemConfigController(SystemConfigPersistenceService systemConfigPersistenceService) {
        this.systemConfigPersistenceService = systemConfigPersistenceService;
    }

    /**
     * 获取配置分组列表
     */
    @GetMapping("/groups")
    public Result<List<Map<String, Object>>> getConfigGroups() {
        return Result.success(systemConfigPersistenceService.getConfigGroups());
    }

    /**
     * 获取配置列表（按分组）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getConfigs(@RequestParam(required = false) String group) {
        return Result.success(systemConfigPersistenceService.getConfigs(group));
    }

    /**
     * 获取整页配置
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAllConfig() {
        return Result.success(systemConfigPersistenceService.getAllConfig());
    }

    /**
     * 更新配置
     */
    @PostMapping("/update")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> params) {
        systemConfigPersistenceService.updateAllConfig(params);
        return Result.success();
    }
}
