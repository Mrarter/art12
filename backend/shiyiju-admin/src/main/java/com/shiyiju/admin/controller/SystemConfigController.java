package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.SystemConfigPersistenceService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 系统配置控制器
 */
@RestController
@RequestMapping({"/admin/config", "/api/admin/config"})
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

    /**
     * 获取价格增长配置
     */
    @GetMapping("/priceGrowth")
    public Result<Map<String, Object>> getPriceGrowthConfig() {
        return Result.success(systemConfigPersistenceService.getPriceGrowthConfig());
    }

    /**
     * 获取平台抽佣财务统计
     */
    @GetMapping("/platformCommission/finance")
    public Result<Map<String, Object>> getPlatformCommissionFinance() {
        return Result.success(systemConfigPersistenceService.getPlatformCommissionFinance());
    }

    /**
     * 获取平台抽佣流水
     */
    @GetMapping("/platformCommission/flows")
    public Result<PageResult<Map<String, Object>>> getPlatformCommissionFlows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(systemConfigPersistenceService.getPlatformCommissionFlows(page, size, keyword));
    }

    /**
     * 获取平台抽佣流水详情
     */
    @GetMapping("/platformCommission/flows/{billId}")
    public Result<Map<String, Object>> getPlatformCommissionFlowDetail(@PathVariable Long billId) {
        return Result.success(systemConfigPersistenceService.getPlatformCommissionFlowDetail(billId));
    }

    /**
     * 更新价格增长配置
     */
    @PostMapping("/priceGrowth")
    public Result<Void> updatePriceGrowthConfig(@RequestBody Map<String, Object> params) {
        systemConfigPersistenceService.updatePriceGrowthConfig(params);
        return Result.success();
    }
}
