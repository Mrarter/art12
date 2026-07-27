package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统一佣金记录表
 * 替代多个模块的 commission_log / commission_logs
 */
@Data
@TableName("commission_record")
public class CommissionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 佣金接收人用户ID */
    private Long userId;

    /** 来源用户ID（购买者/推广人） */
    private Long sourceUserId;

    /** 关联订单ID */
    private Long orderId;

    /** 关联作品ID */
    private Long artworkId;

    /** 佣金类型 */
    private String commissionType;

    /** 佣金层级 */
    private Integer commissionLevel;

    /** 佣金比例(%) */
    private BigDecimal rate;

    /** 佣金金额 */
    private BigDecimal amount;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
