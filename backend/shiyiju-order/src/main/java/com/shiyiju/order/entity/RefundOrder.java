package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified refund order. The first implementation creates the table contract;
 * channel refund calls can be moved here from business order code incrementally.
 */
@Data
@TableName("refund_order")
public class RefundOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private String payNo;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal refundAmount;

    private String channel;

    private String status;

    private String channelRefundNo;

    private String reason;

    private LocalDateTime refundTime;

    private String requestPayload;

    private String responsePayload;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
