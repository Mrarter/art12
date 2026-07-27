package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 艺术家数据分析控制器
 * 提供艺术家主页的访问量、粉丝趋势、互动率、受众画像等分析数据
 *
 * 前端路径: /api/user/artist/analytics/{artistId}/overview
 *          → gateway → user-service:8081/user/artist/analytics/...
 */
@Slf4j
@RestController
@RequestMapping("/user/artist/analytics")
@RequiredArgsConstructor
public class ArtistAnalyticsController {

    private final UserService userService;

    /**
     * 获取核心指标概览
     */
    @GetMapping("/{artistId}/overview")
    public Result<Map<String, Object>> getOverview(@PathVariable Long artistId) {
        return Result.success(userService.getArtistAnalyticsOverview(artistId));
    }

    /**
     * 获取趋势数据
     * @param days 天数: 7/30/90
     */
    @GetMapping("/{artistId}/trend")
    public Result<Map<String, Object>> getTrend(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(userService.getArtistAnalyticsTrend(artistId, days));
    }

    /**
     * 获取受众画像
     */
    @GetMapping("/{artistId}/audience")
    public Result<Map<String, Object>> getAudienceProfile(@PathVariable Long artistId) {
        return Result.success(userService.getArtistAudienceProfile(artistId));
    }
}
