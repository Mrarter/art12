package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账本交易 — 每笔资金变动的唯一记录
 *
 * 设计原则：
 * - 每条记录代表**一个方向**的变动（DEBIT 或 CREDIT）
 * - 一条业务交易（如 PAY）拆为多条 ledger_transaction
 * - 退款 = 反向分录（reversal_of_txn_id 指向原交易）
 * - 永不修改，只追加（Append-only）
 */
@Data
@TableName("ledger_transaction")
public class LedgerTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务交易ID（全局唯一，用于幂等） */
    private String txnId;

    /** 业务类型: PAY / REFUND / RESALE / COMMISSION / WITHDRAW */
    private String bizType;

    /** 方向: DEBIT(借=扣款) / CREDIT(贷=入账) */
    private String direction;

    /** 金额 */
    private BigDecimal amount;

    /** 用户ID */
    private Long userId;

    /** 关联业务ID（订单ID/转售ID） */
    private Long relatedId;

    /** 关联业务类型 */
    private String relatedType;

    /** 反向交易ID（退款时指向原始 PAY 的 txn_id） */
    private String reversalOfTxnId;

    /** 状态: SUCCESS / FAILED */
    private String status;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
