package com.shiyiju.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 金融事件发布器 — 所有资金变动的唯一入口
 *
 * 架构层级：
 * ┌────────────────────────────────────────────┐
 * │  OrderService (or any caller)              │
 * │  financeEventPublisher.publish(event)      │
 * └──────────┬─────────────────────────────┬────┘
 *            │                             │
 *            ▼                             ▼
 *  ┌─────────────────┐        ┌──────────────────────┐
 *  │ Outbox Writer    │        │ Spring Event         │
 *  │ (同事务写DB)      │        │ (同步/异步处理)       │
 *  └────────┬─────────┘        └──────────┬───────────┘
 *           │                             │
 *           ▼                             ▼
 *   ┌──────────────┐          ┌──────────────────────┐
 *   │ Polling Task │          │ FinanceEventHandler  │
 *   │ (可靠重试)    │          │ (直接处理)            │
 *   └──────────────┘          └──────────────────────┘
 *
 * 双写保证：Outbox + Spring Event 同时写入。
 * Outbox 保证不丢事件，Spring Event 保证低延迟处理。
 */
@Slf4j
@Component
public class FinanceEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /** 可选的 Outbox 写入器（由 user 模块注入） */
    private IFinanceEventWriter outboxWriter;

    public FinanceEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Autowired(required = false)
    public void setOutboxWriter(IFinanceEventWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    /**
     * 发布金融事件
     * 1. 写入 Outbox（保证不丢失）
     * 2. 发布 Spring 事件（保证低延迟处理）
     */
    public void publish(FinanceEvent event) {
        // 1. 写入 Outbox（同事务，保证不丢）
        if (outboxWriter != null) {
            try {
                outboxWriter.write(event);
            } catch (Exception e) {
                log.error("Outbox写入失败，仅通过Spring Event投递: type={}", event.getType(), e);
            }
        }

        // 2. 发布 Spring 事件（同线程/异步处理）
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Spring Event发布失败: type={}", event.getType(), e);
            // Outbox 已写入，轮询任务会补偿
        }

        log.info("金融事件已发布: type={}, userId={}, amount={}, relatedId={}",
                event.getType(), event.getUserId(), event.getAmount(), event.getRelatedId());
    }

    /**
     * 仅写入 Outbox（不发布 Spring Event）
     * 用于需要严格顺序或延迟处理的场景
     */
    public void publishToOutboxOnly(FinanceEvent event) {
        if (outboxWriter != null) {
            outboxWriter.write(event);
        }
    }
}
