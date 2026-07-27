package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ 消费日志 — at-least-once + 幂等消费保障
 */
@Data
@TableName("mq_consume_log")
public class MqConsumeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息ID（全局唯一） */
    private String msgId;

    /** 业务交易ID */
    private String txnId;

    /** 事件类型 */
    private String eventType;

    /** 状态: PRODUCED / CONSUMED / FAILED */
    private String status;

    /** 重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private LocalDateTime consumedTime;
}
