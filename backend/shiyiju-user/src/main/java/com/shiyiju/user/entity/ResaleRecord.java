package com.shiyiju.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转售记录表
 */
@Data
@TableName("resale_record")
public class ResaleRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品ID */
    private Long artworkId;

    /** 卖家用户ID */
    private Long sellerUserId;

    /** 买家用户ID */
    private Long buyerUserId;

    /** 来源订单ID（首次出售或上次转售的订单） */
    private Long sourceOrderId;

    /** 转售价格 */
    private BigDecimal resalePrice;

    /** 艺术家持续收益 */
    private BigDecimal artistIncome;

    /** 平台服务费 */
    private BigDecimal platformFee;

    /** 卖家实际到账收入 */
    private BigDecimal sellerIncome;

    /** 状态: pending/paid/completed/cancel */
    private String status;

    /** 交易编号（用于幂等控制） */
    private String tradeNo;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
