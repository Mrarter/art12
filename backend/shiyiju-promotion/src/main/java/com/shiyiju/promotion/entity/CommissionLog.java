package com.shiyiju.promotion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("commission_logs")
public class CommissionLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promoterId;
    private Long orderId;
    private Long orderItemId;
    private Integer level;
    private BigDecimal commissionRate;
    private Long orderAmount;
    private Long commissionAmount;
    private Integer status;
    private LocalDateTime settleTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
