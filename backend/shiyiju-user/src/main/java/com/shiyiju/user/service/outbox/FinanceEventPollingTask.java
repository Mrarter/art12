package com.shiyiju.user.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 金融事件 Outbox 轮询任务
 *
 * 每5秒扫描一次 Outbox 表，取出 PENDING 事件进行处理。
 * 相当于「数据库轮询版 MQ Consumer」。
 *
 * 设计要点：
 * - 使用 FOR UPDATE SKIP LOCKED 防止多实例重复处理
 * - 失败事件自动重试（指数退避）
 * - 超限事件移入 DLQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceEventPollingTask {

    private final FinanceEventOutboxService outboxService;

    /** 每次拉取的最大事件数 */
    private static final int BATCH_SIZE = 50;

    /**
     * 每5秒轮询一次
     */
    @Scheduled(fixedDelay = 5000)
    public void pollOutbox() {
        try {
            int processed = outboxService.pollAndProcess(BATCH_SIZE);
            if (processed > 0) {
                log.debug("Outbox轮询: 处理了 {} 个事件", processed);
            }
        } catch (Exception e) {
            log.error("Outbox轮询异常", e);
        }
    }
}
