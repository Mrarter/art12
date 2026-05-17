package com.shiyiju.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.result.Result;
import com.shiyiju.user.entity.ArtworkPriceHistory;
import com.shiyiju.user.entity.ArtworkTradeRecord;
import com.shiyiju.user.entity.ResaleRecord;
import com.shiyiju.user.service.ResaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 转售市场控制器
 */
@Slf4j
@RestController
@RequestMapping("/user/resale")
@RequiredArgsConstructor
public class ResaleController {

    private final ResaleService resaleService;

    /**
     * 发布转售
     * POST /user/resale/publish
     */
    @PostMapping("/publish")
    public Result<ResaleRecord> publishResale(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params) {
        if (userId == null) return Result.fail(401, "请先登录");

        Long artworkId = params.get("artworkId") != null
                ? ((Number) params.get("artworkId")).longValue() : null;
        BigDecimal resalePrice = params.get("resalePrice") != null
                ? new BigDecimal(params.get("resalePrice").toString()) : null;

        ResaleRecord record = resaleService.publishResale(userId, artworkId, resalePrice);
        return Result.success(record);
    }

    /**
     * 转售市场列表
     * GET /user/resale/list?page=1&pageSize=20&artworkId=
     */
    @GetMapping("/list")
    public Result<Page<ResaleRecord>> listResales(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long artworkId) {
        Page<ResaleRecord> result = resaleService.listResales(page, pageSize, artworkId);
        return Result.success(result);
    }

    /**
     * 我的转售（作为卖家）
     * GET /user/resale/my?page=1&pageSize=20&status=
     */
    @GetMapping("/my")
    public Result<Page<ResaleRecord>> myResales(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status) {
        if (userId == null) return Result.fail(401, "请先登录");
        Page<ResaleRecord> result = resaleService.listMyResales(userId, page, pageSize, status);
        return Result.success(result);
    }

    /**
     * 转售详情
     * GET /user/resale/{id}
     */
    @GetMapping("/{id}")
    public Result<ResaleRecord> getResaleDetail(@PathVariable Long id) {
        ResaleRecord record = resaleService.getResaleDetail(id);
        return Result.success(record);
    }

    /**
     * 取消转售
     * POST /user/resale/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelResale(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        if (userId == null) return Result.fail(401, "请先登录");
        resaleService.cancelResale(id, userId);
        return Result.success();
    }

    /**
     * 作品交易链路
     * GET /user/resale/artwork/{artworkId}/trades
     */
    @GetMapping("/artwork/{artworkId}/trades")
    public Result<List<ArtworkTradeRecord>> getArtworkTrades(@PathVariable Long artworkId) {
        List<ArtworkTradeRecord> trades = resaleService.getArtworkTradeHistory(artworkId);
        return Result.success(trades);
    }

    /**
     * 作品价格历史
     * GET /user/resale/artwork/{artworkId}/price-history
     */
    @GetMapping("/artwork/{artworkId}/price-history")
    public Result<List<ArtworkPriceHistory>> getArtworkPriceHistory(@PathVariable Long artworkId) {
        List<ArtworkPriceHistory> history = resaleService.getArtworkPriceHistory(artworkId);
        return Result.success(history);
    }

    /**
     * 作品转售统计
     * GET /user/resale/artwork/{artworkId}/stats
     */
    @GetMapping("/artwork/{artworkId}/stats")
    public Result<Map<String, Object>> getArtworkStats(@PathVariable Long artworkId) {
        Map<String, Object> stats = resaleService.getArtworkResaleStats(artworkId);
        return Result.success(stats);
    }
}
