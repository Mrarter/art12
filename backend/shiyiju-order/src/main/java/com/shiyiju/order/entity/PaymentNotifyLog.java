package com.shiyiju.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Raw channel callback log for audit and troubleshooting.
 */
@Data
@TableName("payment_notify_log")
public class PaymentNotifyLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channel;

    private String payNo;

    private String bizNo;

    private String channelTradeNo;

    private String notifyType;

    private String rawPayload;

    private Integer verified;

    private String processStatus;

    private String failReason;

    private LocalDateTime createTime;
}
