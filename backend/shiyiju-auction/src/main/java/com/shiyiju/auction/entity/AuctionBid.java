package com.shiyiju.auction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("auction_bids")
public class AuctionBid implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lotId;
    private Long userId;
    private Long bidPrice;
    private LocalDateTime bidTime;
    private Integer isWinning;
}
