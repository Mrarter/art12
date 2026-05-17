package com.shiyiju.user.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiyiju.common.event.FinanceEvent;
import com.shiyiju.user.entity.FinanceEventDlq;
import com.shiyiju.user.entity.FinanceEventOutbox;
import com.shiyiju.user.mapper.FinanceEventDlqMapper;
import com.shiyiju.user.mapper.FinanceEventOutboxMapper;
import com.shiyiju.user.service.FinanceEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 金融事件 Outbox 核心服务
 *
 * 设计：Transactional Outbox Pattern
 * ┌─────────────────────────────────────┐
 * │  业务操作（OrderService）             │
 * │  @Transactional                     │
 * │  1. order.status = PAID            │
 * │  2. outboxService.write(event)     │  ← 同事务写入
 * └──────────────┬──────────────────────┘
 *                │
 *   PollingTask  ▼  每5秒轮询
 * ┌─────────────────────────────────────┐
 * │  FinanceEventPollingTask            │
 * │  1. poll PENDING events             │
 * │  2. status = PROCESSING             │
 * │  3. FinanceEventHandler.process()   │
 * │  4. success → COMPLETED             │
 * │  5. fail → retry / → DLQ           │
 * └─────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceEventOutboxService {

    private final FinanceEventOutboxMapper outboxMapper;
    private final FinanceEventDlqMapper dlqMapper;
    private final FinanceEventHandler financeEventHandler;
    private final ObjectMapper objectMapper;

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;
    /** 重试间隔基数（秒） */
    private static final long RETRY_INTERVAL_SECONDS = 30;

    // ===================== 写入 Outbox =====================

    /**
     * 写入 Outbox（与业务操作同事务）
     * 调用方必须在 @Transactional 内调用此方法
     */
    @Transactional(rollbackFor = Exception.class)
    public void write(FinanceEvent event) {
        try {
            FinanceEventOutbox outbox = new FinanceEventOutbox();
            outbox.setEventType(event.getType().name());
            outbox.setEventBody(objectMapper.writeValueAsString(event));
            outbox.setStatus("PENDING");
            outbox.setRetryCount(0);
            outbox.setMaxRetries(MAX_RETRIES);
            outbox.setCreatedTime(LocalDateTime.now());
            outboxMapper.insert(outbox);
            log.debug("写入Outbox: type={}, id={}", event.getType(), outbox.getId());
        } catch (Exception e) {
            log.error("写入Outbox失败: type={}", event.getType(), e);
            throw new RuntimeException("金融事件写入失败", e);
        }
    }

    // ===================== 轮询处理 =====================

    /**
     * 轮询并处理待处理事件（由定时任务调用）
     */
    public int pollAndProcess(int batchSize) {
        List<FinanceEventOutbox> events = outboxMapper.pollPendingEvents(batchSize);
        if (events.isEmpty()) return 0;

        for (FinanceEventOutbox outbox : events) {
            processOutbox(outbox);
        }
        return events.size();
    }

    /**
     * 处理单个 Outbox 事件
     */
    private void processOutbox(FinanceEventOutbox outbox) {
        // 标记处理中
        outboxMapper.markProcessing(outbox.getId());

        try {
            // 反序列化事件
            FinanceEvent event = objectMapper.readValue(outbox.getEventBody(), FinanceEvent.class);

            // 调用事件处理器
            financeEventHandler.handleFinanceEvent(event);

            // 标记完成
            outboxMapper.markCompleted(outbox.getId());
            log.info("Outbox事件处理完成: id={}, type={}", outbox.getId(), outbox.getEventType());

        } catch (Exception e) {
            log.error("Outbox事件处理失败: id={}, type={}", outbox.getId(), outbox.getEventType(), e);
            handleFailure(outbox, e);
        }
    }

    /**
     * 处理失败 — 可重试的加下次重试时间，超限的移入 DLQ
     */
    private void handleFailure(FinanceEventOutbox outbox, Exception e) {
        int newRetryCount = (outbox.getRetryCount() != null ? outbox.getRetryCount() : 0) + 1;
        if (newRetryCount < MAX_RETRIES) {
            // 可重试：更新重试次数和下次重试时间
            LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(RETRY_INTERVAL_SECONDS * newRetryCount);
            outboxMapper.markFailed(outbox.getId(), newRetryCount,
                    e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "未知错误",
                    nextRetry);
            log.warn("Outbox事件将重试: id={}, retry={}/{}", outbox.getId(), newRetryCount, MAX_RETRIES);
        } else {
            // 超过最大重试次数 → 移入 DLQ
            moveToDlq(outbox, e);
        }
    }

    /**
     * 移入死信队列（DLQ）
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveToDlq(FinanceEventOutbox outbox, Exception e) {
        FinanceEventDlq dlq = new FinanceEventDlq();
        dlq.setOutboxId(outbox.getId());
        dlq.setEventType(outbox.getEventType());
        dlq.setEventBody(outbox.getEventBody());
        dlq.setRetryCount(outbox.getRetryCount());
        dlq.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "未知错误");
        dlq.setFailedTime(LocalDateTime.now());
        dlq.setIsReplayed(0);
        dlqMapper.insert(dlq);

        // 标记 Outbox 为最终失败
        outbox.setStatus("FAILED");
        outbox.setErrorMessage(dlq.getErrorMessage());
        outbox.setCompletedTime(LocalDateTime.now());
        outboxMapper.updateById(outbox);

        log.error("事件移入DLQ: outboxId={}, type={}", outbox.getId(), outbox.getEventType());
    }

    // ===================== Replay =====================

    /**
     * 重放 DLQ 事件到 Outbox
     */
    @Transactional(rollbackFor = Exception.class)
    public void replay(Long dlqId) {
        FinanceEventDlq dlq = dlqMapper.selectById(dlqId);
        if (dlq == null) {
            throw new RuntimeException("DLQ记录不存在: id=" + dlqId);
        }
        if (dlq.getIsReplayed() != null && dlq.getIsReplayed() == 1) {
            throw new RuntimeException("该事件已重放，不可重复执行");
        }

        // 重建 Outbox 事件
        FinanceEventOutbox outbox = new FinanceEventOutbox();
        outbox.setEventType(dlq.getEventType());
        outbox.setEventBody(dlq.getEventBody());
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setMaxRetries(MAX_RETRIES);
        outbox.setCreatedTime(LocalDateTime.now());
        outboxMapper.insert(outbox);

        // 标记 DLQ 为已重放
        dlq.setIsReplayed(1);
        dlq.setReplayedTime(LocalDateTime.now());
        dlqMapper.updateById(dlq);

        log.info("DLQ事件已重放: dlqId={}, newOutboxId={}", dlqId, outbox.getId());
    }

    /**
     * 批量重放所有未重放的 DLQ
     */
    public int replayAll() {
        List<FinanceEventDlq> dlqs = dlqMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinanceEventDlq>()
                        .eq(FinanceEventDlq::getIsReplayed, 0));
        int count = 0;
        for (FinanceEventDlq dlq : dlqs) {
            try {
                replay(dlq.getId());
                count++;
            } catch (Exception e) {
                log.error("批量重放失败: dlqId={}", dlq.getId(), e);
            }
        }
        return count;
    }

    // ===================== 查询 =====================

    public List<FinanceEventDlq> listDlq() {
        return dlqMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinanceEventDlq>()
                        .orderByDesc(FinanceEventDlq::getFailedTime));
    }

    public List<FinanceEventOutbox> listIncomplete() {
        return outboxMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinanceEventOutbox>()
                        .in(FinanceEventOutbox::getStatus, "PENDING", "PROCESSING", "FAILED")
                        .orderByDesc(FinanceEventOutbox::getCreatedTime));
    }
}
