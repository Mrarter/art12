package com.shiyiju.user.service.outbox;

import com.shiyiju.common.event.FinanceEvent;
import com.shiyiju.common.event.IFinanceEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * IFinanceEventWriter 实现 — 写入 Outbox 表
 *
 * 被 common 模块的 FinanceEventPublisher 自动注入。
 * 每次 publish() 调用都会：
 * 1. 写入 Outbox（同事务）
 * 2. 发布 Spring 事件（同线程处理或异步）
 */
@Component
@RequiredArgsConstructor
public class FinanceEventOutboxWriter implements IFinanceEventWriter {

    private final FinanceEventOutboxService outboxService;

    @Override
    public void write(FinanceEvent event) {
        outboxService.write(event);
    }
}
