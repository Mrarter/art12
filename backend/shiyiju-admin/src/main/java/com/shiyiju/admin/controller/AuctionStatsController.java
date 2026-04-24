package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.AuctionService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 拍卖统计控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin/auction/admin")
public class AuctionStatsController {

    @Autowired
    private AuctionService auctionService;

    /**
     * 拍卖统计概览（真实查询数据库）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Map<String, Object> stats = auctionService.getStats(startDate, endDate);
        return Result.success(stats);
    }

    /**
     * 专场排行（真实查询）
     */
    @GetMapping("/session-rank")
    public Result<List<Map<String, Object>>> getSessionRank(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Map<String, Object>> list = auctionService.getSessionRank(startDate, endDate);
        return Result.success(list);
    }

    /**
     * 艺术家排行（真实查询）
     */
    @GetMapping("/artist-rank")
    public Result<List<Map<String, Object>>> getArtistRank(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Map<String, Object>> list = auctionService.getArtistRank(startDate, endDate);
        return Result.success(list);
    }

    /**
     * 成交明细（真实查询）
     */
    @GetMapping("/deals")
    public Result<PageResult<Map<String, Object>>> getDeals(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<Map<String, Object>> result = auctionService.getDeals(page, size, startDate, endDate);
        return Result.success(result);
    }

    /**
     * 拍卖趋势（真实查询）
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(defaultValue = "7") int days) {

        List<Map<String, Object>> list = auctionService.getTrend(days);
        return Result.success(list);
    }
}
