package com.shiyiju.order.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shiyiju.order.entity.Order;
import com.shiyiju.order.entity.PaymentNotifyLog;
import com.shiyiju.order.entity.PaymentOrder;
import com.shiyiju.order.entity.RefundOrder;
import com.shiyiju.order.mapper.PaymentNotifyLogMapper;
import com.shiyiju.order.mapper.PaymentOrderMapper;
import com.shiyiju.order.mapper.RefundOrderMapper;
import com.shiyiju.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the platform payment order lifecycle. Business orders stay in OrderService;
 * channel integrations use payNo as out_trade_no.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    public static final String BIZ_TYPE_ORDER = "ORDER";

    public static final String CHANNEL_WECHAT = "WECHAT";
    public static final String CHANNEL_ALIPAY = "ALIPAY";

    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_PAYING = "PAYING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REFUNDING = "REFUNDING";
    public static final String STATUS_REFUNDED = "REFUNDED";

    private static final DateTimeFormatter PAY_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentNotifyLogMapper notifyLogMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final JdbcTemplate jdbcTemplate;

    public void ensurePaymentTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS payment_order (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                pay_no VARCHAR(40) NOT NULL COMMENT '平台支付单号',
                biz_type VARCHAR(32) NOT NULL COMMENT '业务类型: ORDER/AUCTION_DEPOSIT/RESALE/RECHARGE',
                biz_id BIGINT NOT NULL COMMENT '业务主键',
                biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
                user_id BIGINT NOT NULL COMMENT '付款用户ID',
                amount DECIMAL(12,2) NOT NULL COMMENT '支付金额，沿用业务订单金额单位',
                channel VARCHAR(20) NOT NULL COMMENT 'WECHAT/ALIPAY',
                trade_type VARCHAR(32) NOT NULL COMMENT '渠道交易类型',
                status VARCHAR(20) NOT NULL DEFAULT 'INIT',
                channel_trade_no VARCHAR(80) DEFAULT NULL COMMENT '渠道交易号',
                expire_time DATETIME DEFAULT NULL,
                pay_time DATETIME DEFAULT NULL,
                request_payload TEXT DEFAULT NULL,
                response_payload TEXT DEFAULT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_pay_no (pay_no),
                KEY idx_biz (biz_type, biz_id),
                KEY idx_biz_no (biz_no),
                KEY idx_user_status (user_id, status),
                KEY idx_channel_trade_no (channel_trade_no)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一支付单'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS payment_notify_log (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                channel VARCHAR(20) NOT NULL COMMENT 'WECHAT/ALIPAY',
                pay_no VARCHAR(40) DEFAULT NULL,
                biz_no VARCHAR(64) DEFAULT NULL,
                channel_trade_no VARCHAR(80) DEFAULT NULL,
                notify_type VARCHAR(20) NOT NULL DEFAULT 'PAY',
                raw_payload MEDIUMTEXT DEFAULT NULL,
                verified TINYINT NOT NULL DEFAULT 0,
                process_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
                fail_reason VARCHAR(500) DEFAULT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_pay_no (pay_no),
                KEY idx_biz_no (biz_no),
                KEY idx_channel_time (channel, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道通知日志'
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS refund_order (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                refund_no VARCHAR(40) NOT NULL COMMENT '平台退款单号',
                pay_no VARCHAR(40) NOT NULL COMMENT '平台支付单号',
                biz_type VARCHAR(32) NOT NULL,
                biz_id BIGINT NOT NULL,
                biz_no VARCHAR(64) NOT NULL,
                user_id BIGINT NOT NULL,
                total_amount DECIMAL(12,2) NOT NULL,
                refund_amount DECIMAL(12,2) NOT NULL,
                channel VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'INIT',
                channel_refund_no VARCHAR(80) DEFAULT NULL,
                reason VARCHAR(500) DEFAULT NULL,
                refund_time DATETIME DEFAULT NULL,
                request_payload TEXT DEFAULT NULL,
                response_payload TEXT DEFAULT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_refund_no (refund_no),
                KEY idx_pay_no (pay_no),
                KEY idx_biz (biz_type, biz_id),
                KEY idx_user_status (user_id, status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一退款单'
            """);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createOrderPayment(Order order, String channel, String tradeType, String description) {
        ensurePaymentTables();
        PaymentOrder payment = new PaymentOrder();
        payment.setPayNo(nextPayNo());
        payment.setBizType(BIZ_TYPE_ORDER);
        payment.setBizId(order.getId());
        payment.setBizNo(order.getOrderNo());
        payment.setUserId(order.getUserId());
        payment.setAmount(order.getPayAmount());
        payment.setChannel(channel);
        payment.setTradeType(tradeType);
        payment.setStatus(STATUS_INIT);
        payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
        payment.setRequestPayload(toJson(Map.of(
                "orderNo", order.getOrderNo(),
                "description", description == null ? "" : description
        )));
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());
        paymentOrderMapper.insert(payment);
        return payment;
    }

    public void markPaying(String payNo, Object responsePayload) {
        ensurePaymentTables();
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPayNo, payNo)
                .in(PaymentOrder::getStatus, STATUS_INIT, STATUS_PAYING)
                .set(PaymentOrder::getStatus, STATUS_PAYING)
                .set(PaymentOrder::getResponsePayload, toJson(responsePayload))
                .set(PaymentOrder::getUpdateTime, LocalDateTime.now()));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder markPaySuccess(String outTradeNo, String channel, String channelTradeNo, Object responsePayload) {
        ensurePaymentTables();
        PaymentOrder payment = findByPayNo(outTradeNo);
        if (payment == null) {
            payment = findLatestByBizNo(outTradeNo);
        }
        if (payment == null) {
            log.warn("支付成功回调未找到支付单: outTradeNo={}, channel={}", outTradeNo, channel);
            return null;
        }
        if (STATUS_SUCCESS.equals(payment.getStatus())) {
            return payment;
        }
        int updated = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, payment.getId())
                .in(PaymentOrder::getStatus, STATUS_INIT, STATUS_PAYING)
                .set(PaymentOrder::getStatus, STATUS_SUCCESS)
                .set(PaymentOrder::getChannelTradeNo, channelTradeNo)
                .set(PaymentOrder::getPayTime, LocalDateTime.now())
                .set(PaymentOrder::getResponsePayload, toJson(responsePayload))
                .set(PaymentOrder::getUpdateTime, LocalDateTime.now()));
        if (updated > 0) {
            payment = paymentOrderMapper.selectById(payment.getId());
        }
        return payment;
    }

    public PaymentOrder findLatestByBizNo(String bizNo) {
        ensurePaymentTables();
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getBizNo, bizNo)
                .orderByDesc(PaymentOrder::getCreateTime)
                .last("LIMIT 1"));
    }

    public PaymentOrder findLatestByBiz(String bizType, Long bizId) {
        ensurePaymentTables();
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getBizType, bizType)
                .eq(PaymentOrder::getBizId, bizId)
                .orderByDesc(PaymentOrder::getCreateTime)
                .last("LIMIT 1"));
    }

    public PaymentOrder findByPayNo(String payNo) {
        ensurePaymentTables();
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPayNo, payNo)
                .last("LIMIT 1"));
    }

    public void recordNotify(String channel, String notifyType, String outTradeNo, String bizNo,
            String channelTradeNo, Object rawPayload, boolean verified, String processStatus, String failReason) {
        ensurePaymentTables();
        PaymentNotifyLog logRecord = new PaymentNotifyLog();
        logRecord.setChannel(channel);
        logRecord.setNotifyType(notifyType);
        logRecord.setPayNo(outTradeNo);
        logRecord.setBizNo(bizNo);
        logRecord.setChannelTradeNo(channelTradeNo);
        logRecord.setRawPayload(toJson(rawPayload));
        logRecord.setVerified(verified ? 1 : 0);
        logRecord.setProcessStatus(processStatus);
        logRecord.setFailReason(failReason);
        logRecord.setCreateTime(LocalDateTime.now());
        notifyLogMapper.insert(logRecord);
    }

    @Transactional(rollbackFor = Exception.class)
    public RefundOrder createOrderRefund(Order order, BigDecimal refundAmount, String reason) {
        ensurePaymentTables();
        PaymentOrder payment = findLatestByBiz(PaymentService.BIZ_TYPE_ORDER, order.getId());
        String channel = payment != null ? payment.getChannel() : "UNKNOWN";
        String payNo = payment != null ? payment.getPayNo() : order.getOrderNo();

        RefundOrder existing = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getBizType, BIZ_TYPE_ORDER)
                .eq(RefundOrder::getBizId, order.getId())
                .in(RefundOrder::getStatus, STATUS_INIT, STATUS_REFUNDING)
                .orderByDesc(RefundOrder::getCreateTime)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }

        RefundOrder refund = new RefundOrder();
        refund.setRefundNo(nextRefundNo());
        refund.setPayNo(payNo);
        refund.setBizType(BIZ_TYPE_ORDER);
        refund.setBizId(order.getId());
        refund.setBizNo(order.getOrderNo());
        refund.setUserId(order.getUserId());
        refund.setTotalAmount(order.getPayAmount());
        refund.setRefundAmount(refundAmount != null ? refundAmount : order.getPayAmount());
        refund.setChannel(channel);
        refund.setStatus(STATUS_INIT);
        refund.setReason(reason);
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        refundOrderMapper.insert(refund);

        if (payment != null) {
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getId, payment.getId())
                    .set(PaymentOrder::getStatus, STATUS_REFUNDING)
                    .set(PaymentOrder::getUpdateTime, LocalDateTime.now()));
        }
        return refund;
    }

    public void markRefundSuccessByBizNo(String bizNo, String channelRefundNo, Object responsePayload) {
        ensurePaymentTables();
        RefundOrder refund = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getBizNo, bizNo)
                .orderByDesc(RefundOrder::getCreateTime)
                .last("LIMIT 1"));
        if (refund == null) {
            return;
        }
        LambdaUpdateWrapper<RefundOrder> refundUpdate = new LambdaUpdateWrapper<RefundOrder>()
                .eq(RefundOrder::getId, refund.getId())
                .ne(RefundOrder::getStatus, STATUS_REFUNDED)
                .set(RefundOrder::getStatus, STATUS_REFUNDED)
                .set(RefundOrder::getRefundTime, LocalDateTime.now())
                .set(RefundOrder::getResponsePayload, toJson(responsePayload))
                .set(RefundOrder::getUpdateTime, LocalDateTime.now());
        if (channelRefundNo != null && !channelRefundNo.isBlank()) {
            refundUpdate.set(RefundOrder::getChannelRefundNo, channelRefundNo);
        }
        refundOrderMapper.update(null, refundUpdate);
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPayNo, refund.getPayNo())
                .set(PaymentOrder::getStatus, STATUS_REFUNDED)
                .set(PaymentOrder::getUpdateTime, LocalDateTime.now()));
    }

    public PageResult<Map<String, Object>> listPayments(String status, String channel, String keyword, int page, int pageSize) {
        ensurePaymentTables();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            args.add(status.trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (channel != null && !channel.isBlank()) {
            where.append(" AND channel = ?");
            args.add(channel.trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (pay_no LIKE ? OR biz_no LIKE ? OR channel_trade_no LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_order" + where, Long.class, args.toArray());
        java.util.List<Object> queryArgs = new java.util.ArrayList<>(args);
        queryArgs.add(Math.max(page - 1, 0) * pageSize);
        queryArgs.add(pageSize);
        java.util.List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, pay_no AS payNo, biz_type AS bizType, biz_id AS bizId, biz_no AS bizNo,
                   user_id AS userId, amount, channel, trade_type AS tradeType, status,
                   channel_trade_no AS channelTradeNo, expire_time AS expireTime, pay_time AS payTime,
                   create_time AS createTime, update_time AS updateTime
            FROM payment_order
            """ + where + " ORDER BY create_time DESC, id DESC LIMIT ?, ?", queryArgs.toArray());
        return PageResult.of(total == null ? 0L : total, page, pageSize, rows);
    }

    public PageResult<Map<String, Object>> listNotifyLogs(String channel, String keyword, int page, int pageSize) {
        ensurePaymentTables();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (channel != null && !channel.isBlank()) {
            where.append(" AND channel = ?");
            args.add(channel.trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (pay_no LIKE ? OR biz_no LIKE ? OR channel_trade_no LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_notify_log" + where, Long.class, args.toArray());
        java.util.List<Object> queryArgs = new java.util.ArrayList<>(args);
        queryArgs.add(Math.max(page - 1, 0) * pageSize);
        queryArgs.add(pageSize);
        java.util.List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, channel, pay_no AS payNo, biz_no AS bizNo, channel_trade_no AS channelTradeNo,
                   notify_type AS notifyType, verified, process_status AS processStatus,
                   fail_reason AS failReason, create_time AS createTime
            FROM payment_notify_log
            """ + where + " ORDER BY create_time DESC, id DESC LIMIT ?, ?", queryArgs.toArray());
        return PageResult.of(total == null ? 0L : total, page, pageSize, rows);
    }

    private String nextPayNo() {
        return "PAY" + LocalDateTime.now().format(PAY_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String nextRefundNo() {
        return "REF" + LocalDateTime.now().format(PAY_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        try {
            return JSON.toJSONString(value);
        } catch (Exception e) {
            return Objects.toString(value, null);
        }
    }
}
