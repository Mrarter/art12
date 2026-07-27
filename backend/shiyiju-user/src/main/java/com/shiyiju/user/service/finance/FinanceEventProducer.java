package com.shiyiju.user.service.finance;

import com.shiyiju.common.event.FinanceEvent;
import com.shiyiju.common.event.FinanceEventPublisher;
import com.shiyiju.user.entity.MqConsumeLog;
import com.shiyiju.user.mapper.MqConsumeLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 金融事件 MQ 生产者
 *
 * 职责：
 * 1. 为每个事件生成唯一 msg_id
 * 2. 写入 Outbox（持久化可靠存储）
 * 3. 发布 Spring Event（低延迟处理）
 *
 * 在当前无独立 MQ（RocketMQ/Kafka）的环境下，
 * Outbox + Spring Event 实现了等效的 at-least-once 语义。
 * 升级到独立 MQ 时，只需替换 publish 实现即可。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceEventProducer {

    private final FinanceEventPublisher eventPublisher;
    private final MqConsumeLogMapper consumeLogMapper;

    /**
     * 发布金融事件到 MQ（当前 = Outbox + Spring Event）
     *
     * @param event 金融事件
     * @return msg_id（用于下游幂等消费）
     */
    public String produce(FinanceEvent event) {
        String msgId = "MSG-" + UUID.randomUUID().toString().replace("-", "")
                + System.currentTimeMillis();

        // 记录 MQ 生产日志
        MqConsumeLog logRecord = new MqConsumeLog();
        logRecord.setMsgId(msgId);
        logRecord.setTxnId(event.getOrderNo() != null ? event.getOrderNo() : String.valueOf(event.getRelatedId()));
        logRecord.setEventType(event.getType().name());
        logRecord.setStatus("PRODUCED");
        logRecord.setCreatedTime(LocalDateTime.now());
        consumeLogMapper.insert(logRecord);

        // 发布事件（Outbox + Spring Event 双写）
        eventPublisher.publish(event);

        log.info("MQ事件已发布: msgId={}, type={}, txnId={}", msgId, event.getType(), logRecord.getTxnId());
        return msgId;
    }
}
