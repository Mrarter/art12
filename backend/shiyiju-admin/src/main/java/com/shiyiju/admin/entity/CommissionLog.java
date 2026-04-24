package com.shiyiju.admin.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金记录实体
 */
@Data
public class CommissionLog {
    private Long id;
    private Long userId;
    private Long orderId;
    private String orderNo;
    private Long artworkId;
    private Long buyerId;
    private BigDecimal orderAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private Integer commissionType;
    private Integer level;
    private Integer status;
    private LocalDateTime settleTime;
    private String remark;
    private LocalDateTime createTime;
}
