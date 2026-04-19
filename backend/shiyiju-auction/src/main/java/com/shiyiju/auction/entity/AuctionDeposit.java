package com.shiyiju.auction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("auction_deposits")
public class AuctionDeposit implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lotId;
    private Long userId;
    private Long amount;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime refundTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
