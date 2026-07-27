package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 金融事件死信队列（DLQ）
 *
 * 超过最大重试次数的事件移入此表，
 * 管理员可手动重放（Replay）到 Outbox。
 */
@Data
@TableName("finance_event_dlq")
public class FinanceEventDlq implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原 Outbox ID */
    private Long outboxId;

    /** 事件类型 */
    private String eventType;

    /** 事件体 */
    private String eventBody;

    /** 已重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    /** 进入 DLQ 时间 */
    private LocalDateTime failedTime;

    /** 是否已重放 */
    private Integer isReplayed;

    /** 重放时间 */
    private LocalDateTime replayedTime;
}
