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
    /** 客户端幂等键，防止网络重试造成重复出价。 */
    private String requestId;
}
