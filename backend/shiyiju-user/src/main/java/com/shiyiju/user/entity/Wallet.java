package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户钱包表
 */
@Data
@TableName("user_wallet")
public class Wallet implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结金额 */
    private BigDecimal freezeAmount;

    /** 待结算金额 */
    private BigDecimal pendingAmount;

    /** 保证金 */
    private BigDecimal depositAmount;

    /** 累计收入 */
    private BigDecimal totalIncome;

    /** 累计提现 */
    private BigDecimal totalWithdraw;

    /** 乐观锁 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
