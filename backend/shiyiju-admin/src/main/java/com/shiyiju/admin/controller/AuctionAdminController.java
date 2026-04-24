package com.shiyiju.admin.controller;

import com.shiyiju.admin.service.AuctionService;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 拍卖管理控制器（真实持久化版本）
 */
@RestController
@RequestMapping("/admin/auction")
public class AuctionAdminController {

    private static final Logger log = LoggerFactory.getLogger(AuctionAdminController.class);

    private final AuctionService auctionService;

    public AuctionAdminController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    // ==================== 专场管理 ====================

    /**
     * 专场列表（真实查询数据库）
     */
    @GetMapping("/sessions")
    public Result<PageResult<Map<String, Object>>> getSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        PageResult<Map<String, Object>> result = auctionService.getSessions(page, size, status);
        return Result.success(result);
    }

    /**
     * 获取专场详情
     */
    @GetMapping("/sessions/{id}")
    public Result<Map<String, Object>> getSessionDetail(@PathVariable Long id) {
        Map<String, Object> session = auctionService.getSessionById(id);
        if (session == null) {
            return Result.fail("专场不存在");
        }
        return Result.success(session);
    }

    /**
     * 创建专场（真保存到数据库）
     */
    @PostMapping("/sessions")
    public Result<Map<String, Object>> createSession(@RequestBody Map<String, Object> params) {
        String title = (String) params.get("title");
        
        if (title == null || title.trim().isEmpty()) {
            return Result.fail("专场名称不能为空");
        }

        try {
            Map<String, Object> sessionParams = new HashMap<>();
            sessionParams.put("title", title);
            sessionParams.put("description", params.get("description"));
            sessionParams.put("startTime", params.get("startTime"));
            sessionParams.put("endTime", params.get("endTime"));
            sessionParams.put("status", params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : 0);
            
            Long id = auctionService.createSession(sessionParams);
            log.info("创建专场成功: {}", title);
            Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建专场失败", e);
            return Result.fail("创建专场失败: " + e.getMessage());
        }
    }

    /**
     * 更新专场
     */
    @PutMapping("/sessions/{id}")
    public Result<Void> updateSession(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            auctionService.updateSession(id, params);
            log.info("更新专场成功: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("更新专场失败", e);
            return Result.fail("更新专场失败: " + e.getMessage());
        }
    }

    /**
     * 删除专场
     */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        try {
            boolean success = auctionService.deleteSession(id);
            if (success) {
                log.info("删除专场成功: id={}", id);
                return Result.success();
            } else {
                return Result.fail("专场不存在");
            }
        } catch (Exception e) {
            log.error("删除专场失败", e);
            return Result.fail("删除专场失败: " + e.getMessage());
        }
    }

    // ==================== 拍品管理 ====================

    /**
     * 拍品列表（真实查询数据库）
     */
    @GetMapping("/lots")
    public Result<PageResult<Map<String, Object>>> getLots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long sessionId) {

        PageResult<Map<String, Object>> result = auctionService.getLots(page, size, sessionId);
        return Result.success(result);
    }

    /**
     * 获取拍品详情
     */
    @GetMapping("/lots/{id}")
    public Result<Map<String, Object>> getLotDetail(@PathVariable Long id) {
        Map<String, Object> lot = auctionService.getLotById(id);
        if (lot == null) {
            return Result.fail("拍品不存在");
        }
        return Result.success(lot);
    }

    /**
     * 创建拍品
     */
    @PostMapping("/lots")
    public Result<Map<String, Object>> createLot(@RequestBody Map<String, Object> params) {
        if (params.get("sessionId") == null || params.get("title") == null) {
            return Result.fail("专场ID和拍品名称不能为空");
        }

        try {
            Long id = auctionService.createLot(params);
            log.info("创建拍品成功: {}", params.get("title"));
            Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建拍品失败", e);
            return Result.fail("创建拍品失败: " + e.getMessage());
        }
    }

    /**
     * 更新拍品
     */
    @PutMapping("/lots/{id}")
    public Result<Void> updateLot(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            auctionService.updateLot(id, params);
            log.info("更新拍品成功: id={}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("更新拍品失败", e);
            return Result.fail("更新拍品失败: " + e.getMessage());
        }
    }

    /**
     * 删除拍品
     */
    @DeleteMapping("/lots/{id}")
    public Result<Void> deleteLot(@PathVariable Long id) {
        try {
            boolean success = auctionService.deleteLot(id);
            if (success) {
                log.info("删除拍品成功: id={}", id);
                return Result.success();
            } else {
                return Result.fail("拍品不存在");
            }
        } catch (Exception e) {
            log.error("删除拍品失败", e);
            return Result.fail("删除拍品失败: " + e.getMessage());
        }
    }

    // ==================== 出价记录 ====================

    /**
     * 出价记录（真实查询数据库）
     */
    @GetMapping("/bids")
    public Result<PageResult<Map<String, Object>>> getBids(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long lotId) {

        PageResult<Map<String, Object>> result = auctionService.getBids(page, size, lotId);
        return Result.success(result);
    }

    /**
     * 异常出价预警
     */
    @GetMapping("/bids/warning")
    public Result<List<Map<String, Object>>> getBidWarnings() {
        return Result.success(new ArrayList<>());
    }
}
