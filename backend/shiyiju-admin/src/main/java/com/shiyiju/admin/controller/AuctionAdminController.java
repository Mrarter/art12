package com.shiyiju.admin.controller;

import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 拍卖管理控制器
 */
@RestController
@RequestMapping("/admin/auction")
public class AuctionAdminController {

    private static final Logger log = LoggerFactory.getLogger(AuctionAdminController.class);

    /**
     * 专场列表
     */
    @GetMapping("/sessions")
    public Result<PageResult<Map<String, Object>>> getSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        String[] titles = {"春季艺术品拍卖专场", "当代油画专场", "名家书法专场"};
        int[] statuses = {2, 2, 1};
        for (int i = 0; i < titles.length; i++) {
            Map<String, Object> session = new HashMap<>();
            session.put("id", i + 1);
            session.put("title", titles[i]);
            session.put("status", status != null ? status : statuses[i]);
            session.put("statusText", statuses[i] == 1 ? "预告" : statuses[i] == 2 ? "进行中" : "已结束");
            session.put("lotCount", 20 + i * 10);
            session.put("bidCount", 50 + i * 20);
            session.put("startTime", "2024-04-01 10:00:00");
            sessions.add(session);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(sessions);
        result.setTotal(3L);
        return Result.success(result);
    }

    /**
     * 创建专场
     */
    @PostMapping("/sessions")
    public Result<Void> createSession(@RequestBody Map<String, Object> params) {
        log.info("创建专场: {}", params);
        return Result.success();
    }

    /**
     * 拍品列表
     */
    @GetMapping("/lots")
    public Result<PageResult<Map<String, Object>>> getLots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long sessionId) {
        List<Map<String, Object>> lots = new ArrayList<>();
        String[] titles = {"山水长卷", "花开富贵", "书法对联", "静物写生"};
        long[] prices = {50000, 30000, 20000, 45000};
        for (int i = 0; i < titles.length; i++) {
            Map<String, Object> lot = new HashMap<>();
            lot.put("id", i + 1);
            lot.put("title", titles[i]);
            lot.put("startPrice", prices[i]);
            lot.put("currentPrice", prices[i] + i * 5000);
            lot.put("bidCount", 5 + i * 3);
            lot.put("status", i % 2 == 0 ? 2 : 1);
            lots.add(lot);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(lots);
        result.setTotal(4L);
        return Result.success(result);
    }

    /**
     * 出价记录
     */
    @GetMapping("/bids")
    public Result<PageResult<Map<String, Object>>> getBids(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long lotId) {
        List<Map<String, Object>> bids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> bid = new HashMap<>();
            bid.put("id", i + 1);
            bid.put("lotId", lotId != null ? lotId : 1);
            bid.put("nickname", "竞拍者" + (i + 1));
            bid.put("bidPrice", 50000 + i * 1000);
            bid.put("bidTime", "2024-04-01 " + (10 + i) + ":30:00");
            bid.put("isWinning", i == 4);
            bids.add(bid);
        }
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(bids);
        result.setTotal(5L);
        return Result.success(result);
    }

    /**
     * 异常出价预警
     */
    @GetMapping("/bids/warning")
    public Result<List<Map<String, Object>>> getBidWarnings() {
        List<Map<String, Object>> warnings = new ArrayList<>();
        Map<String, Object> warning = new HashMap<>();
        warning.put("id", 1);
        warning.put("lotTitle", "山水长卷");
        warning.put("nickname", "用户A");
        warning.put("bidCount", 15);
        warning.put("bidMinutes", 5);
        warning.put("warningType", "高频出价");
        warnings.add(warning);
        return Result.success(warnings);
    }
}
