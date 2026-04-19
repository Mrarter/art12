package com.shiyiju.auction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("auction_lots")
public class AuctionLot implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long artworkId;
    private String lotNo;
    private Long startPrice;
    private Long reservePrice;
    private Long currentPrice;
    private Long bidIncrement;
    private Integer bidCount;
    private Long highestBidderId;
    private Long depositAmount;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long finalPrice;
    private Long winnerId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
