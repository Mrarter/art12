package com.shiyiju.auction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auction_bid")
public class AuctionBid implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lotId;
    private Long userId;
    private BigDecimal bidPrice;
    private LocalDateTime bidTime;
    private Integer status;
}
