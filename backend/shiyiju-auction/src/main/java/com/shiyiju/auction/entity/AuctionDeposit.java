package com.shiyiju.auction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auction_deposit")
public class AuctionDeposit implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lotId;
    private Long userId;
    private BigDecimal amount;
    private Integer payStatus;
    private LocalDateTime payTime;
    private LocalDateTime refundTime;
    private String transactionId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
