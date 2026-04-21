package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 拍卖统计控制器
 */
@RestController
@RequestMapping("/admin/auction/admin")
public class AuctionStatsController {

    /**
     * 拍卖统计概览
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSessions", 50);
        stats.put("totalTurnover", 1250000.00);
        stats.put("totalBids", 3500);
        stats.put("activeSessions", 5);
        stats.put("todayTurnover", 85000.00);
        stats.put("todayBids", 120);
        return Result.success(stats);
    }

    /**
     * 专场排行
     */
    @GetMapping("/session-rank")
    public Result<List<Map<String, Object>>> getSessionRank(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        String[] names = {"春季艺术品拍卖", "当代油画专场", "书法精品拍卖"};
        
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", i + 1);
            item.put("title", names[i]);
            item.put("turnover", 500000.00 - i * 100000);
            item.put("lotCount", 50 - i * 10);
            item.put("bidCount", 200 - i * 30);
            list.add(item);
        }
        return Result.success(list);
    }

    /**
     * 艺术家排行
     */
    @GetMapping("/artist-rank")
    public Result<List<Map<String, Object>>> getArtistRank(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("artistId", i);
            item.put("artistName", "艺术家" + i);
            item.put("作品数量", 20 - i);
            item.put("成交额", 200000.00 - i * 15000);
            item.put("成交率", 85.0 - i * 2);
            list.add(item);
        }
        return Result.success(list);
    }

    /**
     * 成交明细
     */
    @GetMapping("/deals")
    public Result<PageResult<Map<String, Object>>> getDeals(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> deal = new LinkedHashMap<>();
            deal.put("id", i);
            deal.put("artworkTitle", "作品" + i);
            deal.put("artistName", "艺术家" + i);
            deal.put("finalPrice", 50000.00 + i * 5000);
            deal.put("buyerName", "买家" + i);
            deal.put("sessionTitle", "拍卖专场" + i);
            deal.put("dealTime", "2024-04-" + String.format("%02d", 10 + i) + " 15:30:00");
            list.add(deal);
        }
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(100L);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }

    /**
     * 拍卖趋势
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(defaultValue = "7") int days) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", "2024-04-" + String.format("%02d", 15 - i));
            item.put("turnover", 80000.00 + i * 5000);
            item.put("bids", 100 + i * 10);
            item.put("deals", 5 + i % 3);
            list.add(item);
        }
        return Result.success(list);
    }
}