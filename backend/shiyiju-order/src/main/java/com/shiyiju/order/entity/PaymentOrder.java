package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified payment order across WeChat Pay and Alipay.
 */
@Data
@TableName("payment_order")
public class PaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private Long userId;

    private BigDecimal amount;

    private String channel;

    private String tradeType;

    private String status;

    private String channelTradeNo;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private String requestPayload;

    private String responsePayload;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
