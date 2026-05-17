package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水表
 */
@Data
@TableName("wallet_bill")
public class WalletBill implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 流水类型 */
    private String billType;

    /** 变动金额 */
    private BigDecimal amount;

    /** 变动前余额 */
    private BigDecimal beforeBalance;

    /** 变动后余额 */
    private BigDecimal afterBalance;

    /** 关联业务ID */
    private Long relatedId;

    /** 关联业务类型 */
    private String relatedType;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
