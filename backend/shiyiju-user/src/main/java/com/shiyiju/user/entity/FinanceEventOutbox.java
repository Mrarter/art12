package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 金融事件 Outbox 表
 *
 * 所有资金变动事件先写入此表（与业务操作同事务），
 * 再由定时任务轮询发送到事件处理器，
 * 确保「业务操作」与「事件投递」的事务一致性。
 */
@Data
@TableName("finance_event_outbox")
public class FinanceEventOutbox implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件类型（对应 FinanceEventType 枚举名） */
    private String eventType;

    /** 事件体（序列化的 FinanceEvent JSON） */
    private String eventBody;

    /** 状态: PENDING / PROCESSING / COMPLETED / FAILED */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetries;

    /** 最近一次错误信息 */
    private String errorMessage;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 完成时间 */
    private LocalDateTime completedTime;
}
