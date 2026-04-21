package com.shiyiju.admin.controller;

import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 分销管理控制器
 */
@RestController
@RequestMapping("/admin/promotion")
public class PromotionAdminController {

    private static final Logger log = LoggerFactory.getLogger(PromotionAdminController.class);

    /**
     * 获取分销配置
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("platformRate", 5);
        config.put("totalDistributionLimit", 30);
        config.put("levels", getLevels());
        return Result.success(config);
    }

    private List<Map<String, Object>> getLevels() {
        List<Map<String, Object>> levels = new ArrayList<>();
        levels.add(createLevel(1, "Lv.1见习", 0, 0, 0));
        levels.add(createLevel(2, "Lv.2新锐", 3, 2000, 1));
        levels.add(createLevel(3, "Lv.3资深", 10, 10000, 2));
        levels.add(createLevel(4, "Lv.4金牌", 30, 50000, 3));
        levels.add(createLevel(5, "Lv.5合伙人", 80, 200000, 5));
        return levels;
    }

    private Map<String, Object> createLevel(int level, String name, int minDirect, long minSales, double teamBonus) {
        Map<String, Object> levelConfig = new HashMap<>();
        levelConfig.put("level", level);
        levelConfig.put("name", name);
        levelConfig.put("minDirectCount", minDirect);
        levelConfig.put("minTeamMonthlySales", minSales);
        levelConfig.put("teamBonusRate", teamBonus);
        return levelConfig;
    }

    /**
     * 更新分销配置
     */
    @PostMapping("/config")
    public Result<Void> updateConfig(@RequestBody Map<String, Object> params) {
        log.info("更新分销配置: platformRate={}, totalDistributionLimit={}", 
            params.get("platformRate"), params.get("totalDistributionLimit"));
        return Result.success();
    }

    /**
     * 获取二级分销配置
     */
    @GetMapping("/distribution")
    public Result<Map<String, Object>> getDistributionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("firstRate", 0.10);
        config.put("secondRate", 0.05);
        config.put("lockPeriod", 30);
        return Result.success(config);
    }

    /**
     * 获取等级分布统计
     */
    @GetMapping("/statistics/level-distribution")
    public Result<Map<String, Object>> getLevelDistribution() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 1256);
        result.put("distribution", Arrays.asList(
            createDistItem(1, "Lv.1见习", 520, 41.4),
            createDistItem(2, "Lv.2新锐", 380, 30.3),
            createDistItem(3, "Lv.3资深", 220, 17.5),
            createDistItem(4, "Lv.4金牌", 100, 8.0),
            createDistItem(5, "Lv.5合伙人", 36, 2.9)
        ));
        return Result.success(result);
    }

    private Map<String, Object> createDistItem(int level, String name, int count, double percent) {
        Map<String, Object> item = new HashMap<>();
        item.put("level", level);
        item.put("name", name);
        item.put("count", count);
        item.put("percent", percent);
        return item;
    }

    /**
     * 佣金发放趋势
     */
    @GetMapping("/statistics/commission-trend")
    public Result<List<Map<String, Object>>> getCommissionTrend(
            @RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", "2024-04-" + String.format("%02d", 20 - i % 30));
            item.put("amount", 5000 + (days - i) * 200);
            trend.add(item);
        }
        return Result.success(trend);
    }

    /**
     * 头部团队业绩排行
     */
    @GetMapping("/statistics/top-teams")
    public Result<List<Map<String, Object>>> getTopTeams(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> teams = new ArrayList<>();
        String[] names = {"艺术推广大使", "金牌代理", "资深推广"};
        for (int i = 0; i < names.length && i < limit; i++) {
            Map<String, Object> team = new HashMap<>();
            team.put("rank", i + 1);
            team.put("nickname", names[i]);
            team.put("level", 5 - i);
            team.put("teamSize", 100 - i * 15);
            team.put("monthlySales", 500000 - i * 50000);
            teams.add(team);
        }
        return Result.success(teams);
    }
}
