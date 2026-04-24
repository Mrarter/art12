package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.PromotionService;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 - 分销管理控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin/promotion")
public class PromotionAdminController {

    private static final Logger log = LoggerFactory.getLogger(PromotionAdminController.class);

    @Autowired
    private PromotionService promotionService;

    /**
     * 获取分销配置（真实查询）
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> config = promotionService.getConfig();
        return Result.success(config);
    }

    /**
     * 更新分销配置（真保存）
     */
    @PostMapping("/config")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> params) {
        try {
            if (params.get("directRate") != null) {
                promotionService.updateConfig("commission.direct.rate", String.valueOf(params.get("directRate")));
            }
            if (params.get("teamRate") != null) {
                promotionService.updateConfig("commission.team.rate", String.valueOf(params.get("teamRate")));
            }
            if (params.get("firstLevelRate") != null) {
                promotionService.updateConfig("commission.level.first", String.valueOf(params.get("firstLevelRate")));
            }
            if (params.get("secondLevelRate") != null) {
                promotionService.updateConfig("commission.level.second", String.valueOf(params.get("secondLevelRate")));
            }
            log.info("更新分销配置成功");
            return Result.success();
        } catch (Exception e) {
            log.error("更新分销配置失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取二级分销配置
     */
    @GetMapping("/distribution")
    public Result<Map<String, Object>> getDistributionConfig() {
        Map<String, Object> config = promotionService.getConfig();
        return Result.success(config);
    }

    /**
     * 获取等级分布统计（真实查询）
     */
    @GetMapping("/statistics/level-distribution")
    public Result<Map<String, Object>> getLevelDistribution() {
        Map<String, Object> result = promotionService.getLevelDistribution();
        return Result.success(result);
    }

    /**
     * 佣金发放趋势（真实查询）
     */
    @GetMapping("/statistics/commission-trend")
    public Result<List<Map<String, Object>>> getCommissionTrend(
            @RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> trend = promotionService.getCommissionTrend(days);
        return Result.success(trend);
    }

    /**
     * 头部团队业绩排行（真实查询）
     */
    @GetMapping("/statistics/top-teams")
    public Result<List<Map<String, Object>>> getTopTeams(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> teams = promotionService.getTopTeams(limit);
        return Result.success(teams);
    }
}
