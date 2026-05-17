package com.shiyiju.order.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.order.OrderFailReason;
import com.shiyiju.order.entity.OrderFailRecord;
import com.shiyiju.order.mapper.OrderFailRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单失败记录服务
 * 负责记录订单创建失败的详细信息，支持回滚补偿和重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFailRecorder {

    private final OrderFailRecordMapper orderFailRecordMapper;

    /**
     * 记录订单失败事件
     *
     * @param userId     用户ID
     * @param failReason 失败原因
     * @param detail     失败详情
     * @param params     请求参数（任意对象，自动序列化为JSON）
     */
    public void record(RecordContext ctx) {
        OrderFailRecord record = new OrderFailRecord();
        record.setOrderNo(ctx.orderNo);
        record.setUserId(ctx.userId);
        record.setArtworkId(ctx.artworkId);
        record.setResaleId(ctx.resaleId);
        record.setCartIds(ctx.cartIds);
        record.setSource(ctx.source);
        record.setFailReason(ctx.failReason.getCode());
        record.setFailMessage(ctx.failMessage);
        record.setRequestParams(ctx.requestParamsJson);
        record.setRetryCount(0);
        record.setMaxRetries(3);
        record.setRetryStatus(0);  // 0-未重试
        record.setCompensated(ctx.compensated ? 1 : 0);
        record.setNextRetryAt(determineNextRetry(ctx.failReason));

        orderFailRecordMapper.insert(record);

        log.warn("订单失败已记录 - userId={}, reason={}, orderNo={}, recordId={}",
                ctx.userId, ctx.failReason.getCode(), ctx.orderNo, record.getId());
    }

    /**
     * 根据失败原因判断是否需要补偿回滚
     * 已生成订单号但后续步骤失败时必须补偿
     */
    public void markCompensated(Long recordId) {
        OrderFailRecord record = orderFailRecordMapper.selectById(recordId);
        if (record != null) {
            record.setCompensated(1);
            record.setCompensateAt(LocalDateTime.now());
            orderFailRecordMapper.updateById(record);
        }
    }

    /**
     * 查询用户的可重试失败记录
     */
    public List<OrderFailRecord> getRetryableRecords(Long userId) {
        return orderFailRecordMapper.selectList(
                new LambdaQueryWrapper<OrderFailRecord>()
                        .eq(OrderFailRecord::getUserId, userId)
                        .and(w -> w.eq(OrderFailRecord::getRetryStatus, 0)
                                .or(w2 -> w2.eq(OrderFailRecord::getRetryStatus, 1)
                                        .apply("retry_count < max_retries")))
                        .orderByDesc(OrderFailRecord::getCreateTime)
        );
    }

    /**
     * 获取某条失败记录详情
     */
    public OrderFailRecord getById(Long id) {
        return orderFailRecordMapper.selectById(id);
    }

    /**
     * 更新重试计数与状态
     */
    public void updateRetryStatus(Long recordId, int retryCount, int status) {
        OrderFailRecord record = orderFailRecordMapper.selectById(recordId);
        if (record != null) {
            record.setRetryCount(retryCount);
            record.setRetryStatus(status);
            if (status == 3) {
                // 重试失败，放弃不再尝试
                record.setNextRetryAt(null);
            } else if (retryCount < record.getMaxRetries()) {
                // 下次重试时间：指数退避
                long delaySeconds = (long) Math.pow(2, retryCount) * 5;
                record.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            }
            orderFailRecordMapper.updateById(record);
        }
    }

    /**
     * 判断失败是否可重试
     */
    public boolean isRetryable(OrderFailRecord record) {
        if (record == null) return false;
        if (record.getRetryStatus() == 2) return false; // 已成功
        if (record.getRetryStatus() == 3) return false; // 已放弃
        return record.getRetryCount() < record.getMaxRetries();
    }

    /**
     * 根据失败原因决定首次重试延迟
     */
    private LocalDateTime determineNextRetry(OrderFailReason reason) {
        long delaySeconds;
        switch (reason) {
            case INVENTORY_LOCK_FAILED:
            case CONCURRENT_CONFLICT:
            case LOCK_ACQUIRE_FAILED:
                delaySeconds = 2;  // 并发类问题快速重试
                break;
            case NETWORK_ERROR:
            case CIRCUIT_BREAKER:
                delaySeconds = 10; // 网络类问题稍等
                break;
            case DATABASE_ERROR:
                delaySeconds = 5;  // 数据库问题
                break;
            default:
                delaySeconds = 3;
        }
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    /**
     * 记录上下文构建器
     */
    public static class RecordContext {
        private String orderNo;
        private Long userId;
        private Long artworkId;
        private Long resaleId;
        private String cartIds;
        private String source;
        private OrderFailReason failReason;
        private String failMessage;
        private String requestParamsJson;
        private boolean compensated;

        public static RecordContext builder() {
            return new RecordContext();
        }

        public RecordContext orderNo(String orderNo) { this.orderNo = orderNo; return this; }
        public RecordContext userId(Long userId) { this.userId = userId; return this; }
        public RecordContext artworkId(Long artworkId) { this.artworkId = artworkId; return this; }
        public RecordContext resaleId(Long resaleId) { this.resaleId = resaleId; return this; }
        public RecordContext cartIds(String cartIds) { this.cartIds = cartIds; return this; }
        public RecordContext source(String source) { this.source = source; return this; }
        public RecordContext failReason(OrderFailReason failReason) { this.failReason = failReason; return this; }
        public RecordContext failMessage(String failMessage) { this.failMessage = failMessage; return this; }
        public RecordContext requestParams(Object params) {
            this.requestParamsJson = params != null ? JSON.toJSONString(params) : null;
            return this;
        }
        public RecordContext compensated(boolean compensated) { this.compensated = compensated; return this; }
    }
}
