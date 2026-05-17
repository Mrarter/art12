package com.shiyiju.common.event;

/**
 * 金融事件写入器接口
 *
 * 由 user 模块的 FinanceEventOutboxService 实现，
 * 在 common 模块的 FinanceEventPublisher 中通过 @Autowired(required=false) 注入。
 *
 * 如果实现不存在（如 order 模块独立测试时），
 * 事件仅通过 Spring ApplicationEventPublisher 同步发布（降级模式）。
 */
public interface IFinanceEventWriter {

    /**
     * 写入金融事件（通常写入 Outbox 表）
     * 调用方应保证在事务内调用
     */
    void write(FinanceEvent event);
}
