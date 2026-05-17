package com.shiyiju.user.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.user.entity.FinanceEventDlq;
import com.shiyiju.user.entity.FinanceEventOutbox;
import com.shiyiju.user.service.outbox.FinanceEventOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 金融事件管理后台 — Outbox / DLQ / Replay 管理
 *
 * 用于监控和恢复金融事件处理状态。
 */
@Slf4j
@RestController
@RequestMapping("/admin/finance-event")
@RequiredArgsConstructor
public class FinanceEventAdminController {

    private final FinanceEventOutboxService outboxService;

    @Value("${wallet.admin-key:shiyiju-wallet-admin-2026}")
    private String adminKey;

    /**
     * 查看死信队列
     * GET /admin/finance-event/dlq
     */
    @GetMapping("/dlq")
    public Result<List<FinanceEventDlq>> listDlq(
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        return Result.success(outboxService.listDlq());
    }

    /**
     * 重放单个 DLQ 事件
     * POST /admin/finance-event/dlq/{id}/replay
     */
    @PostMapping("/dlq/{id}/replay")
    public Result<Void> replayDlq(
            @PathVariable Long id,
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        outboxService.replay(id);
        log.info("管理员重放DLQ事件: id={}", id);
        return Result.success();
    }

    /**
     * 批量重放所有 DLQ
     * POST /admin/finance-event/dlq/replay-all
     */
    @PostMapping("/dlq/replay-all")
    public Result<Map<String, Integer>> replayAllDlq(
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        int count = outboxService.replayAll();
        log.info("管理员批量重放DLQ: count={}", count);
        return Result.success(Map.of("replayedCount", count));
    }

    /**
     * 查看未完成事件
     * GET /admin/finance-event/incomplete
     */
    @GetMapping("/incomplete")
    public Result<List<FinanceEventOutbox>> listIncomplete(
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        return Result.success(outboxService.listIncomplete());
    }

    /**
     * 手动触发一次轮询
     * POST /admin/finance-event/poll
     */
    @PostMapping("/poll")
    public Result<Map<String, Integer>> triggerPoll(
            @RequestHeader("Wallet-Admin-Key") String key) {
        if (!adminKey.equals(key)) return Result.fail(403, "无效的管理密钥");
        int processed = outboxService.pollAndProcess(100);
        return Result.success(Map.of("processed", processed));
    }
}
