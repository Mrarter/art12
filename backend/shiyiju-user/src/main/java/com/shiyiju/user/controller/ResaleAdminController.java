package com.shiyiju.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.result.Result;
import com.shiyiju.user.entity.ArtworkPriceHistory;
import com.shiyiju.user.entity.ArtworkTradeRecord;
import com.shiyiju.user.entity.ResaleRecord;
import com.shiyiju.user.service.ResaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 - 转售管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/resale")
@RequiredArgsConstructor
public class ResaleAdminController {

    private final ResaleService resaleService;

    @Value("${wallet.admin-key:shiyiju-wallet-admin-2026}")
    private String adminKey;

    /**
     * 转售记录管理 - 分页列表
     * GET /admin/resale/list?page=1&pageSize=20&status=&artworkId=
     */
    @GetMapping("/list")
    public Result<Page<ResaleRecord>> listResales(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long artworkId,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Page<ResaleRecord> result = resaleService.adminListResales(page, pageSize, status, artworkId);
        return Result.success(result);
    }

    /**
     * 转售详情
     * GET /admin/resale/{id}
     */
    @GetMapping("/{id}")
    public Result<ResaleRecord> getDetail(
            @PathVariable Long id,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        ResaleRecord record = resaleService.getResaleDetail(id);
        return Result.success(record);
    }

    /**
     * 手动完成转售（后台确认）
     * POST /admin/resale/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public Result<ResaleRecord> completeResale(
            @PathVariable Long id,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        ResaleRecord record = resaleService.completeResale(id);
        return Result.success(record);
    }

    /**
     * 取消转售（后台强制取消）
     * POST /admin/resale/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelResale(
            @PathVariable Long id,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        resaleService.adminCancelResale(id);
        log.info("管理员强制取消转售: id={}", id);
        return Result.success();
    }

    /**
     * 平台抽佣统计
     * GET /admin/resale/platform-fee-stats
     */
    @GetMapping("/platform-fee-stats")
    public Result<Map<String, Object>> platformFeeStats(
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Map<String, Object> stats = resaleService.adminPlatformFeeStats();
        return Result.success(stats);
    }

    /**
     * 流通数据统计
     * GET /admin/resale/circulation-stats
     */
    @GetMapping("/circulation-stats")
    public Result<Map<String, Object>> circulationStats(
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Map<String, Object> stats = resaleService.adminCirculationStats();
        return Result.success(stats);
    }

    /**
     * 作品交易链路查询
     * GET /admin/resale/artwork/{artworkId}/trades
     */
    @GetMapping("/artwork/{artworkId}/trades")
    public Result<List<ArtworkTradeRecord>> getArtworkTrades(
            @PathVariable Long artworkId,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        List<ArtworkTradeRecord> trades = resaleService.getArtworkTradeHistory(artworkId);
        return Result.success(trades);
    }

    /**
     * 作品价格历史查询
     * GET /admin/resale/artwork/{artworkId}/price-history
     */
    @GetMapping("/artwork/{artworkId}/price-history")
    public Result<List<ArtworkPriceHistory>> getArtworkPriceHistory(
            @PathVariable Long artworkId,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        List<ArtworkPriceHistory> history = resaleService.getArtworkPriceHistory(artworkId);
        return Result.success(history);
    }

    /**
     * 作品转售统计
     * GET /admin/resale/artwork/{artworkId}/stats
     */
    @GetMapping("/artwork/{artworkId}/stats")
    public Result<Map<String, Object>> getArtworkStats(
            @PathVariable Long artworkId,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Map<String, Object> stats = resaleService.getArtworkResaleStats(artworkId);
        return Result.success(stats);
    }

    /**
     * 标记转售已支付（订单支付回调后调用）
     * POST /admin/resale/mark-paid
     */
    @PostMapping("/mark-paid")
    public Result<Void> markAsPaid(
            @RequestBody Map<String, Object> params,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        Long resaleId = params.get("resaleId") != null ? ((Number) params.get("resaleId")).longValue() : null;
        Long buyerUserId = params.get("buyerUserId") != null ? ((Number) params.get("buyerUserId")).longValue() : null;
        if (resaleId == null || buyerUserId == null) {
            return Result.fail(400, "参数不完整");
        }
        resaleService.markAsPaid(resaleId, buyerUserId);
        log.info("转售标记已支付(服务调用): resaleId={}, buyerUserId={}", resaleId, buyerUserId);
        return Result.success();
    }
}
